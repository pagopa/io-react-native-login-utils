package com.iologinutils

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsCallback
import androidx.browser.customtabs.CustomTabsIntent
import com.facebook.react.bridge.*
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
      val session = CustomTabActivityHelper.mClient?.newSession(object : CustomTabsCallback() {
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

  var generatedError: Boolean = false;

  @ReactMethod
  @Throws(IOException::class)
  fun getRedirects(
    url: String,
    headers: ReadableMap,
    callbackURLParameter: String?,
    promise: Promise
  ) {
    val urlArray = ArrayList<String>()
    try {
      findRedirects(url, headers, urlArray,callbackURLParameter,promise)
      if(!generatedError) {
        val urls = urlArray.toTypedArray()
        val resultArray: WritableArray = Arguments.fromArray(urls)
        promise.resolve(resultArray)
      }

      return
    } catch (e: IOException) {
      promise.reject("NativeRedirectError", generateErrorObject("First Request Error",null,null,null))
      return
    }
  }

  fun findRedirects(url: String, headers: ReadableMap, urlArray: ArrayList<String>, callbackURLParameter: String?, promise: Promise) {
    try {
      val connection = URL(url).openConnection() as HttpURLConnection
      connection.instanceFollowRedirects = false
      val headersMap: HashMap<String, Any> = headers.toHashMap()
      for (key in headersMap.keys) {
        val value = headersMap[key].toString()
        connection.setRequestProperty(key, value)
      }
      handleRedirects(connection,url,urlArray,callbackURLParameter,promise)
    }catch (e: Exception){
      promise.reject("NativeRedirectError", generateErrorObject("Error while creating connection redirect",null,null,null))
      return
    }

  }

  //region redirects
  fun findRedirects(url: String, urlArray: ArrayList<String>,  callbackURLParameter: String?, promise: Promise) {
    try {
    val connection = URL(url).openConnection() as HttpURLConnection
    connection.instanceFollowRedirects = false
    handleRedirects(connection,url,urlArray,callbackURLParameter,promise)
    }
    catch (e: Exception){
      promise.reject("NativeRedirectError", generateErrorObject("Error while creating connection redirect",null,null,null))
      return
    }
  }
  //endregion

  private fun handleRedirects(
    connection: HttpURLConnection,
    url: String,
    urlArray: ArrayList<String>,
    callbackURLParameter: String?,
    promise: Promise
  ) {
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
      if(getUrlParameter(redirectUrl).contains(callbackURLParameter)){
        return
      } else {
        findRedirects(redirectUrl, urlArray, callbackURLParameter,promise)
      }
    }
    else if (responseCode >= 400){
      val urlParameters = getUrlParameter(url)
      val urlNoQuery = getUrlWithoutQuery(url)
      val errorObject = generateErrorObject("RedirectingError",responseCode,urlNoQuery,urlParameters)
      promise.reject("NativeRedirectError",errorObject)
      return
    }
  }

  private fun getUrlParameter(url: String): List<String> {
    val urlAsURL = URL(url)
    val parameters = mutableListOf<String>()
    urlAsURL.query.split("&")
      .forEach {
        val param = it.split("=")
        parameters.add(param[0])
      }
    return parameters
  }

  private fun getUrlWithoutQuery(url:String): String {
    val urlAsURL = URL(url)
    return "${urlAsURL.protocol}://${urlAsURL.authority}${urlAsURL.path}"
  }

  private fun generateErrorObject(error:String, responseCode: Int?, url:String?, parameters: List<String>?): WritableMap {
    val errorObject = WritableNativeMap()
    errorObject.putString("Error", error)
    if(responseCode != null){
      errorObject.putInt("StatusCode", responseCode)
    }
    if(url != null) {
      errorObject.putString("URL", url)

      if (parameters != null) {
        val writableArray: WritableArray = WritableNativeArray()
        for (str in parameters) {
          writableArray.pushString(str)
        }
        errorObject.putArray("Parameters",writableArray)
      }
    }
    generatedError = true;
    return errorObject
  }

  companion object {
    const val name = "IoLoginUtils"
  }

  override fun getName() = IoLoginUtilsModule.name
}
