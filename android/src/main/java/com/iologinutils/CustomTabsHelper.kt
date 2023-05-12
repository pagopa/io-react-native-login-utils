package com.iologinutils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.util.Log

object CustomTabsHelper {
  private const val TAG = "CustomTabsHelper"
  private const val ACTION_CUSTOM_TABS_CONNECTION =
    "android.support.customtabs.action.CustomTabsService"
  private var sPackageNameToUse: String? = null
  fun getPackageNameToUse(context: Context, uri: Uri?): String? {
    if (sPackageNameToUse != null) return sPackageNameToUse
    val pm = context.packageManager
    val activityIntent = Intent(Intent.ACTION_VIEW, uri)
    val defaultViewHandlerInfo = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      @Suppress("DEPRECATION")
      pm.resolveActivity(activityIntent, 0)
    } else {
      pm.resolveActivity(activityIntent, PackageManager.ResolveInfoFlags.of(0))
    }
    var defaultViewHandlerPackageName: String? = null
    if (defaultViewHandlerInfo != null) {
      defaultViewHandlerPackageName = defaultViewHandlerInfo.activityInfo.packageName
    }
    val resolvedActivityList = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
      @Suppress("DEPRECATION")
      pm.queryIntentActivities(activityIntent, 0)
    } else {
      pm.queryIntentActivities(activityIntent, PackageManager.ResolveInfoFlags.of(0))
    }
    val packagesSupportingCustomTabs: MutableList<String?> = ArrayList()
    for (info in resolvedActivityList) {
      val serviceIntent = Intent()
      serviceIntent.action = ACTION_CUSTOM_TABS_CONNECTION
      serviceIntent.setPackage(info.activityInfo.packageName)
      val service = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        @Suppress("DEPRECATION")
        pm.resolveService(serviceIntent, 0)
      } else {
        pm.resolveService(serviceIntent, PackageManager.ResolveInfoFlags.of(0))
      }
      if (service != null) {
        packagesSupportingCustomTabs.add(info.activityInfo.packageName)
      }
    }
    sPackageNameToUse = if (packagesSupportingCustomTabs.isEmpty()) {
      null
    } else if (!TextUtils.isEmpty(defaultViewHandlerPackageName)
      && !hasSpecializedHandlerIntents(context, activityIntent)
      && packagesSupportingCustomTabs.contains(defaultViewHandlerPackageName)
    ) {
      defaultViewHandlerPackageName
    } else {
      packagesSupportingCustomTabs[0]
    }
    return sPackageNameToUse
  }

  private fun hasSpecializedHandlerIntents(context: Context, intent: Intent): Boolean {
    try {
      val pm = context.packageManager
      val handlers = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(
          intent,
          PackageManager.GET_RESOLVED_FILTER
        )
      } else {
        pm.queryIntentActivities(
          intent,
          PackageManager.ResolveInfoFlags.of(PackageManager.GET_RESOLVED_FILTER.toLong())
        )
      }
      if (handlers.size == 0) {
        return false
      }
      for (resolveInfo in handlers) {
        val filter = resolveInfo.filter ?: continue
        if (filter.countDataAuthorities() == 0 || filter.countDataPaths() == 0) continue
        if (resolveInfo.activityInfo == null) continue
        return true
      }
    } catch (e: RuntimeException) {
      Log.e(TAG, "Runtime exception while getting specialized handlers")
    }
    return false
  }
}
