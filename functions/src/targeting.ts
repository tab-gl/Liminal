/** Ad-delivery targeting. Pure, so it can be unit tested without Firebase. */

export interface AdContext {
  categoryId?: string;
  platform?: string;
  country?: string;
}

const PLATFORMS = new Set(["android", "ios", "web", "desktop", "other"]);

function strings(value: unknown): string[] {
  return Array.isArray(value) ? value.filter((v): v is string => typeof v === "string" && v.length > 0) : [];
}

/**
 * Builds the requesting user's delivery context.
 *
 * Country prefers the edge-provided header and falls back to the client hint.
 * Both only choose which ad a person sees, never what an advertiser is billed,
 * so a spoofed value costs nobody money.
 */
export function buildAdContext(
  data: Record<string, unknown> | undefined,
  headers: Record<string, unknown> = {},
): AdContext {
  const platform = typeof data?.platform === "string" ? data.platform.toLowerCase() : undefined;
  const headerCountry = headers["x-appengine-country"];
  const rawCountry = typeof headerCountry === "string" && headerCountry ? headerCountry : data?.country;
  const country =
    typeof rawCountry === "string" && /^[A-Za-z]{2}$/.test(rawCountry) ? rawCountry.toUpperCase() : undefined;
  const categoryId =
    typeof data?.categoryId === "string" && data.categoryId.length <= 64 ? data.categoryId : undefined;
  return {
    categoryId,
    platform: platform && PLATFORMS.has(platform) ? platform : undefined,
    country,
  };
}

/**
 * A campaign matches when every non-empty targeting dimension matches the
 * context. Empty means "no restriction"; a restricted dimension with no context
 * value does not match, so a category campaign never leaks onto the general feed.
 */
export function matchesTargeting(targeting: unknown, ctx: AdContext): boolean {
  const t = (targeting ?? {}) as Record<string, unknown>;
  const categories = strings(t.categoryIds);
  const platforms = strings(t.platforms).map((p) => p.toLowerCase());
  const countries = strings(t.countries).map((c) => c.toUpperCase());

  if (categories.length > 0 && (!ctx.categoryId || !categories.includes(ctx.categoryId))) return false;
  if (platforms.length > 0 && (!ctx.platform || !platforms.includes(ctx.platform))) return false;
  if (countries.length > 0 && (!ctx.country || !countries.includes(ctx.country))) return false;
  return true;
}
