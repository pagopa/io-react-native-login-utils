package com.iologinutils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent

class AuthenticationService {

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

  fun performAuthenticationRequest(url: String) {
    val uri = Uri.parse(url)
    val customTabIntent = createCustomTabsIntentBuilder().build().intent
    customTabIntent.setData(uri);
    if (TextUtils.isEmpty(customTabIntent.getPackage())) {
      customTabIntent.setPackage(mBrowserHandler.getBrowserPackage());
    }

    Log.d("AuthenticationService", "Using ${customTabIntent.`package`} as browser for auth");
    customTabIntent.putExtra(
      CustomTabsIntent.EXTRA_TITLE_VISIBILITY_STATE,
      CustomTabsIntent.NO_TITLE
    );
    customTabIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

    Log.d("AuthenticationService", "Initiating authentication request to $url");
    mIOContext.startActivity(
      AuthenticationManagerActivity.createStartIntent(
        mIOContext,
        customTabIntent
      )
    );
  }

  fun dispose() {
    if (mDisposed) {
      return
    }
    mBrowserHandler.unbind()
    mDisposed = true
  }

  private fun checkNotDisposed() {
    check(!mDisposed) { "Service has been disposed and rendered inoperable" }
  }

}
