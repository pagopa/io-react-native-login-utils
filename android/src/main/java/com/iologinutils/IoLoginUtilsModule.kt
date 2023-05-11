package com.iologinutils

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsIntent
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.bridge.WritableArray
import com.facebook.react.module.annotations.ReactModule
import okhttp3.internal.wait
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

@ReactModule(name = IoLoginUtilsModule.name)
class IoLoginUtilsModule(reactContext: ReactApplicationContext?) :
  ReactContextBaseJavaModule(reactContext) {

  //region custom tabs
  @ReactMethod
  fun openAuthenticationSession(url: String?, callbackURLScheme: String?, promise: Promise) {
    CustomTabActivity.promise = promise
    prepareCustomTab(Uri.parse(url), promise)
  }

  @Synchronized
  fun prepareCustomTab(uri: Uri?, promise: Promise) {
    val activity: Activity? = currentActivity
    try {
      val intentBuilder = CustomTabsIntent.Builder()
      val customTabHelper = CustomTabActivityHelper()
      CustomTabActivity.customTabContext = activity
      CustomTabActivity.customTabHelper = customTabHelper
      customTabHelper.bindCustomTabsService(activity, uri)
      synchronized(CustomTabActivityHelper.lock) {
        while (CustomTabActivityHelper.mClient == null) {
          try {
            CustomTabActivityHelper.lock.wait()
          } catch (e: InterruptedException) {
            promise.reject("error", e.message)
          }
        }
      }
      val session = CustomTabActivityHelper.mClient.newSession(object : CustomTabsCallback() {
        override fun onNavigationEvent(navigationEvent: Int, extras: Bundle?) {
          super.onNavigationEvent(navigationEvent, extras)
          if (navigationEvent == TAB_HIDDEN) {
            promise.reject("TabClosed", "Code=1")
          }
        }
      })
      intentBuilder.setSession(session!!)
      customTabHelper.mayLaunchUrl(uri)
      val intent = intentBuilder.build()
      CustomTabActivityHelper.openCustomTab(activity, intent, uri, promise)
    } catch (e: Exception) {
      promise.reject("error", e.message)
    }
  }
  //endregion

  @ReactMethod
  @Throws(IOException::class)
  fun getRedirects(url: String?, headers: ReadableMap, promise: Promise) {
    val urlArray = ArrayList<String>()
    try {
      findRedirects(url, headers, urlArray)
      val urls = urlArray.toTypedArray()
      val resultArray: WritableArray = Arguments.fromArray(urls)
      promise.resolve(resultArray)
    } catch (e: IOException) {
      promise.reject("error", e.message)
    }
  }

  @Throws(IOException::class)
  fun findRedirects(url: String?, headers: ReadableMap, urlArray: ArrayList<String>) {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.instanceFollowRedirects = false
    val headersMap: HashMap<String, Any> = headers.toHashMap()
    for (key in headersMap.keys) {
      val value = headersMap[key].toString()
      connection.setRequestProperty(key, value)
    }
    val responseCode = connection.responseCode
    if (responseCode in 300..399) {
      var redirectUrl = connection.getHeaderField("Location")
      if (redirectUrl.startsWith("/")) {
        val previousUrl = URL(url)
        val redirectScheme = previousUrl.protocol
        val redirectHost = previousUrl.host
        val port = previousUrl.port.toString()
        redirectUrl =
          redirectScheme + "://" + redirectHost + (if (port == "-1") "" else ":$port") + redirectUrl
      }
      urlArray.add(redirectUrl)
      findRedirects(redirectUrl, urlArray)
    } else {
      return
    }
  }

  //region redirects
  @Throws(IOException::class)
  fun findRedirects(url: String?, urlArray: ArrayList<String>) {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.instanceFollowRedirects = false
    val responseCode = connection.responseCode
    if (responseCode in 300..399) {
      var redirectUrl = connection.getHeaderField("Location")
      if (redirectUrl.startsWith("/")) {
        val previousUrl = URL(url)
        val redirectScheme = previousUrl.protocol
        val redirectHost = previousUrl.host
        val port = previousUrl.port.toString()
        redirectUrl =
          redirectScheme + "://" + redirectHost + (if (port == "-1") "" else ":$port") + redirectUrl
      }
      urlArray.add(redirectUrl)
      findRedirects(redirectUrl, urlArray)
    } else {
      return
    }
  }
  //endregion

  companion object {
    const val name = "IoLoginUtils"
  }

  override fun getName() = IoLoginUtilsModule.name
}
