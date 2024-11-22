package com.iologinutils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log

/**
 * Stores state and handles events related to the authorization flow. The activity is
 * started by {@link AuthorizationService#performAuthorizationRequest
 * AuthorizationService.performAuthorizationRequest}, and records all state pertinent to
 * the authorization request before invoking the authorization intent. It also functions
 * to control the back stack, ensuring that the authorization activity will not be reachable
 * via the back button after the flow completes.
 *
 * <p>The following diagram illustrates the operation of the activity:
 *
 * <pre>
 *                          Back Stack Towards Top
 *                +------------------------------------------>
 *
 * +------------+      +---------------+      +----------------+      +--------------+
 * |            | (1)  |               | (2)  |                | (S1) |              |
 * | Initiating +----->| Authorization +----->| Authorization  +----->| Redirect URI |
 * |  Activity  |      |    Manager    |      |   Activity     |      |   Receiver   |
 * |    (IO)    |<-----+   Activity    |<-----+ (e.g. browser) |      |   Activity   |
 * |            | (C2) |               | (C1) |                |      |              |
 * +------------+      +-+--+----------+      +----------------+      +-------+------+
 *           ^            |     ^                                              |
 *           |    (S3)    |     |                      (S2)                    |
 *           +------------+     +----------------------------------------------+
 *
 *
 * </pre>
 *
 * <p>The process begins with an activity requesting that an authorization flow be started,
 * using {@link AuthorizationService#performAuthorizationRequest}.
 *
 * <ul>
 *   <li>Step 1: Using an intent derived from {@link #createStartIntent}, this activity is
 *       started. The state delivered in this intent is recorded for future use.
 *   <li>Step 2: The authorization intent, typically a browser tab, is started. At this point,
 *       depending on user action, we will either end up in a "completion" flow (S) or
 *       "cancellation flow" (C).
 *
 *   <li>C flow:
 *     <ul>
 *       <li>Step C1: If the user presses the back button or otherwise causes the
 *           authorization activity to finish, the AuthorizationManagementActivity will be
 *           recreated or restarted.
 *       <li>Step C2: The Promise contained in the AuthorizationService is rejected with the
 *           with {@link IoLoginError#Type#NATIVE_AUTH_SESSION_CLOSED} error.
 *     </ul>
 *   <li>S flow:
 *     <ul>
 *       <li>Step S1: The authorization activity completes with a success of failure, and sends
 *           this result to {@link RedirectUriReceiverActivity}.
 *       <li>Step S2: {@link RedirectUriReceiverActivity} extracts the forwarded data, and
 *           invokes AuthorizationManagementActivity using an intent derived from
 *           {@link #createResponseHandlingIntent}. This intent has flag CLEAR_TOP set, which
 *           will result in both the authorization activity and
 *           {@link RedirectUriReceiverActivity} being destroyed, if necessary, such that
 *           AuthorizationManagementActivity is once again at the top of the back stack.
 *       <li>Step S3: The Promise contained in the AuthorizationService is resolved with the
 *  *        received data.
 *     </ul>
 * </ul>
 */
class AuthorizationManagerActivity : Activity() {

  private var authorizationStarted = false
  private var authIntent: Intent? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    intent.extras?.let {
      extractState(it)
    } ?: savedInstanceState?.let {
      extractState(it)
    } ?: run {
      IoLoginUtilsModule.rejectAndClearAuthorizationPromise(
        "NativeAuthSessionError",
        IoLoginError.Type.ILLEGAL_STATE_EXCEPTION
      )
    }
  }

  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    setIntent(intent)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.apply {
      putBoolean(KEY_AUTHORIZATION_STARTED, authorizationStarted)
      putParcelable(KEY_AUTH_INTENT, authIntent)
    }
  }

  override fun onResume() {
    super.onResume()

    if (!authorizationStarted) {
      try {
        startActivity(authIntent)
        authorizationStarted = true
      } catch (e: ActivityNotFoundException) {
        handleBrowserNotFound()
        finish()
      } catch (e: NullPointerException) {
        // If the system is low on resources, it may decide to kill background activities.
        // This means that, upon returning to this activity from RedirectUriReceiverActivity,
        // two instances of AuthorizationManagerActivity are spawned (the first that started the
        // Custom Tabs Activity and the second that was started from RedirectUriReceiverActivity).
        // On some devices and Android versions, this results in a null intent that is passed to the
        // 'startActivity' method, called in onResume
        handleNullPointerException()
        finish()
      }
      return
    }

    intent.data?.let { responseUri ->
      handleAuthorizationComplete(responseUri)
    } ?: run {
      handleAuthorizationCanceled()
    }

    finish()
  }

  private fun handleAuthorizationComplete(responseUri: Uri) {
    Log.d(
      TAG,
      "Authorization complete - invoking completion intent with $responseUri"
    )
    IoLoginUtilsModule.authorizationPromise?.resolve(responseUri.toString())
    IoLoginUtilsModule.authorizationPromise = null
  }

  private fun handleAuthorizationCanceled() {
    Log.d(TAG, "Authorization flow canceled by user")

    IoLoginUtilsModule.rejectAndClearAuthorizationPromise(
      "NativeAuthSessionError",
      IoLoginError.Type.NATIVE_AUTH_SESSION_CLOSED
    )
  }

  private fun extractState(state: Bundle) {
    state.apply {
      authIntent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        state.getParcelable(KEY_AUTH_INTENT, Intent::class.java)
      } else {
        @Suppress("DEPRECATION")
        state.getParcelable(KEY_AUTH_INTENT)
      }
      authorizationStarted = getBoolean(KEY_AUTHORIZATION_STARTED, false)
    }
  }

  private fun handleBrowserNotFound() {
    Log.d(TAG, "Authorization flow canceled due to missing browser")
    IoLoginUtilsModule.rejectAndClearAuthorizationPromise(
      "NativeAuthSessionError",
      IoLoginError.Type.BROWSER_NOT_FOUND
    )
  }

  private fun handleNullPointerException() {
    Log.d(TAG, "handleNullPointerException")
    IoLoginUtilsModule.rejectAndClearAuthorizationPromise(
      "NativeAuthSessionError",
      IoLoginError.Type.ANDROID_SYSTEM_FAILURE
    )
  }

  companion object {
    private val TAG = AuthorizationManagerActivity::class.java.simpleName

    const val KEY_AUTH_INTENT = "authIntent"
    const val KEY_AUTHORIZATION_STARTED = "authStarted"

    fun createStartIntent(context: Context, authIntent: Intent): Intent =
      Intent(context, AuthorizationManagerActivity::class.java).apply {
        putExtra(KEY_AUTH_INTENT, authIntent)
      }

    fun createResponseHandlingIntent(context: Context, responseUri: Uri): Intent =
      Intent(context, AuthorizationManagerActivity::class.java).apply {
        data = responseUri
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
      }
  }
}
