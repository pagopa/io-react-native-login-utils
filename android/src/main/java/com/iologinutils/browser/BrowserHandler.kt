/*
 * This code is derived from the AppAuth-Android library, licensed under the Apache License, Version 2.0.
 * For reference please check https://github.com/openid/AppAuth-Android
 * Copyright 2016 The AppAuth for Android Authors. All Rights Reserved.
 * The Apache License, Version 2.0, is available at https://www.apache.org/licenses/LICENSE-2.0

 * Our original work is covered by the MIT license, as indicated in the project's root directory.
 */

package com.iologinutils.browser

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference


/**
 * Hides the details of establishing connections and sessions with custom tabs, to make testing
 * easier.
 */
class BrowserHandler(private val context: Context) {

  private val browserPackage: String = BrowserPackageHelper.instance.getPackageNameToUse(this.context)
  private val connection: CustomTabsServiceConnection?
  private val client: AtomicReference<CustomTabsClient?> = AtomicReference()
  private val clientLatch: CountDownLatch = CountDownLatch(1)

  init {
    connection = bindCustomTabsService()
  }

  private fun bindCustomTabsService(): CustomTabsServiceConnection? {
    val connection: CustomTabsServiceConnection = object : CustomTabsServiceConnection() {
      override fun onServiceDisconnected(componentName: ComponentName) {
        Log.d(TAG, "CustomTabsService is disconnected")
        setClient(null)
      }

      override fun onCustomTabsServiceConnected(
        componentName: ComponentName,
        customTabsClient: CustomTabsClient
      ) {
        Log.d(TAG, "CustomTabsService is connected")
        customTabsClient.warmup(0)
        setClient(customTabsClient)
      }

      private fun setClient(client: CustomTabsClient?) {
        this@BrowserHandler.client.set(client)
        clientLatch.countDown()
      }
    }
    if (!CustomTabsClient.bindCustomTabsService(
        context,
        browserPackage,
        connection
      )
    ) {
      // this is expected if the browser does not support custom tabs
      Log.i(TAG, "Unable to bind custom tabs service")
      clientLatch.countDown()
      return null
    }
    return connection
  }

  fun createCustomTabsIntentBuilder(): CustomTabsIntent.Builder {
    return CustomTabsIntent.Builder(createSession())
  }

  fun unbind() {
    connection?.let {
      context.unbindService(it)
      Log.i(TAG, "CustomTabsService is disconnected")
    } ?: Log.i(TAG, "Connection was null")
    client.set(null)
  }

  private fun createSession(): CustomTabsSession? {
    try {
      clientLatch.await(CLIENT_WAIT_TIME, TimeUnit.SECONDS)
    } catch (e: InterruptedException) {
      Log.i(TAG, "Interrupted while waiting for browser connection")
      clientLatch.countDown()
    }
    val client = client.get()
    return client?.newSession(null)
  }

  fun getBrowserPackage(): String {
    return browserPackage
  }

  companion object {
    /**
     * Wait for at most this amount of time for the browser connection to be established.
     */
    private const val CLIENT_WAIT_TIME = 1L

    private val TAG = BrowserHandler::class.java.simpleName
  }
}
