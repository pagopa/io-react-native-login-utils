package com.iologinutils

import com.facebook.react.bridge.*
import com.facebook.react.module.annotations.ReactModule
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

@ReactModule(name = IoLoginUtilsModule.name)
class IoLoginUtilsModule(reactContext: ReactApplicationContext?) :
  ReactContextBaseJavaModule(reactContext) {

  //region custom tabs
  @ReactMethod
  fun openAuthenticationSession(url: String, callbackURLScheme: String, promise: Promise) {
    currentActivity?.let { ioActivity ->

      val service = AuthenticationService(ioActivity)
      service.performAuthenticationRequest(url)

    } ?: run {
      promise.reject(
        "NativeAuthSessionError",
        generateErrorObject("MissingActivityOnPrepare", null, null, null)
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
        generateErrorObject("First Request Error", null, null, null)
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
        generateErrorObject("Error while creating connection redirect", null, null, null)
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
        generateErrorObject("Error while creating connection redirect", null, null, null)
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
      val urlParameters = getUrlParameter(url)
      val urlNoQuery = getUrlWithoutQuery(url)
      val errorObject =
        generateErrorObject("Redirecting Error", responseCode, urlNoQuery, urlParameters)
      promise.reject("NativeRedirectError", errorObject)
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
  }

  override fun getName() = IoLoginUtilsModule.name
}
