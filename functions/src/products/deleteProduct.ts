import { onCall, HttpsError } from "firebase-functions/v2/https";
import { createHash } from "node:crypto";
import { getFirestore, FieldValue } from "firebase-admin/firestore";
import { CLOUDINARY_URL, REGION, parseCloudinaryUrl } from "../config";
import { badRequest, failedPrecondition, requireCaller } from "../guards";

/**
 * Deletes a product and its Cloudinary assets.
 *
 * Only the owning store (or an admin) may delete a listing. The document is
 * removed first so the marketplace stops serving it, then the media is purged.
 */
export const deleteProduct = onCall(
  { region: REGION, secrets: [CLOUDINARY_URL], enforceAppCheck: true },
  async (request) => {
    const caller = await requireCaller(request);
    const productId = String(request.data?.productId ?? "");
    if (!productId) throw badRequest("A product id is required.");

    const firestore = getFirestore();
    const ref = firestore.collection("products").doc(productId);
    const snap = await ref.get();
    if (!snap.exists) throw failedPrecondition("That product no longer exists.");

    const storeId = String(snap.get("storeId") ?? "");
    const isOwner = caller.storeId === storeId;
    if (!isOwner && caller.role !== "ADMIN") {
      throw new HttpsError("permission-denied", "You can only delete products from your own store.");
    }

    const images = (snap.get("images") as Array<{ publicId?: string }>) ?? [];
    await ref.delete();
    await firestore
      .collection("stores")
      .doc(storeId)
      .set({ productCount: FieldValue.increment(-1) }, { merge: true });

    const publicIds = images
      .map((image) => image.publicId)
      .filter((id): id is string => Boolean(id) && id!.startsWith("sproutly/"));

    if (publicIds.length > 0) {
      const { cloudName, apiKey, apiSecret } = parseCloudinaryUrl(CLOUDINARY_URL.value());
      const timestamp = Math.floor(Date.now() / 1000);
      await Promise.all(
        publicIds.map(async (publicId) => {
          const signature = createHash("sha1")
            .update(`public_id=${publicId}&timestamp=${timestamp}` + apiSecret)
            .digest("hex");
          const body = new URLSearchParams({
            public_id: publicId,
            timestamp: String(timestamp),
            api_key: apiKey,
            signature,
          });
          await fetch(`https://api.cloudinary.com/v1_1/${cloudName}/image/destroy`, {
            method: "POST",
            body,
          }).catch(() => undefined);
        }),
      );
    }

    return { ok: true, deletedAssets: publicIds.length };
  },
);
