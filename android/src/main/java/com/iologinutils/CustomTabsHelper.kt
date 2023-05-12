package com.iologinutils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
    val defaultViewHandlerInfo = pm.resolveActivity(activityIntent, 0)
    var defaultViewHandlerPackageName: String? = null
    if (defaultViewHandlerInfo != null) {
      defaultViewHandlerPackageName = defaultViewHandlerInfo.activityInfo.packageName
    }
    val resolvedActivityList = pm.queryIntentActivities(activityIntent, 0)

    val packagesSupportingCustomTabs: MutableList<String?> = ArrayList()
    for (info in resolvedActivityList) {
      val serviceIntent = Intent()
      serviceIntent.action = ACTION_CUSTOM_TABS_CONNECTION
      serviceIntent.setPackage(info.activityInfo.packageName)
      pm.resolveService(serviceIntent, 0)?.let {
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
      val handlers = pm.queryIntentActivities(
          intent,
          PackageManager.GET_RESOLVED_FILTER
        )
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
