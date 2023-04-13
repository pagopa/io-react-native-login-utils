package com.iologinutils;

import android.app.Activity;
import android.content.ComponentName;
import android.net.Uri;
import android.util.Log;

import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;

import com.facebook.react.bridge.Promise;

public class CustomTabActivityHelper extends CustomTabsServiceConnection{
  private CustomTabsSession mCustomTabsSession;
  public static CustomTabsClient mClient;
  private CustomTabsServiceConnection mConnection;
  public static Object lock = new Object();


  public static void openCustomTab(Activity activity,
                                   CustomTabsIntent customTabsIntent,
                                   Uri uri,
                                   Promise promise) {
    String packageName = CustomTabsHelper.getPackageNameToUse(activity,uri);

    if (packageName == null) {
      promise.reject("error", "missing browser");
    } else {
      customTabsIntent.intent.setPackage(packageName);
      customTabsIntent.launchUrl(activity, uri);
    }
  }

  public void unbindCustomTabsService(Activity activity) {
    if (mConnection == null) return;
    activity.unbindService(mConnection);
    mClient = null;
    mCustomTabsSession = null;
    mConnection = null;
  }

  public CustomTabsSession getSession() {
    if (mClient == null) {
      mCustomTabsSession = null;
    } else if (mCustomTabsSession == null) {
      mCustomTabsSession = mClient.newSession(null);
    }
    return mCustomTabsSession;
  }

  public void bindCustomTabsService(Activity activity,Uri uri) {
    if (mClient != null) return;

    String packageName = CustomTabsHelper.getPackageNameToUse(activity,uri);
    if (packageName == null) return;

    mConnection = this;
    CustomTabsClient.bindCustomTabsService(activity, packageName, mConnection);
  }

  public boolean mayLaunchUrl(Uri uri) {
    if (mClient == null) return false;

    CustomTabsSession session = getSession();
    if (session == null) return false;

    return session.mayLaunchUrl(uri, null, null);
  }

  @Override
  public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
    synchronized(lock) {
      mClient = client;
      mClient.warmup(0L);
      lock.notifyAll();
    }
  }

  @Override
  public void onServiceDisconnected(ComponentName name) {
    mClient = null;
    mCustomTabsSession = null;
  }
}
