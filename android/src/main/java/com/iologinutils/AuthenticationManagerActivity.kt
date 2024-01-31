package com.iologinutils

import android.app.Activity
import android.content.ActivityNotFoundException
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
      try {
        startActivity(mAuthIntent);
        mAuthenticationStarted = true;
      } catch (e: ActivityNotFoundException) {
        handleBrowserNotFound()
        finish()
      }
      return
    }

    intent.data?.let { responseUri ->
      handleAuthenticationComplete(responseUri)
    } ?: run {
      handleAuthenticationCanceled();
    }

    finish();
  }

  private fun handleAuthenticationComplete(responseUri: Uri) {
    Log.d(
      "AuthManagerActivity",
      "Authentication complete - invoking completion intent with $responseUri"
    )

    IoLoginUtilsModule.promise?.resolve(responseUri.toString())
  }

  private fun handleAuthenticationCanceled() {
    Log.d("AuthManagerActivity", "Authentication flow canceled by user")

    IoLoginUtilsModule.promise?.reject("Test", "Test")
  }

  private fun extractState(state: Bundle?) {
    state?.let {
      mAuthIntent = it.getParcelable(KEY_AUTH_INTENT)
      mAuthenticationStarted = it.getBoolean(KEY_AUTHENTICATION_STARTED, false)
    } ?: run {
      throw IllegalStateException("No state to extract");
    }
  }

  private fun handleBrowserNotFound() {
    Log.d("AuthManagerActivity", "Authorization flow canceled due to missing browser")

  }

  companion object {

    const val KEY_AUTH_INTENT = "authIntent"
    const val KEY_AUTHENTICATION_STARTED = "authStarted"

    private fun createBaseIntent(ioContext: Context): Intent {
      return Intent(ioContext, AuthenticationManagerActivity::class.java)
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
