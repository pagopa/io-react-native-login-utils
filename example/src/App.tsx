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
  Switch,
  Text,
  View,
  Platform, // Import Platform
  useColorScheme,
} from 'react-native';
import CookieManager from '@react-native-cookies/cookies';
import { WebView } from 'react-native-webview';
import { Picker } from '@react-native-picker/picker'; // Import Picker

const REDIRECT_URL_HOST_IDP = 'https://app-backend.io.italia.it';
const REDIRECT_URL_HOST = 'https://www.versionestabile.it';
const REDIRECT_URL_PATH = '/pagopa/public/redirect-and-cookie';

const IDP_OPTIONS = [
  'arubaid',
  'ehtid',
  'infocamereid',
  'infocertid',
  'intesiid',
  'lepidaid',
  'namirialid',
  'posteid',
  'sielteid',
  'spiditalia',
  'timid',
  'teamsystemid',
  'xx_servizicie',
];

export default function App() {
  const colorScheme = useColorScheme();
  const isDarkMode = colorScheme === 'dark';
  const textColor = { color: isDarkMode ? '#FFFFFF' : '#000000' };

  const [authResult, setAuthResult] = React.useState<string | undefined>();
  const [useIdpUrl, setUseIdpUrl] = React.useState<boolean>(false);
  const [selectedIdp, setSelectedIdp] = React.useState<string>(
    IDP_OPTIONS[0] as string
  );
  const [redirectResult, setRedirectResult] = React.useState<
    string[] | undefined
  >();
  const [inAppBrowserSupported, setInAppBrowserSupported] = React.useState<
    boolean | undefined
  >(undefined);

  const dynamicRedirectUrlPathPoste = `/login?entityID=${selectedIdp}&authLevel=SpidL2`;
  const testLoginUrl = `http://127.0.0.1:3000/login?authLevel-SpidL2&entityID-${selectedIdp}`;

  const REDIRECT_URL = useIdpUrl
    ? `${REDIRECT_URL_HOST_IDP}${dynamicRedirectUrlPathPoste}` // Use dynamic path
    : `${REDIRECT_URL_HOST}${REDIRECT_URL_PATH}`;

  const QUERY_PARAM = useIdpUrl ? 'SAMLRequest' : 'testcookie';

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
          <Text numberOfLines={2} style={textColor} key={index}>
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
      <View style={styles.statusInfoContainer}>
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

      {/* Control Group for Redirect Testing */}
      <View style={styles.controlGroup}>
        <View style={styles.inlineSetting}>
          <Text style={textColor}>Use IdP Flow</Text>
          <Switch onValueChange={setUseIdpUrl} value={useIdpUrl} />
        </View>

        <View style={styles.inlineSetting}>
          <Text style={[textColor, !useIdpUrl && styles.disabledText]}>
            Identity Provider:
          </Text>
          <Picker
            selectedValue={selectedIdp}
            style={[
              styles.picker,
              // eslint-disable-next-line react-native/no-inline-styles
              Platform.OS === 'android' && {
                color: textColor.color, // Color for selected item text in Picker view
                backgroundColor: isDarkMode ? '#3A3A3A' : '#F0F0F0', // Background for Picker view
              },
              !useIdpUrl && styles.disabledPicker, // Visual cue for disabled state
            ]}
            // itemStyle is primarily for iOS Wheel Picker item text color
            itemStyle={
              Platform.OS === 'ios' ? { color: textColor.color } : undefined
            }
            onValueChange={(itemValue) => setSelectedIdp(itemValue)}
            enabled={useIdpUrl} // Picker is usable only if Poste ID Flow is active
            dropdownIconColor={textColor.color} // Color of the dropdown arrow on Android
          >
            {IDP_OPTIONS.map((idp) => (
              <Picker.Item
                key={idp}
                label={idp.toUpperCase()}
                value={idp}
                // `color` prop for Picker.Item is iOS only for item text in the dropdown/wheel
                color={Platform.OS === 'ios' ? textColor.color : undefined}
              />
            ))}
          </Picker>
        </View>

        <View style={styles.button}>
          <Button
            title="Test Redirects"
            onPress={() => {
              CookieManager.clearAll(true).then(() => {
                getRedirects(
                  REDIRECT_URL,
                  { foo: 'bar', bar: 'beer' },
                  QUERY_PARAM
                )
                  .then((values: string[]) => {
                    console.log('Redirects:', values);
                    CookieManager.get(
                      useIdpUrl ? REDIRECT_URL_HOST_IDP : REDIRECT_URL_HOST,
                      true
                    ).then((cookies) => {
                      console.log(
                        `Cookies for ${
                          useIdpUrl ? REDIRECT_URL_HOST_IDP : REDIRECT_URL_HOST
                        }:\n`,
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
      </View>

      <View style={styles.button}>
        <Button
          title="Test Login"
          onPress={() => {
            openAuthenticationSession(
              testLoginUrl, // Uses selectedIdp
              'iologin' // Callback scheme for your app
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
    // This is the main SafeAreaView container
    flex: 1,
    marginHorizontal: 16, // Standardized margin
    alignItems: 'center',
    // justifyContent: 'center', // Removed to allow content to flow from top
  },
  statusInfoContainer: {
    // Container for status texts at the top
    marginVertical: 16,
    alignItems: 'center',
  },
  // Styles for the new control group and Picker
  controlGroup: {
    width: '100%',
    maxWidth: 500, // Max width for larger screens
    marginBottom: 16,
    padding: 10,
    borderWidth: 1,
    borderColor: '#DDDDDD',
    borderRadius: 8,
    backgroundColor: 'rgba(128,128,128,0.05)', // Subtle background for the group
  },
  inlineSetting: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    width: '100%',
    marginVertical: 12,
  },
  picker: {
    height: 50,
    width: 180, // Adjust width as needed
    // Platform-specific styles are applied inline above
  },
  disabledText: {
    opacity: 0.5, // Make text look disabled
  },
  disabledPicker: {
    opacity: Platform.OS === 'ios' ? 0.5 : 0.7, // iOS Picker opacity looks better a bit lower
    // On Android, `enabled=false` often greys out the control sufficiently
  },
  // Existing styles
  button: {
    width: '100%',
    maxWidth: 500, // Max width for larger screens
    marginVertical: 8,
  },
  contentWrapper: {
    marginVertical: 8,
    marginHorizontal: 24,
  },
  box: {
    // This style is defined but not used in the provided code
    width: 60,
    height: 60,
    marginVertical: 20,
  },
  webviewContainer: {
    flex: 1,
  },
  webview: {
    flex: 1,
    marginTop: Platform.OS === 'ios' ? 44 : 0, // Adjust for status bar on iOS if needed
  },
});
