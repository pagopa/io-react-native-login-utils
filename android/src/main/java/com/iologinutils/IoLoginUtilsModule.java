package com.iologinutils;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.module.annotations.ReactModule;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

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
  public static ArrayList<String> getRedirects(String url, ReadableMap headers) throws IOException {
        ArrayList<String> urlArray = new ArrayList<String>();
        findRedirects(url,headers,urlArray);
        return urlArray;
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
}
