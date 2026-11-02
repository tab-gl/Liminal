import { defineSecret } from "firebase-functions/params";

/**
 * The Cloudinary connection string is injected by Secret Manager.
 * Format: cloudinary://<api_key>:<api_secret>@<cloud_name>
 *
 * It is never bundled into the APK and never returned to the client — only a
 * short-lived signature leaves this function.
 */
export const CLOUDINARY_URL = defineSecret("CLOUDINARY_URL");

export const REGION = "asia-south1";

/** Shipping policy — mirrored in the client only for display. */
export const FREE_SHIPPING_THRESHOLD_PAISE = 99_900;
export const FLAT_SHIPPING_PAISE = 4_900;

export const ALLOWED_UPLOAD_FOLDERS = [
  "sproutly/products",
  "sproutly/avatars",
  "sproutly/store-logos",
  "sproutly/store-banners",
] as const;

export interface CloudinaryCredentials {
  cloudName: string;
  apiKey: string;
  apiSecret: string;
}

export function parseCloudinaryUrl(url: string): CloudinaryCredentials {
  // cloudinary://key:secret@cloud
  const match = /^cloudinary:\/\/([^:]+):([^@]+)@(.+)$/.exec(url.trim());
  if (!match) {
    throw new Error("CLOUDINARY_URL is not in the expected cloudinary://key:secret@cloud form");
  }
  const [, apiKey, apiSecret, cloudName] = match;
  return { apiKey, apiSecret, cloudName };
}
