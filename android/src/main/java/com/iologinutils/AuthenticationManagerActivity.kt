package com.iologinutils

import android.app.Activity
import android.app.PendingIntent
import android.app.PendingIntent.CanceledException
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log


class AuthenticationManagerActivity : Activity() {

  private var mAuthenticationStarted = false
  private var mAuthIntent: Intent? = null
  private var mIOIntent: PendingIntent? = null

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
    outState.putParcelable(KEY_IO_INTENT, mIOIntent)
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

    val responseData = Intent()
    responseData.putExtra(AuthenticationResponse.EXTRA_RESPONSE, responseUri)
    sendResult(mIOIntent, responseData, RESULT_OK)
  }

  private fun handleAuthenticationCanceled() {
    Log.d("AuthManagerActivity", "Authentication flow canceled by user")

    IoLoginUtilsModule.promise?.reject("Test", "Test")

    val cancelData = Intent()
    cancelData.putExtra(AuthenticationException.EXTRA_EXCEPTION, "User canceled")
    sendResult(mIOIntent, cancelData, RESULT_CANCELED)
  }

  private fun extractState(state: Bundle?) {
    state?.let {
      mAuthIntent = it.getParcelable(KEY_AUTH_INTENT)
      mIOIntent = it.getParcelable(KEY_IO_INTENT)
      mAuthenticationStarted = it.getBoolean(KEY_AUTHENTICATION_STARTED, false)
    } ?: run {
      throw IllegalStateException("No state to extract");
    }
  }

  private fun handleBrowserNotFound() {
    Log.d("AuthManagerActivity", "Authorization flow canceled due to missing browser")

    val cancelData = Intent()
    cancelData.putExtra(AuthenticationException.EXTRA_EXCEPTION, "User canceled")
    sendResult(mIOIntent, cancelData, RESULT_CANCELED)
  }

  private fun sendResult(callback: PendingIntent?, cancelData: Intent, resultCode: Int) {
    if (callback != null) {
      try {
        callback.send(this, 0, cancelData)
      } catch (e: CanceledException) {
        Log.e("AuthManagerActivity", "Failed to send cancel intent", e)
      }
    } else {
      setResult(resultCode, cancelData)
    }
  }

  companion object {

    const val KEY_AUTH_INTENT = "authIntent"
    const val KEY_IO_INTENT = "ioIntent"
    const val KEY_AUTHENTICATION_STARTED = "authStarted"

    private fun createBaseIntent(ioContext: Context): Intent {
      return Intent(ioContext, AuthenticationManagerActivity::class.java)
    }

    fun createStartIntent(
      ioContext: Context,
      authIntent: Intent,
      ioIntent: PendingIntent? = null
    ): Intent {
      val intent: Intent = createBaseIntent(ioContext)
      intent.putExtra(KEY_AUTH_INTENT, authIntent)
      intent.putExtra(KEY_IO_INTENT, ioIntent)
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
