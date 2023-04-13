package com.iologinutils;

import android.app.Activity;
import android.content.Intent;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.Promise;

public class CustomTabActivity extends Activity {

  public static Promise promise;
  public static CustomTabActivityHelper customTabHelper;
  public static Activity customTabContext;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    handleExtra(getIntent());
  }

  // If your app is in opened in background and you attempt to open an url, it
  // will call this function instead of onCreate()
  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    setIntent(intent);
    handleExtra(getIntent());
  }

  private void handleExtra(Intent intent) {

    try {
      customTabHelper.unbindCustomTabsService(customTabContext);
      Uri uri = intent.getData();
      if (uri != null) {
        promise.resolve(uri.toString());
      }
    } catch (Exception e) {
      promise.reject("error", "Error in resolving promise");
    }
  }
}
