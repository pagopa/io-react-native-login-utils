package com.iologinutils

import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import com.iologinutils.IoLoginError.Companion.generateErrorObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

@ReactModule(name = IoLoginUtilsModule.name)
class IoLoginUtilsModule(reactContext: ReactApplicationContext?) :
  ReactContextBaseJavaModule(reactContext) {

  //region custom tabs
  @ReactMethod
  fun openAuthenticationSession(url: String, callbackURLScheme: String, promise: Promise) {
    authorizationPromise = promise

    currentActivity?.let { ioActivity ->
      AuthenticationService(ioActivity).use { service ->
        service.performAuthenticationRequest((url))
      }
    } ?: run {
      promise.reject(
        "NativeAuthSessionError",
        generateErrorObject(IoLoginError.Type.MISSING_ACTIVITY_ON_PREPARE)
      )
    }
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
        generateErrorObject(IoLoginError.Type.FIRST_REQUEST_ERROR)
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
      val headersMap: HashMap<String, Any> = headers.toHashMap()
      for (key in headersMap.keys) {
        val value = headersMap[key].toString()
        connection.setRequestProperty(key, value)
      }
      handleRedirects(connection, url, urlArray, callbackURLParameter, promise)
    } catch (e: Exception) {
      promise.reject(
        "NativeRedirectError",
        generateErrorObject(IoLoginError.Type.CONNECTION_REDIRECT_ERROR)
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
        generateErrorObject(IoLoginError.Type.CONNECTION_REDIRECT_ERROR)
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
        generateErrorObject(
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
    urlAsURL.query.split("&")
      .forEach {
        val param = it.split("=")
        parameters.add(param[0])
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
  }

  override fun getName() = IoLoginUtilsModule.name
}
