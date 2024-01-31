package com.iologinutils

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.iologinutils.IoLoginError.Companion.generateErrorObject

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

  private var mAuthorizationStarted = false
  private var mAuthIntent: Intent? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    if (savedInstanceState == null) {
      extractState(intent.extras);
    } else {
      extractState(savedInstanceState);
    }
  }

  override fun onNewIntent(intent: Intent?) {
    super.onNewIntent(intent)
    setIntent(intent)
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putBoolean(KEY_AUTHORIZATION_STARTED, mAuthorizationStarted)
    outState.putParcelable(KEY_AUTH_INTENT, mAuthIntent)
  }

  override fun onResume() {
    super.onResume()

    if (!mAuthorizationStarted) {
      try {
        startActivity(mAuthIntent);
        mAuthorizationStarted = true;
      } catch (e: ActivityNotFoundException) {
        handleBrowserNotFound()
        finish()
      }
      return
    }

    intent.data?.let { responseUri ->
      handleAuthorizationComplete(responseUri)
    } ?: run {
      handleAuthorizationCanceled();
    }

    finish();
  }

  private fun handleAuthorizationComplete(responseUri: Uri) {
    Log.d(
      "AuthManagerActivity",
      "Authorization complete - invoking completion intent with $responseUri"
    )
    IoLoginUtilsModule.authorizationPromise?.resolve(responseUri.toString())
  }

  private fun handleAuthorizationCanceled() {
    Log.d("AuthManagerActivity", "Authorization flow canceled by user")

    IoLoginUtilsModule.authorizationPromise?.reject(
      "NativeAuthSessionError",
      generateErrorObject(IoLoginError.Type.NATIVE_AUTH_SESSION_CLOSED)
    )
  }

  private fun extractState(state: Bundle?) {
    state?.apply {
      mAuthIntent = getParcelable(KEY_AUTH_INTENT)
      mAuthorizationStarted = getBoolean(KEY_AUTHORIZATION_STARTED, false)
    } ?: run {
      throw IllegalStateException("No state to extract");
    }
  }

  private fun handleBrowserNotFound() {
    Log.d("AuthManagerActivity", "Authorization flow canceled due to missing browser")
    IoLoginUtilsModule.authorizationPromise?.reject(
      "NativeAuthSessionError",
      generateErrorObject(IoLoginError.Type.BROWSER_NOT_FOUND)
    )
  }

  companion object {

    const val KEY_AUTH_INTENT = "authIntent"
    const val KEY_AUTHORIZATION_STARTED = "authStarted"

    private fun createBaseIntent(ioContext: Context): Intent {
      return Intent(ioContext, AuthorizationManagerActivity::class.java)
    }

    fun createStartIntent(
      ioContext: Context,
      authIntent: Intent,
    ): Intent {
      val intent: Intent = createBaseIntent(ioContext)
      intent.putExtra(KEY_AUTH_INTENT, authIntent)
      return intent
    }

    fun createResponseHandlingIntent(context: Context, responseUri: Uri): Intent {
      val intent = createBaseIntent(context)
      intent.setData(responseUri)
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
      intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
      return intent
    }
  }
}
