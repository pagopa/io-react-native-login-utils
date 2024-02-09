package com.iologinutils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import com.iologinutils.browser.BrowserHandler
import java.io.Closeable


/**
 * Dispatches requests to a PagoPA or IDP service. Note that instances of this class
 * <em>must be manually disposed</em> when no longer required, to avoid leaks
 * (see {@link #close()}.
 */
class AuthorizationService(
  private val context: Context,
  private val browserHandler: BrowserHandler = BrowserHandler(context)
) : Closeable {

  private var disposed = false

  private fun createCustomTabsIntentBuilder(): CustomTabsIntent.Builder {
    checkNotDisposed()

    return browserHandler.createCustomTabsIntentBuilder()
  }

  /**
   * Sends an authorization request to the authorization service, using a
   * <a href="https://developer.chrome.com/multidevice/android/customtabs">custom tab</a>
   * if available.
   * If the user cancels the authorization request, the current activity will regain control.
   */
  fun performAuthorizationRequest(url: String) {
    checkNotDisposed()

    val uri = Uri.parse(url)
    val customTabsIntent = createCustomTabsIntentBuilder().build()

    val authIntent = prepareAuthorizationRequestIntent(uri, customTabsIntent)
    val startIntent = AuthorizationManagerActivity.createStartIntent(
      context,
      authIntent,
    )

    Log.d(TAG, "Initiating authorization request to $url")

    // Calling start activity from outside an activity requires FLAG_ACTIVITY_NEW_TASK.
    if (!isActivity(context)) {
      startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    context.startActivity(startIntent)
  }

  private fun prepareAuthorizationRequestIntent(
    requestUri: Uri,
    customTabsIntent: CustomTabsIntent
  ): Intent {
    checkNotDisposed()

    val authIntent = customTabsIntent.intent
    authIntent.setData(requestUri)
    if (TextUtils.isEmpty(authIntent.getPackage())) {
      authIntent.setPackage(browserHandler.getBrowserPackage())
    }

    Log.d(TAG, "Using ${authIntent.`package`} as browser for auth")

    return authIntent
  }

  private fun isActivity(context: Context): Boolean {
    while (context is ContextWrapper) {
      if (context is Activity) {
        return true
      }
    }
    return false
  }

  override fun close() {
    if (disposed) {
      return
    }
    browserHandler.unbind()
    disposed = true

    Log.d(TAG, "AuthorizationService disposed")
  }

  private fun checkNotDisposed() {
    check(!disposed) { "Service has been disposed and rendered inoperable" }
  }

  companion object {
    private val TAG = AuthorizationService::class.java.simpleName
  }
}
