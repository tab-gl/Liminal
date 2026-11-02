import { initializeApp } from "firebase-admin/app";
import { setGlobalOptions } from "firebase-functions/v2";
import { REGION } from "./config";

initializeApp();

// One region for the whole backend: asia-south1 (Mumbai).
setGlobalOptions({
  region: REGION,
  maxInstances: 40,
  concurrency: 40,
  memory: "512MiB",
  timeoutSeconds: 60,
});

// Auth & roles
export { registerSeller, setUserRole } from "./auth/roles";

// Orders
export { createOrder } from "./orders/createOrder";
export { updateOrderStatus, cancelOrder } from "./orders/orderStatus";

// Media (Cloudinary)
export { getCloudinaryUploadSignature, deleteCloudinaryAsset, recordMediaUsage } from "./media/cloudinary";

// Products
export { deleteProduct } from "./products/deleteProduct";

// Admin
export { adminSetStoreStatus, adminSetProductStatus } from "./admin/moderation";

// Notifications
export { onOrderCreated, onOrderStatusChanged } from "./notifications/onOrderEvents";

// Platform
export { refreshPlatformStats } from "./platform/stats";
