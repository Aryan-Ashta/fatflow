package com.fatflow.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fatflow.app.ui.FatFlowNav
import com.fatflow.app.ui.theme.FatFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as FatFlowApp).container
        val deepLink = intent?.data?.toString()?.contains("barcode") == true
        enableEdgeToEdge()
        setContent {
            FatFlowTheme {
                FatFlowNav(container, deepLinkBarcode = deepLink)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}