import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package 'react-native-io-login-utils' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: '' }) +
  '- You rebuilt the app after installing the package\n' +
  '- You are not using Expo Go\n';

const IoLoginUtils = NativeModules.IoLoginUtils
  ? NativeModules.IoLoginUtils
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

    export function getRedirects(
      url: string,
      headers: object
    ): Promise<Array<string>> {
      return IoLoginUtils.getRedirects(url, headers);
    }
    
    export function openAuthenticationSession(
      url: string,
      callbackURLScheme: string
    ): Promise<string> {
      return IoLoginUtils.openAuthenticationSession(
        url,
        callbackURLScheme
      );
    }