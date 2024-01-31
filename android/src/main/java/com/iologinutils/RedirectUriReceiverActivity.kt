package com.iologinutils

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

/**
 * Activity that receives the redirect Uri sent by the PagoPA or IDP endpoint. It forwards the data
 * received as part of this redirect to {@link AuthorizationManagerActivity}, which
 * destroys the browser tab before returning the result to the starting Activity via Promise
 * provided to {@link AuthorizationService#performAuthorizationRequest}.
 */
class RedirectUriReceiverActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    intent.data?.let { data ->
      // Handling the redirect in this way ensures that we can remove the browser tab from the
      // back stack.

      Log.d("RedirectUriReceiver", "Handling received data ($data}")
      val responseHandlingIntent = AuthorizationManagerActivity.createResponseHandlingIntent(
        this, data
      )
      startActivity(responseHandlingIntent);
    }

    finish();
  }
}
