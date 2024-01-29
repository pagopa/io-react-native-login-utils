import * as React from 'react';

import {
  getRedirects,
  openAuthenticationSession,
} from '@pagopa/io-react-native-login-utils';
import { Button, SafeAreaView, StyleSheet, Text, View } from 'react-native';

export default function App() {
  const [result, setResult] = React.useState<string[] | undefined>();

  React.useEffect(() => {
    getRedirects('https://tinyurl.com/testG0', {}, '').then(setResult);
  }, []);

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.container}>
        <>
          {result?.map((url, index) => (
            <Text key={index}>Result: {`${index}: ${url}`}</Text>
          ))}
        </>
      </View>
      <Button
        title="Test Custom Tabs"
        onPress={() => {
          openAuthenticationSession(
            'http://192.168.1.63:3000/payment-wallet?transactionId=01HNAS7T0D6XXEHK40XJN7MJRB',
            'iowallet'
          )
            .then((res) => {
              console.log(res);
            })
            .catch((err) => {
              console.log(err);
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
