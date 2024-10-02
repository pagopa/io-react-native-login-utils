package com.iologinutils

import com.facebook.react.bridge.WritableNativeArray
import org.json.JSONObject

class IoLoginError {
  enum class Type(val value: String) {
    MISSING_ACTIVITY_ON_PREPARE("MissingActivityOnPrepare"),
    FIRST_REQUEST_ERROR("FirstRequestError"),
    CONNECTION_REDIRECT_ERROR("ConnectionRedirectError"),
    REDIRECTING_ERROR("RedirectingError"),
    NATIVE_AUTH_SESSION_CLOSED("NativeAuthSessionClosed"),
    BROWSER_NOT_FOUND("BrowserNotFound"),
    ILLEGAL_STATE_EXCEPTION("IllegalStateException"),
    ANDROID_SYSTEM_FAILURE("AndroidSystemFailure")
  }

  companion object {
    fun generateErrorObject(
      error: Type,
      responseCode: Int? = null,
      url: String? = null,
      parameters: List<String>? = null
    ): String {
      return JSONObject().apply {
        put("error", error.value)
        responseCode?.let { code -> put("statusCode", code) }
        url?.let {url ->
          put("url", url)
          parameters?.let { params ->
            put("parameters", WritableNativeArray().apply {
              params.forEach { param -> pushString(param) }
            })
          }
        }
      }.toString()
    }
  }
}
