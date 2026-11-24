package com.omegaapi.huesort

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Thin wrapper around Google Mobile Ads.
 *
 * Interstitial ads appear after completed levels. Rewarded ads unlock only
 * in-game helpers (hint, shuffle, extra tube) and never promise real money.
 */
class AdManager(context: Context) {

    private val appContext = context.applicationContext
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null

    init {
        loadInterstitial()
        loadRewarded()
    }

    private fun adRequest(): AdRequest = AdRequest.Builder().build()

    fun loadInterstitial() {
        InterstitialAd.load(
            appContext,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            adRequest(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    fun loadRewarded() {
        RewardedAd.load(
            appContext,
            BuildConfig.ADMOB_REWARDED_ID,
            adRequest(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                }
            }
        )
    }

    /** Call only when the player chooses to continue after a level. */
    fun showInterstitial(activity: Activity, shouldShow: Boolean) {
        if (!shouldShow) return
        val ad = interstitialAd
        if (ad == null) {
            loadInterstitial()
            return
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                loadInterstitial()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                interstitialAd = null
                loadInterstitial()
            }
        }
        ad.show(activity)
    }

    /** Returns false when no ad is ready, so no reward is granted for free. */
    fun showRewarded(activity: Activity, onReward: () -> Unit): Boolean {
        val ad = rewardedAd
        if (ad == null) {
            loadRewarded()
            return false
        }
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadRewarded()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedAd = null
                loadRewarded()
            }
        }
        ad.show(activity) { onReward() }
        return true
    }
}
