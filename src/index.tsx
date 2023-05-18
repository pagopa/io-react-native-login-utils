import { NativeModules, Platform } from 'react-native';

const LINKING_ERROR =
  `The package '@pagopa/io-react-native-login-utils' doesn't seem to be linked. Make sure: \n\n` +
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

export type AreaError = 'NativeRedirectError' | 'NativeAuthSessionError';

export type IosError =
  | 'Invalid URL'
  | 'Request Error'
  | 'Invalid response'
  | 'Redirecting Error'
  | 'NativeAuthSessionClosed'
  | 'MissingResponseURL'
  | 'ErrorOnResponseOrNativeComponent'
  | 'GenericErrorOnResponse'
  | 'NativeComponentNotInstantiated'
  | 'iOSVersionNotSupported'
  | 'Redirecting Error-MissingURL';

export type AndroidError =
  | 'ErrorOnClientSync'
  | 'NativeAuthSessionClosed'
  | 'NativeComponentNotInstantiated'
  | 'First Request Error'
  | 'Error while creating connection redirect'
  | 'Redirecting Error'
  | 'MissingDataFromIntent'
  | 'CustomTabContextIsNull'
  | 'CustomTabActivityContextIsNull';

export type Error = IosError | AndroidError;

export type LoginUtilsError = {
  userInfo: {
    Error: Error;
    URL: string | undefined;
    StatusCode: number | undefined;
    Parameter: Array<string> | undefined;
  };
  code: AreaError;
};

export function getRedirects(
  url: string,
  headers: object,
  callbackURLParameter: string
): Promise<Array<string>> {
  return IoLoginUtils.getRedirects(url, headers, callbackURLParameter);
}

export function openAuthenticationSession(
  url: string,
  callbackURLScheme: string
): Promise<string> {
  return IoLoginUtils.openAuthenticationSession(url, callbackURLScheme);
}
