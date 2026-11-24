package com.omegaapi.huesort

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.google.android.gms.ads.MobileAds
import com.omegaapi.huesort.ui.GameScreen
import com.omegaapi.huesort.ui.theme.HueSortTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        MobileAds.initialize(this) {}

        setContent {
            HueSortTheme {
                val adManager = remember { AdManager(this) }
                GameScreen(adManager = adManager)
            }
        }
    }
}
