package com.iologinutils

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity


class RedirectUriReceiverActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    intent.data?.let { data ->
      // while this does not appear to be achieving much, handling the redirect in this way
      // ensures that we can remove the browser tab from the back stack. See the documentation
      // on AuthenticationManagementActivity for more details.
      startActivity(
        AuthenticationManagerActivity.createResponseHandlingIntent(
          this, data
        )
      );
    }

    finish();
  }
}
