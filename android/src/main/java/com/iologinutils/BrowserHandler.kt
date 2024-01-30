/*
 * Copyright 2015 The AppAuth for Android Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iologinutils

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
class BrowserHandler(context: Context) {

  private val mContext: Context = context
  private val mBrowserPackage: String
  private val mConnection: CustomTabsServiceConnection?
  private val mClient: AtomicReference<CustomTabsClient?>
  private val mClientLatch: CountDownLatch

  init {
    mBrowserPackage = BrowserPackageHelper.instance.getPackageNameToUse(mContext)
    mClient = AtomicReference()
    mClientLatch = CountDownLatch(1)
    mConnection = bindCustomTabsService()
  }

  private fun bindCustomTabsService(): CustomTabsServiceConnection? {
    val connection: CustomTabsServiceConnection = object : CustomTabsServiceConnection() {
      override fun onServiceDisconnected(componentName: ComponentName) {
        Log.d("BrowserHandler", "CustomTabsService is disconnected")
        setClient(null)
      }

      override fun onCustomTabsServiceConnected(
        componentName: ComponentName,
        customTabsClient: CustomTabsClient
      ) {
        Log.d("BrowserHandler","CustomTabsService is connected")
        customTabsClient.warmup(0)
        setClient(customTabsClient)
      }

      private fun setClient(client: CustomTabsClient?) {
        mClient.set(client)
        mClientLatch.countDown()
      }
    }
    if (!CustomTabsClient.bindCustomTabsService(
        mContext,
        mBrowserPackage,
        connection
      )
    ) {
      // this is expected if the browser does not support custom tabs
      Log.i("BrowserHandler","Unable to bind custom tabs service")
      mClientLatch.countDown()
      return null
    }
    return connection
  }

  fun createCustomTabsIntentBuilder(): CustomTabsIntent.Builder {
    return CustomTabsIntent.Builder(createSession())
  }

  fun unbind() {
    if (mConnection == null) {
      return
    }
    mContext.unbindService(mConnection)
    mClient.set(null)
    Log.i("BrowserHandler","CustomTabsService is disconnected")
  }

  private fun createSession(): CustomTabsSession? {
    try {
      mClientLatch.await(CLIENT_WAIT_TIME, TimeUnit.SECONDS)
    } catch (e: InterruptedException) {
      Log.i("BrowserHandler","Interrupted while waiting for browser connection")
      mClientLatch.countDown()
    }
    val client = mClient.get()
    return client?.newSession(null)
  }

  fun getBrowserPackage(): String {
    return mBrowserPackage
  }

  companion object {
    /**
     * Wait for at most this amount of time for the browser connection to be established.
     */
    private const val CLIENT_WAIT_TIME = 1L

  }
}
