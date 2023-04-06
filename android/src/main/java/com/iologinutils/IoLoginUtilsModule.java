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

import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsServiceConnection;
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

    private CustomTabsClient customTabsClient;
    private CustomTabsSession customTabsSession;
    private CustomTabsServiceConnection customTabsServiceConnection;


    @ReactMethod
    public void openAuthenticationSession(String url, String callbackURLScheme, Promise promise) {
        Activity activity = getCurrentActivity();
        if (activity == null) {
            promise.reject("error", "Activity is null");
            return;
        }

        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        CustomTabsIntent customTabsIntent = builder.build();
        CustomTabsCallback customTabsCallback = new CustomTabsCallback() {
            @Override
            public void onNavigationEvent(int navigationEvent, Uri uri, Bundle extras) {
                if (navigationEvent == CustomTabsCallback.NAVIGATION_FINISHED) {
                    if (uri.toString().startsWith(callbackURLScheme)) {
                        customTabsIntent.intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.startActivity(customTabsIntent.intent);
                        promise.resolve(uri.toString());
                    }
                }
            }
        };

        customTabsServiceConnection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
                customTabsClient = client;
                customTabsSession = customTabsClient.newSession(customTabsCallback);
                customTabsSession.mayLaunchUrl(Uri.parse(url), null, null);
                customTabsIntent.intent.setPackage(customTabsClient.getPackageName());
                customTabsIntent.launchUrl(activity, Uri.parse(url));
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                customTabsClient = null;
                customTabsSession = null;
            }
        };

        String packageName = CustomTabsHelper.getPackageNameToUse(activity);
        if (packageName != null) {
            CustomTabsClient.bindCustomTabsService(activity, packageName, customTabsServiceConnection);
        } else {
            promise.reject("error", "No compatible browser found");
        }
    }
}
