import * as React from 'react';

import { StyleSheet, View, Text } from 'react-native';
import { getRedirects } from '@pagopa/io-react-native-login-utils';

export default function App() {
  const [result, setResult] = React.useState<string[] | undefined>();

  React.useEffect(() => {
    getRedirects('https://tinyurl.com/testG0', {}).then(setResult);
  }, []);

  return (
    <View style={styles.container}>
      <>
        {result?.map((url, index) => (
          <Text key={index}>Result: {`${index}: ${url}`}</Text>
        ))}
      </>
    </View>
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
