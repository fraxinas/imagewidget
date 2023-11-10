package com.example.imagewidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.RemoteViews
import androidx.appcompat.app.AppCompatActivity
import com.example.imagewidget.databinding.ImageDisplayWidgetConfigureBinding


/**
 * The configuration screen for the [ImageDisplayWidget] AppWidget.
 */
class ImageDisplayWidgetConfigureActivity : AppCompatActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var appWidgetText: EditText
    private var onClickListener = View.OnClickListener {
        val context = this@ImageDisplayWidgetConfigureActivity

        // When the button is clicked, store the string locally
        val widgetText = appWidgetText.text.toString()
        saveTitlePref(context, appWidgetId, widgetText)

        // It is the responsibility of the configuration activity to update the app widget
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val imageUri = loadImageUriPref(context, appWidgetId)?.let { Uri.parse(it) }
        if (imageUri != null) {
            updateAppWidget(context, appWidgetManager, appWidgetId, imageUri)
        } else {
            Log.e("WidgetConfig", "No image URI to update widget with.")
        }

        // Make sure we pass back the original appWidgetId
        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(RESULT_OK, resultValue)
        finish()
    }
    private lateinit var binding: ImageDisplayWidgetConfigureBinding

    public override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        // Set the result to CANCELED.  This will cause the widget host to cancel
        // out of the widget placement if the user presses the back button.
        setResult(RESULT_CANCELED)

        binding = ImageDisplayWidgetConfigureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appWidgetText = binding.appwidgetText as EditText
        binding.addButton.setOnClickListener(onClickListener)

        // Find the widget id from the intent.
        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        appWidgetText.setText(loadTitlePref(this@ImageDisplayWidgetConfigureActivity, appWidgetId))

        // Determine the app widget ID from the intent that launched the activity.
        intent?.extras?.let {
            appWidgetId = it.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        }

        // If the app widget ID is invalid, close the activity.
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
        }
        openImagePicker()
    }

    // Add to ImageDisplayWidgetConfigureActivity.kt
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_IMAGE_PICK && resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = data?.data

            Log.d("WidgetConfig", "Image URI received: $imageUri")

            if (imageUri != null) {
                try {
                    contentResolver.takePersistableUriPermission(
                        imageUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    Log.d("WidgetConfig", "Persistable URI permission taken")

                    // Save the URI to SharedPreferences
                    saveImageUriPref(this, appWidgetId, imageUri.toString())
                    Log.d("WidgetConfig", "Image URI saved to SharedPreferences")

                    // Request an update for the current widget
                    val appWidgetManager = AppWidgetManager.getInstance(this)
                    val views = RemoteViews(this.packageName, R.layout.image_display_widget)
                    updateAppWidget(this, appWidgetManager, appWidgetId, imageUri)

                    // Confirm the widget update to the home screen
                    val resultValue = Intent().apply {
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    }
                    setResult(RESULT_OK, resultValue)
                    finish()

                    val updateIntent = Intent(this, ImageDisplayWidget::class.java).apply {
                        action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(appWidgetId))
                    }

                    sendBroadcast(updateIntent)

                } catch (e: SecurityException) {
                    Log.e("WidgetConfig", "Error taking persistable URI permission", e)
                }
            } else {
                Log.e("WidgetConfig", "Image URI was null")
            }
        } else {
            Log.e("WidgetConfig", "Unexpected result: requestCode=$requestCode, resultCode=$resultCode")
        }
    }


    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        startActivityForResult(intent, REQUEST_CODE_IMAGE_PICK)
    }

    companion object {
        private const val REQUEST_CODE_IMAGE_PICK = 1
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            imageUri: Uri
        ) {
            val views = RemoteViews(context.packageName, R.layout.image_display_widget)
            if (imageUri != null) {
                Log.d("WidgetUpdate", "Attempting to set image URI on widget $appWidgetId: $imageUri")
                views.setImageViewUri(R.id.imageView, imageUri)
            } else {
                Log.e("WidgetUpdate", "imageUri is null for widget $appWidgetId")
            }

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
            Log.d("WidgetUpdate", "Widget should be updated now")
        }

    }

}

const val PREFS_NAME = "com.example.imagewidget.ImageDisplayWidget"
const val PREF_PREFIX_KEY_IMAGE = "appwidget_image_"
const val PREF_PREFIX_KEY_TEXT = "appwidget_text_"

fun saveImageUriPref(context: Context, appWidgetId: Int, uri: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
    prefs.putString(PREF_PREFIX_KEY_IMAGE + appWidgetId, uri)
    prefs.apply()
    Log.d("WidgetConfig", "Saved image URI: $uri for widgetId: $appWidgetId")
}

fun loadImageUriPref(context: Context, appWidgetId: Int): String? {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val uri = prefs.getString(PREF_PREFIX_KEY_IMAGE + appWidgetId, null)
    Log.d("WidgetPrefs", "Loaded image URI from SharedPreferences for widget ID: $appWidgetId - URI: $uri")
    return uri
}

fun saveTitlePref(context: Context, appWidgetId: Int, text: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
    prefs.putString(PREF_PREFIX_KEY_TEXT + appWidgetId, text)
    prefs.apply()
    Log.d("WidgetConfig", "Saved title text: $text for widgetId: $appWidgetId")
}

fun loadTitlePref(context: Context, appWidgetId: Int): String {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val text = prefs.getString(PREF_PREFIX_KEY_TEXT + appWidgetId, null)
    return text ?: context.getString(R.string.appwidget_text)
}

fun deleteTitlePref(context: Context, appWidgetId: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
    prefs.remove(PREF_PREFIX_KEY_TEXT + appWidgetId)
    prefs.apply()
}