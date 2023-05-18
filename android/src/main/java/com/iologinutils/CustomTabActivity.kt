package com.iologinutils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.View.OnAttachStateChangeListener
import android.widget.FrameLayout
import android.widget.ProgressBar
import com.facebook.react.bridge.Promise
import com.iologinutils.IoLoginUtilsModule.Companion.generateErrorObject

class CustomTabActivity : Activity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    context = this
    super.onCreate(savedInstanceState)
    handleExtra(intent)
    overridePendingTransition(0, 0) // Disable animation
  }

  // If your app is in opened in background and you attempt to open an url, it
  // will call this function instead of onCreate()
  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    context = this
    setIntent(intent)
    handleExtra(getIntent())
  }

  private fun handleExtra(intent: Intent) {
    customTabContext?.let { tabContext ->
        customTabHelper?.unbindCustomTabsService(tabContext)
        intent.data?.let { uri ->
          promise?.resolve(uri.toString())
          emptyStatics()
        } ?: run {
          promise?.reject("NativeAuthSessionError", generateErrorObject("MissingDataFromIntent", null, null, null))
          emptyStatics()
        }
        val progressBar = ProgressBar(tabContext, null, android.R.attr.progressBarStyleLarge)
        progressBar.isIndeterminate = true
        val layoutParams = FrameLayout.LayoutParams(
          FrameLayout.LayoutParams.WRAP_CONTENT,
          FrameLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.gravity = Gravity.CENTER
        val frameLayout = FrameLayout(tabContext)
        frameLayout.addView(progressBar, layoutParams)
        setContentView(frameLayout)
        overridePendingTransition(0, 0)
        frameLayout.addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
          override fun onViewAttachedToWindow(view: View) {
            context?.finish() ?: run {
              promise?.reject("NativeAuthSessionError", generateErrorObject("CustomTabActivityContextIsNull",null,null,null))
              emptyStatics()
            }
          }
          override fun onViewDetachedFromWindow(view: View) {}
        })

    } ?: run {
      promise?.reject("NativeAuthSessionError", generateErrorObject("CustomTabContextIsNull",null,null,null))
      emptyStatics()
    }
  }


  companion object {
    var promise: Promise? = null
    var customTabHelper: CustomTabActivityHelper? = null
    @SuppressLint("StaticFieldLeak")
    var customTabContext: Activity? = null
    @SuppressLint("StaticFieldLeak")
    var context: Activity? = null
  }

  private fun emptyStatics(){
    promise = null
    customTabHelper = null
    customTabContext = null
    context = null
  }
}
