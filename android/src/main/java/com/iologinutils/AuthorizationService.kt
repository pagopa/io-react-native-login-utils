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
class AuthorizationService : Closeable {

  private val mIOContext: Context
  private val mBrowserHandler: BrowserHandler
  private var mDisposed = false

  constructor(context: Context) : this(
    context,
    BrowserHandler((context)),
  )

  constructor(
    context: Context,
    browserHandler: BrowserHandler,
  ) {
    mIOContext = context
    mBrowserHandler = browserHandler
  }

  private fun createCustomTabsIntentBuilder(): CustomTabsIntent.Builder {
    checkNotDisposed()

    return mBrowserHandler.createCustomTabsIntentBuilder()
  }

  /**
   * Sends an authorization request to the authorization service, using a
   * <a href="https://developer.chrome.com/multidevice/android/customtabs">custom tab</a>
   * if available.
   * If the user cancels the authorization request, the current activity will regain control.
   */
  fun performAuthorizationRequest(url: String) {
    checkNotDisposed();

    val uri = Uri.parse(url)
    val customTabsIntent = createCustomTabsIntentBuilder().build()

    val authIntent = prepareAuthorizationRequestIntent(uri, customTabsIntent);
    val startIntent = AuthorizationManagerActivity.createStartIntent(
      mIOContext,
      authIntent,
    )

    Log.d("AuthorizationService", "Initiating authorization request to $url");

    // Calling start activity from outside an activity requires FLAG_ACTIVITY_NEW_TASK.
    if (!isActivity(mIOContext)) {
      startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    mIOContext.startActivity(startIntent);
  }

  private fun prepareAuthorizationRequestIntent(
    requestUri: Uri,
    customTabsIntent: CustomTabsIntent
  ): Intent {
    checkNotDisposed()

    val authIntent = customTabsIntent.intent
    authIntent.setData(requestUri);
    if (TextUtils.isEmpty(authIntent.getPackage())) {
      authIntent.setPackage(mBrowserHandler.getBrowserPackage());
    };

    Log.d("AuthorizationService", "Using ${authIntent.`package`} as browser for auth")

    return authIntent;
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
    if (mDisposed) {
      return
    }
    mBrowserHandler.unbind()
    mDisposed = true

    Log.d("AuthorizationService", "AuthorizationService disposed")
  }

  private fun checkNotDisposed() {
    check(!mDisposed) { "Service has been disposed and rendered inoperable" }
  }

}