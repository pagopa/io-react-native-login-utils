package com.iologinutils

import com.facebook.react.bridge.WritableArray
import com.facebook.react.bridge.WritableNativeArray
import org.json.JSONObject


class IoLoginError {
  enum class Type(val value: String) {
    MISSING_ACTIVITY_ON_PREPARE("MissingActivityOnPrepare"),
    FIRST_REQUEST_ERROR("FirstRequestError"),
    CONNECTION_REDIRECT_ERROR("ConnectionRedirectError"),
    REDIRECTING_ERROR("RedirectingError"),
    NATIVE_AUTH_SESSION_CLOSED("NativeAuthSessionClosed"),
    BROWSER_NOT_FOUND("BrowserNotFound")
  }

  companion object {
    fun generateErrorObject(
      error: Type,
      responseCode: Int? = null,
      url: String? = null,
      parameters: List<String>? = null
    ): String {
      val json = JSONObject()
      json.put("error", error.value)
      if (responseCode != null) {
        json.put("statusCode", responseCode)
      }
      if (url != null) {
        json.put("url", url)
        if (parameters != null) {
          val writableArray: WritableArray = WritableNativeArray()
          for (str in parameters) {
            writableArray.pushString(str)
          }
          json.put("parameters", writableArray)
        }
      }
      return json.toString()
    }
  }

}





