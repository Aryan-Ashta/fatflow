package com.fatflow.app.widgets

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.text.Text

class FatFlowWideWidget : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FatFlowWideWidgetContent()
}

class FatFlowWideWidgetContent : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            Text(text = "FatFlow")
        }
    }
}