import * as React from 'react';
import {
  LoginUtilsError,
  getRedirects,
  isLoginUtilsError,
  openAuthenticationSession,
  supportsInAppBrowser,
} from '@pagopa/io-react-native-login-utils';
import {
  Button,
  SafeAreaView,
  StyleSheet,
  Text,
  View,
  useColorScheme,
} from 'react-native';
import CookieManager from '@react-native-cookies/cookies';
import { WebView } from 'react-native-webview';

const REDIRECT_URL_HOST = 'https://www.versionestabile.it';
const REDIRECT_URL_PATH = '/pagopa/public/redirect-and-cookie';
const REDIRECT_URL = `${REDIRECT_URL_HOST}${REDIRECT_URL_PATH}`;

export default function App() {
  const colorScheme = useColorScheme();
  const isDarkMode = colorScheme === 'dark';
  const textColor = { color: isDarkMode ? '#FFFFFF' : '#000000' };

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

  return redirectResult ? (
    <View style={styles.webviewContainer}>
      <WebView
        source={{ uri: redirectResult[redirectResult.length - 1] ?? '' }}
        style={styles.webview}
      />
      <View style={styles.contentWrapper}>
        {redirectResult?.map((url, index) => (
          <Text style={textColor} key={index}>
            Result: {`${index}: ${url}`}
          </Text>
        ))}
      </View>
      <View style={styles.contentWrapper}>
        <Button
          title="Back"
          onPress={() => {
            setRedirectResult(undefined);
          }}
        />
      </View>
    </View>
  ) : (
    <SafeAreaView style={styles.container}>
      <View style={styles.container}>
        <>
          <Text style={textColor}>
            {inAppBrowserSupported !== undefined
              ? `${
                  inAppBrowserSupported
                    ? 'InApp Browser Supported'
                    : 'InApp Browser Unsupported'
                }`
              : 'InApp Browser support Unknown yet'}
          </Text>
          {authResult && <Text style={textColor}>{authResult}</Text>}
        </>
      </View>
      <View style={styles.button}>
        <Button
          title="Test Redirects"
          onPress={() => {
            CookieManager.clearAll(true).then(() => {
              getRedirects(REDIRECT_URL, {}, 'testcookie')
                .then((values: string[]) => {
                  console.log('Redirects:', values);
                  CookieManager.get(REDIRECT_URL_HOST, true).then((cookies) => {
                    console.log(
                      `Cookies for ${REDIRECT_URL_HOST}:\n`,
                      JSON.stringify(cookies, null, 2)
                    );
                  });
                  values?.map((url, index) =>
                    console.log(`Redirect ${index + 1}: ${url}`)
                  );
                  setRedirectResult(values);
                })
                .catch((err: LoginUtilsError) => {
                  console.log(`${err.code} ${err.userInfo?.error}`);
                });
            });
          }}
        />
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
  contentWrapper: {
    marginVertical: 8,
    marginHorizontal: 24,
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
  webviewContainer: {
    flex: 1,
  },
  webview: {
    flex: 1,
    marginTop: 30,
  },
});
