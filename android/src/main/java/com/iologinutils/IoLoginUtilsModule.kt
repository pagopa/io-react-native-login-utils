package com.iologinutils

import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import com.iologinutils.IoLoginError.Companion.generateErrorUserInfo
import com.iologinutils.browser.BrowserPackageHelper
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

@ReactModule(name = IoLoginUtilsModule.NAME)
class IoLoginUtilsModule(reactContext: ReactApplicationContext?) :
  ReactContextBaseJavaModule(reactContext) {

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
    promise.resolve(supportsCustomTabs)
  }

  @ReactMethod
  @Throws(IOException::class)
  fun getRedirects(
    url: String,
    headers: ReadableMap,
    callbackURLParameter: String?,
    promise: Promise
  ) {

    val urlArray = arrayListOf<String>()

    try {
      findRedirects(url, headers, urlArray,  callbackURLParameter, promise) { result ->
        val urls = result.toTypedArray()
        val resultArray: WritableArray = Arguments.fromArray(urls)
        promise.resolve(resultArray)
      }
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
    headers: ReadableMap? = null,
    urlArray: ArrayList<String>,
    callbackURLParameter: String?,
    promise: Promise,
    onComplete: (urlArray: ArrayList<String>) -> Unit
  ) {
    try {
      val connection = URL(url).openConnection() as HttpURLConnection
      connection.instanceFollowRedirects = false
      headers?.toHashMap()?.forEach { (key, value) ->
        connection.setRequestProperty(key, value.toString())
      }
      handleRedirects(connection, url, urlArray, callbackURLParameter, promise, onComplete)
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
    promise: Promise,
    onComplete: (urlArray: ArrayList<String>) -> Unit
  ) {
    findRedirects(url, null, urlArray, callbackURLParameter, promise, onComplete)
  }

  private fun handleRedirects(
    connection: HttpURLConnection,
    url: String,
    urlArray: ArrayList<String>,
    callbackURLParameter: String?,
    promise: Promise,
    onComplete: (urlArray: ArrayList<String>) -> Unit
  ) {
    val responseCode = connection.responseCode
    val serverHeaders = connection.headerFields
    if (BuildConfig.DEBUG) {
      for ((key, values) in serverHeaders) {
        debugLog(">>> $key: $values")
      }
    }
    val setCookieHeader = serverHeaders["Set-Cookie"] ?: emptyList()

    val webkitCookieManager = android.webkit.CookieManager.getInstance()
    webkitCookieManager.setAcceptCookie(true)

    // Sync each cookie to Android WebView/WebKit
    syncCookies(url, setCookieHeader) {
      if (BuildConfig.DEBUG) {
        val lookForWebKitCookies = webkitCookieManager.getCookie(url)
        debugLog("<<< $lookForWebKitCookies")
      }

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
          onComplete(urlArray)
          return@syncCookies
        } else {
          findRedirects(redirectUrl, urlArray, callbackURLParameter, promise, onComplete)
          return@syncCookies
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
        return@syncCookies
      }
    }
  }

  private fun getUrlParameter(url: String): List<String> {
    val urlAsURL = URL(url)
    val parameters = mutableListOf<String>()
    urlAsURL.query?.let { queryString ->
      queryString.split("&")
        .forEach { queryParam ->
          val param = queryParam.split("=")
          parameters.add(param[0])
        }
    }
    return parameters
  }

  private fun getUrlWithoutQuery(url: String): String {
    val urlAsURL = URL(url)
    return "${urlAsURL.protocol}://${urlAsURL.authority}${urlAsURL.path}"
  }

  private fun debugLog(message: String) {
    if (BuildConfig.DEBUG) {
      println(message)
    }
  }

  private fun syncCookies(url: String, cookies: List<String>, onComplete: () -> Unit) {
    val webkitCookieManager = android.webkit.CookieManager.getInstance()
    webkitCookieManager.setAcceptCookie(true)

    fun setNext(index: Int) {
      if (index >= cookies.size) {
        webkitCookieManager.flush()
        onComplete()
        return
      }
      val cookieString = cookies[index]
      debugLog("$$$ Cookie string: $cookieString")
      webkitCookieManager.setCookie(url, cookieString) { _ /*success true/false*/ ->
        setNext(index + 1)
      }
    }
    setNext(0)
  }
  //endregion

  companion object {
    const val NAME = "IoLoginUtils"

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

  override fun getName() = NAME
}
