package app.tab.store.ads

import android.widget.FrameLayout
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import app.tab.store.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * A 320x50 banner.
 *
 * The unit id swaps for Google's official test unit in debug builds - clicking
 * your own live ads is the fastest way to lose the AdMob account. Every
 * placement is captioned "Ad", because Sproutly labels all paid inventory
 * including its own.
 */
private const val PRODUCTION_BANNER_UNIT = "ca-app-pub-9808362123414569/3733801140"
private const val TEST_BANNER_UNIT = "ca-app-pub-3940256099942544/6300978111"

@Composable
fun AdMobBanner(
    modifier: Modifier = Modifier,
    personalised: Boolean = true,
) {
    val unitId = if (BuildConfig.DEBUG) TEST_BANNER_UNIT else PRODUCTION_BANNER_UNIT
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Text(
            text = "Ad",
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AndroidView(
            modifier = Modifier.fillMaxWidth().height(50.dp),
            factory = { context ->
                FrameLayout(context).apply {
                    val adView = AdView(context)
                    adView.setAdSize(AdSize.BANNER)
                    adView.adUnitId = unitId
                    addView(adView)
                    val request = AdRequest.Builder().apply {
                        if (!personalised) addKeyword("non_personalized_ads")
                    }.build()
                    adView.loadAd(request)
                }
            },
        )
    }
}
