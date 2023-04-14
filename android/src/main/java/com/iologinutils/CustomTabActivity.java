package com.iologinutils;

import android.app.Activity;
import android.content.Intent;

import android.net.Uri;
import android.os.Bundle;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;

import com.facebook.react.bridge.Promise;


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
    overridePendingTransition(0, 0); // Disable animation
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


      ProgressBar progressBar = new ProgressBar(customTabContext, null, android.R.attr.progressBarStyleLarge);
      progressBar.setIndeterminate(true);
      FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
      layoutParams.gravity = Gravity.CENTER;
      FrameLayout frameLayout = new FrameLayout(customTabContext);
      frameLayout.addView(progressBar, layoutParams);
      setContentView(frameLayout);


      overridePendingTransition(0, 0);
      frameLayout.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
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
