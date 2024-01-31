import * as React from 'react';

import {
  LoginUtilsError,
  getRedirects,
  openAuthenticationSession,
} from '@pagopa/io-react-native-login-utils';
import { Button, SafeAreaView, StyleSheet, Text, View } from 'react-native';

export default function App() {
  const [outcome, setOutcome] = React.useState<string | undefined>();
  const [result, setResult] = React.useState<string[] | undefined>();

  React.useEffect(() => {
    console.log('First render!');
  }, []);

  React.useEffect(() => {
    getRedirects('https://tinyurl.com/testG0', {}, '')
      .then(setResult)
      .catch((err: LoginUtilsError) => {
        console.log(err);
      });
  }, []);

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.container}>
        <>
          {result?.map((url, index) => (
            <Text key={index}>Result: {`${index}: ${url}`}</Text>
          ))}
          {outcome && <Text>{outcome}</Text>}
        </>
      </View>
      <Button
        title="Test Custom Tabs"
        onPress={() => {
          openAuthenticationSession(
            'http://192.168.1.63:3000/payment-wallet?transactionId=01HNAS7T0D6XXEHK40XJN7MJRB',
            'iowallet'
          )
            .then((data) => {
              console.log(data);
              setOutcome(data);
            })
            .catch((err) => {
              console.log(err);
              setOutcome(undefined);
            });
        }}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
});
