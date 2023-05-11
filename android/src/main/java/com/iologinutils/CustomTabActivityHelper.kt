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
import okhttp3.internal.notifyAll

class CustomTabActivityHelper : CustomTabsServiceConnection() {
  private var mCustomTabsSession: CustomTabsSession? = null
  private var mConnection: CustomTabsServiceConnection? = null
  fun unbindCustomTabsService(activity: Activity) {
    if (mConnection == null) return
    activity.unbindService(mConnection!!)
    mClient = null
    mCustomTabsSession = null
    mConnection = null
  }

  private val session: CustomTabsSession?
    get() {
      if (mClient == null) {
        mCustomTabsSession = null
      } else if (mCustomTabsSession == null) {
        mCustomTabsSession = mClient!!.newSession(null)
      }
      return mCustomTabsSession
    }

  fun bindCustomTabsService(activity: Activity?, uri: Uri?) {
    if (mClient != null) return
    val packageName = getPackageNameToUse(activity!!, uri) ?: return
    mConnection = this
    CustomTabsClient.bindCustomTabsService(activity, packageName,
      mConnection as CustomTabActivityHelper
    )
  }

  fun mayLaunchUrl(uri: Uri?): Boolean {
    if (mClient == null) return false
    val session = session ?: return false
    return session.mayLaunchUrl(uri, null, null)
  }

  override fun onCustomTabsServiceConnected(name: ComponentName, client: CustomTabsClient) {
    synchronized(lock) {
      mClient = client
      mClient!!.warmup(0L)
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
      activity: Activity?,
      customTabsIntent: CustomTabsIntent,
      uri: Uri?,
      promise: Promise
    ) {
      val packageName = getPackageNameToUse(activity!!, uri)
      if (packageName == null) {
        promise.reject("error", "missing browser")
      } else {
        customTabsIntent.intent.setPackage(packageName)
        customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
        customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        customTabsIntent.launchUrl(activity, uri!!)
      }
    }
  }
}
