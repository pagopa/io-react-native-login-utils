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

export type IOSError =
  | 'RequestError'
  | 'InvalidResponse'
  | 'MissingResponseURL'
  | 'ErrorOnResponseOrNativeComponent'
  | 'GenericErrorOnResponse'
  | 'iOSVersionNotSupported'
  | 'RedirectingErrorMissingURL'
  | 'NativeAuthSessionClosed'
  | 'NativeComponentNotInstantiated'
  | 'RedirectingError';

export type AndroidError =
  | 'MissingActivityOnPrepare'
  | 'FirstRequestError'
  | 'ConnectionRedirectError'
  | 'BrowserNotFound'
  | 'NativeAuthSessionClosed'
  | 'NativeComponentNotInstantiated'
  | 'RedirectingError'
  | 'IllegalStateException';

export type Error = IOSError | AndroidError;

export type LoginUtilsError = {
  userInfo: {
    error: Error;
    url: string | undefined;
    statusCode: number | undefined;
    parameter: Array<string> | undefined;
  };
  code: AreaError;
};

export const isLoginUtilsError = (e: unknown): e is LoginUtilsError =>
  (e as LoginUtilsError).userInfo !== undefined;

export function getRedirects(
  url: string,
  headers: object,
  callbackURLParameter: string
): Promise<Array<string>> {
  return IoLoginUtils.getRedirects(url, headers, callbackURLParameter);
}

export function openAuthenticationSession(
  url: string,
  callbackURLScheme: string,
  shareiOSCookies: boolean = false
): Promise<string> {
  return IoLoginUtils.openAuthenticationSession(
    url,
    callbackURLScheme,
    shareiOSCookies
  );
}

export function supportsInAppBrowser(): Promise<boolean> {
  return IoLoginUtils.supportsInAppBrowser();
}
