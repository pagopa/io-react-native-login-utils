package com.iologinutils;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.module.annotations.ReactModule;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;


import android.app.PendingIntent;
import android.content.Context;


import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsSession;

@ReactModule(name = IoLoginUtilsModule.NAME)
public class IoLoginUtilsModule extends ReactContextBaseJavaModule {
  public static final String NAME = "IoLoginUtils";

  public IoLoginUtilsModule(ReactApplicationContext reactContext) {
    super(reactContext);
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }


  @ReactMethod
  public static void getRedirects(String url, ReadableMap headers, Promise promise) throws IOException {
        ArrayList<String> urlArray = new ArrayList<String>();
        try {
            findRedirects(url, headers, urlArray);
            String[] urls = urlArray.toArray(new String[0]);
            WritableArray resultArray = Arguments.fromArray(urls);
            promise.resolve(resultArray);
          }
        catch (IOException e) {
          promise.reject("error", e.getMessage());
        }
    }

    public static void findRedirects(String url, ReadableMap headers, ArrayList<String> urlArray) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setInstanceFollowRedirects(false);
        HashMap<String, Object> headersMap = headers.toHashMap();
        for (String key : headersMap.keySet()) {
            String value = headersMap.get(key).toString();
            connection.setRequestProperty(key, value);
        }
        int responseCode = connection.getResponseCode();
        if (responseCode >= 300 && responseCode <= 399) {
            String redirectUrl = connection.getHeaderField("Location");
            if(redirectUrl.startsWith("/")){
                URL previousUrl = new URL(url);
                String redirectScheme = previousUrl.getProtocol();
                String redirectHost = previousUrl.getHost();
                redirectUrl = redirectScheme + "://" + redirectHost + redirectUrl;
            }
            urlArray.add(redirectUrl);
            findRedirects(redirectUrl,urlArray);
        } else {
            return;
        }
    }

    public static void findRedirects(String url, ArrayList<String> urlArray) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setInstanceFollowRedirects(false);
        int responseCode = connection.getResponseCode();
        if (responseCode >= 300 && responseCode <= 399) {
            String redirectUrl = connection.getHeaderField("Location");
            if (redirectUrl.startsWith("/")) {
                URL previousUrl = new URL(url);
                String redirectScheme = previousUrl.getProtocol();
                String redirectHost = previousUrl.getHost();
                redirectUrl = redirectScheme + "://" + redirectHost + redirectUrl;
            }
            urlArray.add(redirectUrl);
            findRedirects(redirectUrl, urlArray);
        } else {
            return;
        }
    }


    @ReactMethod
    public void openAuthenticationSession(String url, String callbackURLScheme, Promise promise) {
      Context context = super.getReactApplicationContext();
      openCustomTab(Uri.parse(url),callbackURLScheme, promise, context);
    }

  public void openCustomTab(Uri uri, String callbackURLScheme ,Promise promise, Context context) {
    final AtomicReference<CustomTabsClient> mClient = new AtomicReference<>();

    final String PACKAGE_NAME = "com.android.chrome";
      try{
        final Activity activity = getCurrentActivity();

        Intent sendLinkIntent = new Intent(context,activity.getClass());
        sendLinkIntent.putExtra(Intent.EXTRA_SUBJECT,"This is the link you were exploring");
        PendingIntent pendingIntent = PendingIntent.getActivity(context,0,sendLinkIntent,PendingIntent.FLAG_UPDATE_CURRENT);

        CustomTabsIntent.Builder customIntentBuilder = new CustomTabsIntent.Builder();
        CustomTabsIntent customTabsIntent = customIntentBuilder.build();



        customTabsIntent.intent.setPackage(PACKAGE_NAME);
        CustomTabsCallback callback = new CustomTabsCallback() {
          @Override
          public void onNavigationEvent(int navigationEvent, Bundle extras) {
            super.onNavigationEvent(navigationEvent, extras);
            if (navigationEvent == NAVIGATION_FINISHED) {
              // Check if the URL contains the callbackURLScheme
              try {
                pendingIntent.send();
              } catch (PendingIntent.CanceledException e) {
                promise.reject("error", e.getMessage());
              }
              Uri url = sendLinkIntent.getData();
              if (url.toString().startsWith(callbackURLScheme)) {
                // Close the custom tab and resolve the promise with the URL
                customTabsIntent.intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(customTabsIntent.intent);
                promise.resolve(url.toString());
              }
            }
          }
        };
        CustomTabsSession customSession = mClient.get().newSession(callback);
        customIntentBuilder.setSession(customSession);
        customTabsIntent.launchUrl(activity, uri);


      } catch(Exception e){
        promise.reject("error", e.getMessage());
      }

  }


}
