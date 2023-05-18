package com.iologinutils

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import com.facebook.react.bridge.Promise
import com.iologinutils.CustomTabsHelper.getPackageNameToUse
import com.iologinutils.IoLoginUtilsModule.Companion.generateErrorObject
import okhttp3.internal.notifyAll

class CustomTabActivityHelper : CustomTabsServiceConnection() {
  private var mCustomTabsSession: CustomTabsSession? = null
  private var mConnection: CustomTabsServiceConnection? = null
  fun unbindCustomTabsService(activity: Activity) {
    mConnection?.let {
      activity.unbindService(it)
    }
    mClient = null
    mCustomTabsSession = null
    mConnection = null
  }

  private val session: CustomTabsSession?
    get() {
      mClient?.let {
        mCustomTabsSession = mCustomTabsSession ?: it.newSession(null)
      }
      return mCustomTabsSession
    }

  fun bindCustomTabsService(activity: Activity?, uri: Uri?) {
    mClient?.let { return }
    activity?.let { validActivity ->
      val packageName = getPackageNameToUse(validActivity, uri) ?: return
      mConnection = this
      CustomTabsClient.bindCustomTabsService(validActivity, packageName, this)
    }
  }

  fun mayLaunchUrl(uri: Uri?): Boolean = mClient?.let { session?.mayLaunchUrl(uri, null, null) } ?: false

  override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
    synchronized(lock) {
      mClient = client
      client.warmup(0L)
      lock.notifyAll()
    }
  }

  override fun onServiceDisconnected(name: ComponentName) {
    mClient = null
    mCustomTabsSession = null
  }

  companion object {
    var mClient: CustomTabsClient? = null
    var lock = Any()
    fun openCustomTab(
      activity: Activity,
      customTabsIntent: CustomTabsIntent,
      uri: Uri,
      promise: Promise
    ) {
      val packageName = getPackageNameToUse(activity, uri)
      if (packageName == null) {
        promise.reject("NativeAuthSessionError", generateErrorObject("MissingBrowserPackageNameWhileOpening", null, null, null))
        return
      } else {
        customTabsIntent.intent.setPackage(packageName)
        customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        customTabsIntent.launchUrl(activity, uri)
      }
    }
  }
}
