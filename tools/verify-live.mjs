#!/usr/bin/env node
/**
 * Checks that the *live* Firebase project matches this repository.
 *
 * Deploying is only half the job — the common failure is a project that is
 * running an older ruleset, has indexes still building, or an empty category
 * collection, while the app on the device looks broken. This asks the live
 * project directly, using only the public API key from google-services.json,
 * so it needs no credentials and can be run by anyone.
 *
 *   node tools/verify-live.mjs
 *
 * Exit code 0 when everything is ready, 1 when something is not.
 */
import { execFileSync } from "node:child_process";
import { readFileSync } from "node:fs";
import https from "node:https";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = dirname(dirname(fileURLToPath(import.meta.url)));
const config = JSON.parse(
  readFileSync(join(ROOT, "app", "google-services.json"), "utf8"),
);
const PROJECT = config.project_info.project_id;
const API_KEY = config.client[0].api_key[0].current_key;
const BASE = `https://firestore.googleapis.com/v1/projects/${PROJECT}/databases/(default)/documents`;

const CLOUD_NAME = "wjoegjfk"; // must match BuildConfig.CLOUDINARY_CLOUD_NAME

const results = [];
const record = (name, status, detail) => results.push({ name, status, detail });

/**
 * Plain node:https — no curl, no WebAssembly, nothing to install.
 *
 * (fetch is out because its undici dependency allocates WebAssembly, which
 * fails in small containers; curl is out because a phone terminal often has no
 * curl binary. node:https is core and always present.)
 *
 * If HTTPS_PROXY is set, the request goes through curl instead, because
 * node:https does not read proxy environment variables.
 */
function request(url, init = {}) {
  const method = init.method ?? "GET";
  const headers = init.headers ?? {};
  const body = init.body;

  if (process.env.HTTPS_PROXY || process.env.https_proxy) {
    const args = ["-s", "-o", "-", "-w", "\n%{http_code}", "-X", method];
    for (const [key, value] of Object.entries(headers)) args.push("-H", `${key}: ${value}`);
    if (body) args.push("--data-binary", body);
    args.push(url);
    try {
      const output = execFileSync("curl", args, { encoding: "utf8", maxBuffer: 4 << 20 });
      const split = output.lastIndexOf("\n");
      return Promise.resolve({
        status: Number(output.slice(split + 1).trim()),
        text: output.slice(0, split),
      });
    } catch (error) {
      if (error.code === "ENOENT") {
        return Promise.reject(
          new Error("HTTPS_PROXY is set but curl is not installed — run: apk add curl"),
        );
      }
      return Promise.reject(error);
    }
  }

  return new Promise((resolve, reject) => {
    const target = new URL(url);
    const req = https.request(
      {
        hostname: target.hostname,
        path: target.pathname + target.search,
        method,
        headers,
        timeout: 15000,
      },
      (res) => {
        let text = "";
        res.setEncoding("utf8");
        res.on("data", (chunk) => (text += chunk));
        res.on("end", () => resolve({ status: res.statusCode ?? 0, text }));
      },
    );
    req.on("timeout", () => req.destroy(new Error(`timed out: ${url}`)));
    req.on("error", reject);
    if (body) req.write(body);
    req.end();
  });
}

async function json(url, init = {}) {
  const { status, text } = await request(url, init);
  try {
    return { status, body: JSON.parse(text) };
  } catch {
    return { status, body: text };
  }
}

function firstError(body) {
  if (Array.isArray(body)) return body[0]?.error?.message ?? "";
  return body?.error?.message ?? "";
}

// ---------------------------------------------------------------- auth providers
async function checkEmailPassword() {
  const { body } = await json(
    `https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${API_KEY}`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        email: "verify-live-probe@sproutly.test",
        password: "x9Kq2mNv7pLr",
        returnSecureToken: true,
      }),
    },
  );
  const message = firstError(body);
  if (message.includes("INVALID_LOGIN_CREDENTIALS") || message.includes("EMAIL_NOT_FOUND")) {
    record("Auth: email/password", "PASS", "provider enabled");
  } else if (message.includes("OPERATION_NOT_ALLOWED")) {
    record("Auth: email/password", "FAIL", "disabled — enable it in Authentication → Sign-in method");
  } else {
    record("Auth: email/password", "FAIL", message || "unexpected response");
  }
}

async function checkGoogle() {
  const { body } = await json(
    `https://identitytoolkit.googleapis.com/v1/accounts:createAuthUri?key=${API_KEY}`,
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        identifier: "verify-live-probe@sproutly.test",
        providerId: "google.com",
        continueUri: "http://localhost",
      }),
    },
  );
  if (body?.authUri) {
    record("Auth: Google", "PASS", "provider enabled");
  } else {
    record("Auth: Google", "FAIL", firstError(body) || "enable Google in Authentication → Sign-in method");
  }
}

