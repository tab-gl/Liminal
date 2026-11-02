import { HttpsError, onCall } from "firebase-functions/v2/https";
import { createHash } from "node:crypto";
import { getFirestore, FieldValue } from "firebase-admin/firestore";
import {
  ALLOWED_UPLOAD_FOLDERS,
  CLOUDINARY_URL,
  REGION,
  parseCloudinaryUrl,
} from "../config";
import { badRequest, requireCaller } from "../guards";

/**
 * Returns everything the Android client needs to post an image straight to
 * Cloudinary: the cloud name, the public api key, a timestamp and a signature
 * over the upload parameters.
 *
 * The api_secret stays inside Secret Manager and never reaches the device.
 */
export const getCloudinaryUploadSignature = onCall(
  { region: REGION, secrets: [CLOUDINARY_URL], enforceAppCheck: true },
  async (request) => {
    await requireCaller(request);

    const folder = String(request.data?.folder ?? "");
    if (!ALLOWED_UPLOAD_FOLDERS.includes(folder as (typeof ALLOWED_UPLOAD_FOLDERS)[number])) {
      throw badRequest("That upload folder is not allowed.");
    }

    const { cloudName, apiKey, apiSecret } = parseCloudinaryUrl(CLOUDINARY_URL.value());
    const timestamp = Math.floor(Date.now() / 1000);

    // Cloudinary signs the alphabetically sorted, ampersand-joined params
    // (excluding file, api_key and cloud_name).
    const paramsToSign = `folder=${folder}&timestamp=${timestamp}`;
    const signature = createHash("sha1").update(paramsToSign + apiSecret).digest("hex");

    return {
      ok: true,
      cloudName,
      apiKey,
      timestamp: String(timestamp),
      signature,
      folder,
      uploadUrl: `https://api.cloudinary.com/v1_1/${cloudName}/image/upload`,
    };
  },
);

/**
 * Deletes an asset from Cloudinary after checking that the caller owns the
 * document that references it. Orphaned assets (no referencing document) can
 * only be removed by an administrator.
 */
export const deleteCloudinaryAsset = onCall(
  { region: REGION, secrets: [CLOUDINARY_URL], enforceAppCheck: true },
  async (request) => {
    const caller = await requireCaller(request);
    const publicId = String(request.data?.publicId ?? "");
    if (!publicId.startsWith("sproutly/")) {
      throw badRequest("That asset is outside the Sproutly media space.");
    }

    const firestore = getFirestore();
    const owned = await isOwnedByCaller(firestore, publicId, caller.uid);
    if (!owned && caller.role !== "ADMIN") {
      throw new HttpsError("permission-denied", "You can only delete media you uploaded.");
    }

    const { cloudName, apiKey, apiSecret } = parseCloudinaryUrl(CLOUDINARY_URL.value());
    const timestamp = Math.floor(Date.now() / 1000);
    const paramsToSign = `public_id=${publicId}&timestamp=${timestamp}`;
    const signature = createHash("sha1").update(paramsToSign + apiSecret).digest("hex");

    const body = new URLSearchParams({
      public_id: publicId,
      timestamp: String(timestamp),
      api_key: apiKey,
      signature,
    });

    const response = await fetch(
      `https://api.cloudinary.com/v1_1/${cloudName}/image/destroy`,
      { method: "POST", body },
    );

    if (!response.ok) {
      throw badRequest("Cloudinary rejected the delete request.");
    }

    return { ok: true };
  },
);

async function isOwnedByCaller(
  firestore: FirebaseFirestore.Firestore,
  publicId: string,
  uid: string,
): Promise<boolean> {
  const userSnap = await firestore.collection("users").doc(uid).get();
  if (userSnap.exists) {
    const data = userSnap.data() ?? {};
    if (data.photoPublicId === publicId) return true;
  }

  const storeSnap = await firestore
    .collection("stores")
    .where("ownerId", "==", uid)
    .limit(5)
    .get();
  for (const doc of storeSnap.docs) {
    const data = doc.data();
    if (data.logoPublicId === publicId || data.bannerPublicId === publicId) return true;
  }

  const storeIds = storeSnap.docs.map((doc) => doc.id);
  for (const storeId of storeIds) {
    const products = await firestore
      .collection("products")
      .where("storeId", "==", storeId)
      .limit(200)
      .get();
    for (const product of products.docs) {
      const images = (product.get("images") as Array<{ publicId?: string }>) ?? [];
      if (images.some((image) => image.publicId === publicId)) return true;
    }
  }

  return false;
}

export const recordMediaUsage = onCall(
  { region: REGION, enforceAppCheck: true },
  async (request) => {
    const caller = await requireCaller(request);
    const publicId = String(request.data?.publicId ?? "");
    await getFirestore()
      .collection("media_usage")
      .doc(publicId.replace(/\//g, "_"))
      .set(
        {
          publicId,
          ownerId: caller.uid,
          bytes: Number(request.data?.bytes ?? 0),
          createdAt: FieldValue.serverTimestamp(),
        },
        { merge: true },
      );
    return { ok: true };
  },
);
