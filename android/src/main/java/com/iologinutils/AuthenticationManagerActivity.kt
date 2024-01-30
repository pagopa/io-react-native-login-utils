package com.iologinutils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log


class AuthenticationManagerActivity : Activity() {

  private var mAuthenticationStarted = false
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
    outState.putBoolean(KEY_AUTHENTICATION_STARTED, mAuthenticationStarted)
    outState.putParcelable(KEY_AUTH_INTENT, mAuthIntent)
  }

  override fun onResume() {
    super.onResume()

    if (!mAuthenticationStarted) {
      startActivity(mAuthIntent);
      mAuthenticationStarted = true;
      return;
    }

    intent.data?.let { responseUri ->
      handleAuthenticationComplete(responseUri)
    } ?: run {
      handleAuthenticationCanceled();
    }

    finish();
  }

  private fun handleAuthenticationComplete(responseUri: Uri) {

    // val responseData: Intent = extractResponseData(responseUri)
    Log.d("AuthManagerActivity", "Authentication complete - invoking completion intent with $responseUri")

    /*
    try {
      mCompleteIntent.send(this, 0, responseData)
    } catch (ex: CanceledException) {
      Log.e("AuthManagerActivity","Failed to send completion intent", ex)
    }
     */
  }

  private fun handleAuthenticationCanceled() {
    Log.d("AuthManagerActivity", "Authentication flow canceled by user")/*
    if (mCancelIntent != null) {
      try {
        mCancelIntent.send()
      } catch (ex: CanceledException) {
        Log.e("AuthManagerActivity","Failed to send cancel intent", ex)
      }
    } else {
      Log.d("AuthManagerActivity","No cancel intent set - will return to previous activity")
    }
    */
  }

  private fun extractState(state: Bundle?) {
    state?.let {
      mAuthIntent = it.getParcelable(KEY_AUTH_INTENT)
      mAuthenticationStarted = it.getBoolean(KEY_AUTHENTICATION_STARTED, false)
    } ?: run {
      throw IllegalStateException("No state to extract");
    }
  }

  /*
  private fun extractResponseData(responseUri: Uri): Intent {
    return if (responseUri.queryParameterNames.contains(AuthorizationException.PARAM_ERROR)) {
      AuthorizationException.fromOAuthRedirect(responseUri).toIntent()
    } else {
      val response: AuthorizationResponse =
        Builder(mAuthRequest).fromUri(responseUri, mClock).build()
      response.toIntent()
    }
  }
  */

  companion object {

    const val KEY_AUTH_INTENT = "authIntent"
    const val KEY_AUTHENTICATION_STARTED = "authStarted"

    private fun createBaseIntent(ioContext: Context): Intent {
      return Intent(ioContext, AuthenticationManagerActivity::class.java)
    }

    fun createStartIntent(
      ioContext: Context,
      authIntent: Intent?,
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
