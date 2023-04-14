package com.iologinutils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.transition.Slide;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.app.SharedElementCallback;

import com.facebook.react.bridge.Promise;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class CustomTabActivity extends Activity {

  public static Promise promise;
  public static CustomTabActivityHelper customTabHelper;
  public static Activity customTabContext;
  public static int navigationEvent;
  public static Activity context;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    context = this;
    super.onCreate(savedInstanceState);
    handleExtra(getIntent());
  }

  // If your app is in opened in background and you attempt to open an url, it
  // will call this function instead of onCreate()
  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    context = this;
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
    } finally {
      View v = new View(customTabContext);
      setContentView(v);

      v.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
        @Override
        public void onViewAttachedToWindow(View view) {
          if(navigationEvent == 4 || navigationEvent == 2 || navigationEvent == 6){
            context.finish();
          }
        }
        @Override
        public void onViewDetachedFromWindow(View view) {
        }
      });
    }
  }
}
