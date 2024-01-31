package com.iologinutils

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity


class RedirectUriReceiverActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    intent.data?.let { data ->
      // Handling the redirect in this way ensures that we can remove the browser tab from the
      // back stack.

      Log.d("RedirectUriReceiver", "Handling received data ($data}")
      val responseHandlingIntent = AuthenticationManagerActivity.createResponseHandlingIntent(
        this, data
      )
      startActivity(responseHandlingIntent);
    }

    finish();
  }
}
