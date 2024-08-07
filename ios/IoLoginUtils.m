#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE(IoLoginUtils, NSObject)

RCT_EXTERN_METHOD(getRedirects:(NSString*)url
                 withHeaders:(NSDictionary*)headers
                 withCallbackUrlParameter:(NSString*)callbackUrlParameter
                 withResolver:(RCTPromiseResolveBlock)resolve
                 withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(openAuthenticationSession:(NSString*)url
                 withCallbackScheme:(NSString*)callbackScheme
                 shareiOSCookies:(BOOL)shareCookies
                 withResolver:(RCTPromiseResolveBlock)resolve
                 withRejecter:(RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(supportsInAppBrowser:(RCTPromiseResolveBlock)resolve withRejecter:(RCTPromiseRejectBlock)reject)

+ (BOOL)requiresMainQueueSetup
{
  return NO;
}

@end
