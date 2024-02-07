/*
 * This code is derived from the AppAuth-Android library, licensed under the Apache License, Version 2.0.
 * For reference please check https://github.com/openid/AppAuth-Android
 * Copyright 2016 The AppAuth for Android Authors. All Rights Reserved.
 * The Apache License, Version 2.0, is available at https://www.apache.org/licenses/LICENSE-2.0

 * Our original work is covered by the MIT license, as indicated in the project's root directory.
 */

package com.iologinutils.browser

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import androidx.annotation.VisibleForTesting

/**
 * Utility class to obtain the browser package name to be used for
 * [com.iologinutils.AuthorizationService.performAuthorizationRequest] calls. It prioritizes browsers which support
 * [custom tabs](https://developer.chrome.com/multidevice/android/customtabs). To
 * mitigate man-in-the-middle attacks by malicious apps pretending to be browsers for the
 * specific URI we query, only those which are registered as a handler for *all* HTTP and
 * HTTPS URIs will be used.
 */
internal class BrowserPackageHelper private constructor() {
  private var mPackageNameToUse: String? = null

  /**
   * Searches through all apps that handle VIEW intents and have a warmup service. Picks
   * the one chosen by the user if this choice has been made, otherwise any browser with a warmup
   * service is returned. If no browser has a warmup service, the default browser will be
   * returned. If no default browser has been chosen, an arbitrary browser package is returned.
   *
   *
   * This is **not** threadsafe.
   *
   * @param context [Context] to use for accessing [PackageManager].
   * @return The package name recommended to use for connecting to custom tabs related components.
   */
  fun getPackageNameToUse(context: Context): String {
    mPackageNameToUse?.let {
      return it
    }

    val pm = context.packageManager

    // retrieve a list of all the matching handlers for the browser intent.
    // queryIntentActivities will ensure that these are priority ordered, with the default
    // (if set) as the first entry. Ignoring any matches which are not "full" browsers,
    // pick the first that supports custom tabs, or the first full browser otherwise.
    var firstMatch: ResolveInfo? = null
    val resolvedActivityList =
      if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        pm.queryIntentActivities(
          BROWSER_INTENT,
          PackageManager.ResolveInfoFlags.of(PackageManager.GET_RESOLVED_FILTER.toLong())
        )
      } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(BROWSER_INTENT, PackageManager.GET_RESOLVED_FILTER)
      }
    for (info in resolvedActivityList) {
      // ignore handlers which are not browsers
      if (!isFullBrowser(info)) {
        continue
      }

      // we hold the first non-default browser as the default browser to use, if we do
      // not find any that support a warmup service.
      if (firstMatch == null) {
        firstMatch = info
      }
      if (hasWarmupService(pm, info.activityInfo.packageName)) {
        // we have found a browser with a warmup service, return it
        mPackageNameToUse = info.activityInfo.packageName
        return mPackageNameToUse!!
      }
    }

    // No handlers have a warmup service, so we return the first match (typically the
    // default browser), or null if there are no identifiable browsers.
    mPackageNameToUse = firstMatch?.activityInfo?.packageName

    firstMatch?.activityInfo?.packageName?.let {
      mPackageNameToUse = it
      return it
    }

    return mPackageNameToUse!!
  }

  private fun hasWarmupService(pm: PackageManager, packageName: String): Boolean {
    val serviceIntent = Intent()
    serviceIntent.setAction(ACTION_CUSTOM_TABS_CONNECTION)
    serviceIntent.setPackage(packageName)
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
      pm.resolveService(serviceIntent, PackageManager.ResolveInfoFlags.of(0)) != null
    } else {
      @Suppress("DEPRECATION")
      pm.resolveService(serviceIntent, 0) != null
    }
  }

  private fun isFullBrowser(resolveInfo: ResolveInfo): Boolean {
    // The filter must match ACTION_VIEW, CATEGORY_BROWSEABLE, and at least one scheme,
    if (
      !resolveInfo.filter.hasAction(Intent.ACTION_VIEW)
      || !resolveInfo.filter.hasCategory(Intent.CATEGORY_BROWSABLE)
      || resolveInfo.filter.schemesIterator() == null) {
      return false
    }

    // The filter must not be restricted to any particular set of authorities
    if (resolveInfo.filter.authoritiesIterator() != null) {
      return false
    }

    // The filter must support both HTTP and HTTPS.
    var supportsHttp = false
    var supportsHttps = false
    val schemeIterator = resolveInfo.filter.schemesIterator()
    while (schemeIterator.hasNext()) {
      val scheme = schemeIterator.next()
      supportsHttp = supportsHttp or (SCHEME_HTTP == scheme)
      supportsHttps = supportsHttps or (SCHEME_HTTPS == scheme)
      if (supportsHttp && supportsHttps) {
        return true
      }
    }

    // at least one of HTTP or HTTPS is not supported
    return false
  }

  companion object {
    private const val SCHEME_HTTP = "http"
    private const val SCHEME_HTTPS = "https"

    /**
     * The service we expect to find on a web browser that indicates it supports custom tabs.
     */
    @VisibleForTesting
    val ACTION_CUSTOM_TABS_CONNECTION = "android.support.customtabs.action.CustomTabsService"

    /**
     * An arbitrary (but unregistrable, per
     * [IANA rules](https://www.iana.org/domains/reserved)) web intent used to query
     * for installed web browsers on the system.
     */
    @VisibleForTesting
    val BROWSER_INTENT = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.example.com"))
    private var sInstance: BrowserPackageHelper? = null

    @get:Synchronized
    val instance: BrowserPackageHelper
      get() {
        if (sInstance == null) {
          sInstance = BrowserPackageHelper()
        }
        return sInstance!!
      }

    @VisibleForTesting
    @Synchronized
    fun clearInstance() {
      sInstance = null
    }
  }
}
