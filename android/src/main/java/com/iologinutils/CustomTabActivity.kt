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
    try {
      customTabHelper!!.unbindCustomTabsService(customTabContext!!)
      val uri = intent.data
      if (uri != null) {
        promise!!.resolve(uri.toString())
      }
    } catch (e: Exception) {
      promise!!.reject("error", "Error in resolving promise")
    } finally {
      val progressBar = ProgressBar(customTabContext, null, android.R.attr.progressBarStyleLarge)
      progressBar.isIndeterminate = true
      val layoutParams = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT
      )
      layoutParams.gravity = Gravity.CENTER
      val frameLayout = FrameLayout(customTabContext!!)
      frameLayout.addView(progressBar, layoutParams)
      setContentView(frameLayout)
      overridePendingTransition(0, 0)
      frameLayout.addOnAttachStateChangeListener(object : OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(view: View) {
          context!!.finish()
        }

        override fun onViewDetachedFromWindow(view: View) {}
      })
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
}
