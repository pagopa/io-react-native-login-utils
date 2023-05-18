import * as React from 'react';

import { StyleSheet, View, Text, Button, SafeAreaView } from 'react-native';
import {
  getRedirects,
  openAuthenticationSession,
} from '@pagopa/io-react-native-login-utils';

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
            'http://127.0.0.1:3000/login?authLevel-SpidL2&entityID-posteid',
            'iologin'
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