// ------------------------------------------------------------------------ rules
async function checkRulesLive() {
  // firestore.rules allows public reads of stores; the Flutter ruleset does not,
  // so this single probe distinguishes which ruleset is deployed.
  const { status, body } = await json(`${BASE}/stores?key=${API_KEY}&pageSize=1`);
  if (status === 200) {
    record("Rules: Sproutly ruleset live", "PASS", "public store reads allowed");
  } else if (status === 403) {
    record(
      "Rules: Sproutly ruleset live",
      "FAIL",
      "stores are not publicly readable — a different ruleset is deployed. " +
        "Run: firebase deploy --only firestore:rules",
    );
  } else {
    record("Rules: Sproutly ruleset live", "FAIL", firstError(body) || `HTTP ${status}`);
  }
}

async function checkRulesDenyPrivate() {
  // A healthy ruleset denies unauthenticated reads of users.
  const { status } = await json(`${BASE}/users?key=${API_KEY}&pageSize=1`);
  if (status === 403) {
    record("Rules: private data denied", "PASS", "users not readable without auth");
  } else if (status === 200) {
    record("Rules: private data denied", "FAIL", "users readable without auth — too permissive");
  } else {
    record("Rules: private data denied", "WARN", `unexpected HTTP ${status}`);
  }
}

// ------------------------------------------------------------------------- data
async function checkCategories() {
  const { status, body } = await json(`${BASE}/categories?key=${API_KEY}&pageSize=20`);
  if (status !== 200) {
    record("Data: categories seeded", "FAIL", "cannot read categories — deploy rules first");
    return;
  }
  const count = body?.documents?.length ?? 0;
  if (count === 0) {
    record("Data: categories seeded", "FAIL", "0 categories — run tools/seed/seed.mjs");
  } else {
    record("Data: categories seeded", "PASS", `${count} categories`);
  }
}

async function checkCompositeIndex() {
  const { status, body } = await json(`${BASE}:runQuery?key=${API_KEY}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      structuredQuery: {
        from: [{ collectionId: "products" }],
        where: {
          fieldFilter: {
            field: { fieldPath: "status" },
            op: "EQUAL",
            value: { stringValue: "ACTIVE" },
          },
        },
        orderBy: [{ field: { fieldPath: "unitsSold" }, direction: "DESCENDING" }],
        limit: 1,
      },
    }),
  });
  const message = firstError(body);
  if (status === 200) {
    record("Indexes: composite query", "PASS", "status + unitsSold served");
  } else if (status === 403) {
    record("Indexes: composite query", "WARN", "blocked by rules — deploy rules, then re-run");
  } else if (status === 400 && /index/i.test(message)) {
    const link = message.match(/https:\/\/console\.firebase\.google\.com\S+/)?.[0];
    record(
      "Indexes: composite query",
      "FAIL",
      `index missing or still building — firebase deploy --only firestore:indexes${link ? ` (${link})` : ""}`,
    );
  } else {
    record("Indexes: composite query", "FAIL", message || `HTTP ${status}`);
  }
}

// ------------------------------------------------------------------- cloudinary
async function checkCloudinary() {
  const { status, body } = await json(
    `https://api.cloudinary.com/v1_1/${CLOUD_NAME}/image/upload`,
    { method: "POST" },
  );
  const message = body?.error?.message ?? "";
  if (status === 400 && /preset/i.test(message)) {
    record("Cloudinary: cloud name", "PASS", `cloud '${CLOUD_NAME}' reachable`);
  } else if (/cloud_name/i.test(message)) {
    record("Cloudinary: cloud name", "FAIL", `cloud '${CLOUD_NAME}' is not valid`);
  } else {
    record("Cloudinary: cloud name", "WARN", message || `HTTP ${status}`);
  }
}

// ------------------------------------------------------------------------ report
function print() {
  const width = Math.max(...results.map((r) => r.name.length));
  console.log(`\nLive project: ${PROJECT}\n`);
  for (const { name, status, detail } of results) {
    const mark = status === "PASS" ? "✓" : status === "FAIL" ? "✗" : "!";
    console.log(`${mark} ${name.padEnd(width)}  ${detail}`);
  }
  const failed = results.filter((r) => r.status === "FAIL").length;
  const warned = results.filter((r) => r.status === "WARN").length;
  console.log(
    `\n${results.length - failed - warned} passed, ${warned} warning(s), ${failed} failed`,
  );
  if (failed) {
    console.log("\nNot ready. Fix the ✗ lines above, then re-run.");
  } else if (warned) {
    console.log("\nReady, with warnings worth a look.");
  } else {
    console.log("\nThe live project matches this repository.");
  }
}

const checks = [
  checkEmailPassword,
  checkGoogle,
  checkRulesLive,
  checkRulesDenyPrivate,
  checkCategories,
  checkCompositeIndex,
  checkCloudinary,
];

for (const check of checks) {
  try {
    await check();
  } catch (error) {
    record(check.name, "FAIL", error.message);
  }
}

print();
process.exit(results.some((r) => r.status === "FAIL") ? 1 : 0);
