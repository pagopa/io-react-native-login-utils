import * as React from 'react';

import {
  LoginUtilsError,
  getRedirects,
  openAuthenticationSession,
} from '@pagopa/io-react-native-login-utils';
import { Button, SafeAreaView, StyleSheet, Text, View } from 'react-native';

export default function App() {
  const [authResult, setAuthResult] = React.useState<string | undefined>();
  const [redirectResult, setRedirectResult] = React.useState<
    string[] | undefined
  >();

  React.useEffect(() => {
    console.log('First render!');
  }, []);

  React.useEffect(() => {
    getRedirects('https://tinyurl.com/testG0', {}, '')
      .then(setRedirectResult)
      .catch((err: LoginUtilsError) => {
        console.log(err);
      });
  }, []);

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.container}>
        <>
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
              .catch(() => {
                setAuthResult(undefined);
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
