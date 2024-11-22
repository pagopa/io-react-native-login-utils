package com.iologinutils

import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.WritableNativeArray
import com.facebook.react.bridge.WritableNativeMap

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
    fun generateErrorUserInfo(
      error: Type,
      responseCode: Int? = null,
      url: String? = null,
      parameters: List<String>? = null
    ): WritableMap {
      return WritableNativeMap().apply {
        putString("error", error.value)
        url?.let{ it -> putString("url", it)}
        responseCode?.let { it -> putInt("statusCode", it) }
        parameters?.let { params ->
          putArray("parameters", WritableNativeArray().apply {
            params.forEach { param -> pushString(param) }
          })
        }
      }
    }
  }
}
