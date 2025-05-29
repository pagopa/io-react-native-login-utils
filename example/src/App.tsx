import * as React from 'react';

import {
  LoginUtilsError,
  getRedirects,
  isLoginUtilsError,
  openAuthenticationSession,
  supportsInAppBrowser,
} from '@pagopa/io-react-native-login-utils';
import { Button, SafeAreaView, StyleSheet, Text, View } from 'react-native';
import CookieManager from '@react-native-cookies/cookies';

export default function App() {
  const [authResult, setAuthResult] = React.useState<string | undefined>();
  const [redirectResult, setRedirectResult] = React.useState<
    string[] | undefined
  >();
  const [inAppBrowserSupported, setInAppBrowserSupported] = React.useState<
    boolean | undefined
  >(undefined);

  React.useEffect(() => {
    console.log('First render!');
  }, []);

  React.useEffect(() => {
    getRedirects(
      'https://www.versionestabile.it/pagopa/public/redirect-and-cookie',
      {},
      ''
    )
      .then((values: string[]) => {
        console.log('Redirects:', values);
        CookieManager.get('https://www.versionestabile.it').then((cookies) => {
          console.log('Cookies:', cookies);
        });
        setRedirectResult(values);
      })
      .catch((err: LoginUtilsError) => {
        console.log(`${err.code} ${err.userInfo?.error}`);
      });
  }, []);

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.container}>
        <>
          <Text>
            {inAppBrowserSupported !== undefined
              ? `${
                  inAppBrowserSupported
                    ? 'InApp Browser Supported'
                    : 'InApp Browser Unsupported'
                }`
              : 'InApp Browser support Unknown yet'}
          </Text>
          {redirectResult?.map((url, index) => (
            <Text key={index}>Result: {`${index}: ${url}`}</Text>
          ))}
          {authResult && <Text>{authResult}</Text>}
        </>
      </View>
      <View style={styles.button}>
        <Button
          title="Test Login"
          onPress={() => {
            // Be sure to have the Native-Login flag set on the dev server
            // App IO send "x-pagopa-app-version" with the app version to enable the native login
            openAuthenticationSession(
              'http://127.0.0.1:3000/login?authLevel-SpidL2&entityID-posteid',
              'iologin'
            )
              .then((data) => {
                setAuthResult(data);
              })
              .catch((e: unknown) => {
                if (isLoginUtilsError(e)) {
                  console.log(`${e.code} ${e.userInfo?.error}`);
                  setAuthResult(undefined);
                }
              });
          }}
        />
      </View>
      <View style={styles.button}>
        <Button
          title="Test Payment"
          onPress={() => {
            openAuthenticationSession(
              'http://127.0.0.1:3000/payment-wallet?transactionId=01HNAS7T0D6XXEHK40XJN7MJRB',
              'iowallet'
            )
              .then((data) => {
                setAuthResult(data);
              })
              .catch(() => {
                setAuthResult(undefined);
              });
          }}
        />
      </View>
      <View style={styles.button}>
        <Button
          title="Test Not Ephemeral External Site"
          onPress={() => {
            openAuthenticationSession('https://www.google.com', '', true)
              .then((data) => {
                setAuthResult(data);
              })
              .catch(() => {
                setAuthResult(undefined);
              });
          }}
        />
      </View>
      <View style={styles.button}>
        <Button
          title="Supports InApp Browser"
          onPress={() => {
            supportsInAppBrowser().then((supported) =>
              setInAppBrowserSupported(supported)
            );
          }}
        />
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    marginHorizontal: 24,
    marginVertical: 16,
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  button: {
    width: '100%',
    marginVertical: 8,
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
