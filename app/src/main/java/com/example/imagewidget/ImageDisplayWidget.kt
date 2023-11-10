package com.example.imagewidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.RemoteViews
import java.io.IOException

/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in [ImageDisplayWidgetConfigureActivity]
 */
class ImageDisplayWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d("WidgetUpdate", "onUpdate called with widgetIds: ${appWidgetIds.contentToString()}")
        // There may be multiple widgets active, so update all of them
        // Iterate over all widget instances and update them
        for (appWidgetId in appWidgetIds) {
            val imageUriString = loadImageUriPref(context, appWidgetId)
            Log.d("WidgetUpdate", "onUpdate: loaded image URI for widget $appWidgetId: $imageUriString")
            val imageUri = if (imageUriString?.isNotEmpty() == true) Uri.parse(imageUriString) else null
            updateAppWidget(context, appWidgetManager, appWidgetId, imageUri)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // When the user deletes the widget, delete the preference associated with it.

        for (appWidgetId in appWidgetIds) {
            deleteTitlePref(context, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    companion object {
        private const val TAG = "ImageDisplayWidget"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            imageUri: Uri?
        ) {
            Thread {
                val bitmap = try {
                    if (imageUri != null) {
                        Log.d(TAG, "Attempting to load image URI on widget $appWidgetId: $imageUri")
                        context.contentResolver.openInputStream(imageUri)?.use { inputStream ->
                            BitmapFactory.decodeStream(inputStream)
                        }
                    } else {
                        null
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Error loading image bitmap", e)
                    null
                }

                // Update the widget on the main thread
                if (bitmap != null) {
                    val views = RemoteViews(context.packageName, R.layout.image_display_widget)
                    views.setImageViewBitmap(R.id.imageView, bitmap)
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                    Log.d(TAG, "Bitmap set on widget ID $appWidgetId")
                } else {
                    Log.e(TAG, "Bitmap is null for widget ID $appWidgetId")
                }
            }.start()
        }
    }
}

