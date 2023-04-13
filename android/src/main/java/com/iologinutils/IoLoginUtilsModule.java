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

import android.app.Activity;

import android.net.Uri;

import androidx.browser.customtabs.CustomTabsIntent;


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
                String port = String.valueOf(previousUrl.getPort());
                redirectUrl = redirectScheme +"://" + redirectHost + (port.equals("-1") ? "" : ":" + port) + redirectUrl;
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
                String port = String.valueOf(previousUrl.getPort());
                redirectUrl = redirectScheme +"://" + redirectHost + (port.equals("-1") ? "" : ":" + port) + redirectUrl;
            }
            urlArray.add(redirectUrl);
            findRedirects(redirectUrl, urlArray);
        } else {
            return;
        }
    }


    @ReactMethod
    public void openAuthenticationSession(String url, String callbackURLScheme, Promise promise) {
      CustomTabActivity.promise = promise;
      prepareCustomTab(Uri.parse(url),promise);
    }

  public synchronized void prepareCustomTab(Uri uri, Promise promise) {

    Activity activity = getCurrentActivity();
      try{
        CustomTabsIntent.Builder intentBuilder = new CustomTabsIntent.Builder();
        CustomTabActivityHelper customTabHelper = new CustomTabActivityHelper();

        CustomTabActivity.customTabContext = activity;
        CustomTabActivity.customTabHelper = customTabHelper;

        customTabHelper.bindCustomTabsService(activity,uri);

        synchronized(CustomTabActivityHelper.lock) {
          while(CustomTabActivityHelper.mClient == null) {
            try {
              CustomTabActivityHelper.lock.wait();
            } catch (InterruptedException e) {
              promise.reject("error", e.getMessage());
            }
          }
        }
        customTabHelper.mayLaunchUrl(uri);
        CustomTabsIntent intent = intentBuilder.build();
        CustomTabActivityHelper.openCustomTab(activity,intent,uri,promise);
      } catch(Exception e){
        promise.reject("error", e.getMessage());
      }

  }


}

