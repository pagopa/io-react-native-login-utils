package com.iologinutils

import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import com.iologinutils.IoLoginError.Companion.generateErrorUserInfo
import com.iologinutils.browser.BrowserPackageHelper
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.CookieManager
import java.net.CookieHandler
import java.net.CookiePolicy
import java.net.URI

@ReactModule(name = IoLoginUtilsModule.name)
class IoLoginUtilsModule(reactContext: ReactApplicationContext?) :
  ReactContextBaseJavaModule(reactContext) {

  init {
    if (CookieHandler.getDefault() == null) {
      val cookieManager = CookieManager()
      cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL)
      CookieHandler.setDefault(cookieManager)
    }
  }

  //region custom tabs
  @ReactMethod
  fun openAuthenticationSession(
    url: String,
    callbackURLScheme: String,
    @Suppress("UNUSED_PARAMETER") shareiOSCookies: Boolean,
    promise: Promise) {
    authorizationPromise = promise

    currentActivity?.let { ioActivity ->
      AuthorizationService(ioActivity).use { service ->
        service.performAuthorizationRequest((url))
      }
    } ?: run {
      rejectAndClearAuthorizationPromise(
        "NativeAuthSessionError", IoLoginError.Type.MISSING_ACTIVITY_ON_PREPARE
      )
    }
  }

  @ReactMethod
  fun supportsInAppBrowser(promise: Promise) {
    val customTabBrowserPackageName = BrowserPackageHelper.getPackageNameToUse(reactApplicationContext)
    val supportsCustomTabs = !customTabBrowserPackageName.isNullOrEmpty()
    promise.resolve(supportsCustomTabs);
  }

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
      findRedirects(url, headers, urlArray, callbackURLParameter, promise)
      val urls = urlArray.toTypedArray()
      val resultArray: WritableArray = Arguments.fromArray(urls)
      promise.resolve(resultArray)
      return
    } catch (e: IOException) {
      promise.reject(
        "NativeRedirectError",
        "See user info",
        generateErrorUserInfo(IoLoginError.Type.FIRST_REQUEST_ERROR)
      )
      return
    }
  }

  private fun findRedirects(
    url: String,
    headers: ReadableMap,
    urlArray: ArrayList<String>,
    callbackURLParameter: String?,
    promise: Promise
  ) {
    try {
      val connection = URL(url).openConnection() as HttpURLConnection
      connection.instanceFollowRedirects = false
      val headersMap: HashMap<String, Any?> = headers.toHashMap()
      for (key in headersMap.keys) {
        val value = headersMap[key].toString()
        connection.setRequestProperty(key, value)
      }
      handleRedirects(connection, url, urlArray, callbackURLParameter, promise)
    } catch (e: Exception) {
      promise.reject(
        "NativeRedirectError",
        "See user info",
        generateErrorUserInfo(IoLoginError.Type.CONNECTION_REDIRECT_ERROR)
      )
      return
    }

  }

  //region redirects
  private fun findRedirects(
    url: String,
    urlArray: ArrayList<String>,
    callbackURLParameter: String?,
    promise: Promise
  ) {
    try {
      val connection = URL(url).openConnection() as HttpURLConnection
      connection.instanceFollowRedirects = false
      handleRedirects(connection, url, urlArray, callbackURLParameter, promise)
    } catch (e: Exception) {
      promise.reject(
        "NativeRedirectError",
        "See user info",
        generateErrorUserInfo(IoLoginError.Type.CONNECTION_REDIRECT_ERROR)
      )
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
    val serverHeaders = connection.headerFields
    for ((key, values) in serverHeaders) {
      println("🔥 $key: $values")
    }
    val setCookieHeader = serverHeaders["Set-Cookie"] ?: emptyList()
    val javaCookieManager = CookieHandler.getDefault() as CookieManager

    val webkitCookieManager = android.webkit.CookieManager.getInstance()
    webkitCookieManager.setAcceptCookie(true)

    // Sync each cookie to Android WebView/WebKit
    for (cookieString in setCookieHeader) {
      // val cookieString = "${cookie.name}=${cookie.value};"
      println("Cookie string: $cookieString")
      webkitCookieManager.setCookie(url, cookieString)
    }
    webkitCookieManager.flush()

    val checkCookies = webkitCookieManager.getCookie(url)
    println("🤔 ${checkCookies}")

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
      if (getUrlParameter(redirectUrl).contains(callbackURLParameter)) {
        return
      } else {
        findRedirects(redirectUrl, urlArray, callbackURLParameter, promise)
      }
    } else if (responseCode >= 400) {
      promise.reject(
        "NativeRedirectError",
        "See user info",
        generateErrorUserInfo(
          IoLoginError.Type.REDIRECTING_ERROR,
          responseCode,
          url = getUrlWithoutQuery(url),
          parameters = getUrlParameter(url)
        )
      )
      return
    }
  }

  private fun getUrlParameter(url: String): List<String> {
    val urlAsURL = URL(url)
    val parameters = mutableListOf<String>()
    urlAsURL.query?.let {
      it.split("&")
        .forEach {
          val param = it.split("=")
          parameters.add(param[0])
        }
    }
    return parameters
  }

  private fun getUrlWithoutQuery(url: String): String {
    val urlAsURL = URL(url)
    return "${urlAsURL.protocol}://${urlAsURL.authority}${urlAsURL.path}"
  }

  companion object {
    const val name = "IoLoginUtils"

    var authorizationPromise: Promise? = null

    fun rejectAndClearAuthorizationPromise(code: String, errorType: IoLoginError.Type) {
      authorizationPromise?.reject(
        code,
        "See user info",
        generateErrorUserInfo(errorType)
      )
      authorizationPromise = null
    }
  }

  override fun getName() = IoLoginUtilsModule.name
}
