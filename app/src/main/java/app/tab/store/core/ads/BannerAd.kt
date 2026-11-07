package app.tab.store.core.ads

import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import app.tab.store.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * A banner ad.
 *
 * The unit id comes from the build (`ADMOB_BANNER_UNIT`) and defaults to Google's
 * official test unit, so a build with no AdMob account still shows a clearly
 * labelled test ad instead of crashing or showing nothing.
 *
 * Placement rule: never between a person and an action. Ads sit under the
 * storefront's own content — not over a button, not inside an install flow.
 */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    // The AdView is destroyed when it leaves composition — a banner inside a
    // scrolling list is created and thrown away repeatedly otherwise, which both
    // leaks and makes scrolling stutter.
    val holder = remember { mutableStateOf<AdView?>(null) }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { context ->
            FrameLayout(context).apply {
                val view = AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = BuildConfig.ADMOB_BANNER_UNIT
                    loadAd(AdRequest.Builder().build())
                }
                holder.value = view
                addView(view)
            }
        },
    )

    DisposableEffect(Unit) {
        onDispose {
            holder.value?.destroy()
            holder.value = null
        }
    }
}
