import AuthenticationServices

@objc(IoLoginUtils)
class IoLoginUtils: NSObject {

    @objc(supportsInAppBrowser:withRejecter:)
    func supportsInAppBrowser(resolve:@escaping RCTPromiseResolveBlock, reject:@escaping RCTPromiseRejectBlock) -> Void {
        resolve(true)
    }

    @objc(getRedirects:withHeaders:withCallbackUrlParameter:withResolver:withRejecter:)
    func getRedirects(for url: String, headers:[String: String],callbackUrlParameter: String,resolve:@escaping RCTPromiseResolveBlock,
                        reject:@escaping RCTPromiseRejectBlock) -> Void {
        var session: URLSession
        guard let parsedUrl = URL(string: url) else {
            reject("NativeRedirectError","See user info",generateErrorObject(error: "InvalidURL",responseCode: nil,url: nil,parameters: nil))
            return
        }
        let delegate = RedirectDelegate(callback: callbackUrlParameter, reject: reject)
        session = URLSession(configuration: .default, delegate: delegate, delegateQueue: nil)
        
        var request = URLRequest(url: parsedUrl)
        request.httpMethod = "GET"
        
        for (key,value) in headers {
            request.addValue(value, forHTTPHeaderField: key)
        }
        
        
        session.dataTask(with: request) { data, response, error in
            // Invalidate the session when we exit the function scope
            defer { session.finishTasksAndInvalidate() }
            if (error != nil) {
                reject("NativeRedirectError","See user info",generateErrorObject(error: "RequestError",responseCode: nil,url: nil,parameters: nil))
                return
            }
            guard let httpResponse = response as? HTTPURLResponse else {
                reject("NativeRedirectError","See user info",generateErrorObject(error: "InvalidResponse",responseCode: nil,url: nil,parameters: nil))
                    return
                }
            if httpResponse.statusCode >= 400 {
                let urlParameters = getUrlQueryParameters(url: parsedUrl.absoluteString)
                let urlNoQuery = getUrlNoQuery(url: parsedUrl.absoluteString)
                let errorObject = generateErrorObject(error: "RedirectingError", responseCode: httpResponse.statusCode, url: urlNoQuery, parameters: urlParameters)
                reject("NativeRedirectError","See user info",errorObject)
                return
            }
            resolve(delegate.redirects)
            return
        }.resume()
        
    }
    
    @objc(openAuthenticationSession:withCallbackScheme:shareiOSCookies:withResolver:withRejecter:)
    func openAuthenticationSession(for url: String, callbackScheme: String, shareiOSCookies: Bool, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) -> Void {
        if #available(iOS 13.0, *) {
            var authSession: ASWebAuthenticationSession?
            guard let authUrl = URL(string: url) else {
                let urlParameters = getUrlQueryParameters(url: url)
                let urlNoQuery = getUrlNoQuery(url: url)
                let errorObject = generateErrorObject(error: "InvalidURL", responseCode: nil, url: urlNoQuery, parameters: urlParameters)
                reject("NativeAuthSessionError", "See user info", errorObject)
                return
            }
            
            authSession = ASWebAuthenticationSession(url: authUrl, callbackURLScheme: callbackScheme) { (url, error) in
                authSession = nil
                if let error = error {
                    guard let url = url else {
                        let nsError = error as NSError
                        if nsError.code == 1 {
                            let errorObject = generateErrorObject(error: "NativeAuthSessionClosed", responseCode: nil, url: nil, parameters: nil)
                            reject("NativeAuthSessionError", "See user info", errorObject)
                            return
                        }
                        
                        let errorObject = generateErrorObject(error: "MissingResponseURL", responseCode: nil, url: nil, parameters: nil)
                        reject("NativeAuthSessionError", "See user info", errorObject)
                        return
                    }
                    let urlParameters = getUrlQueryParameters(url: url.absoluteString)
                    let urlNoQuery = getUrlNoQuery(url: url.absoluteString)
                    let errorObject = generateErrorObject(error: "ErrorOnResponseOrNativeComponent", responseCode: nil, url: urlNoQuery, parameters: urlParameters)
                    reject("NativeAuthSessionError", "See user info", errorObject)
                    return
                } else if let url = url {
                    resolve(url.absoluteString)
                    return
                } else {
                    let errorObject = generateErrorObject(error: "GenericErrorOnResponse", responseCode: nil, url: nil, parameters: nil)
                    reject("NativeAuthSessionError", "See user info", errorObject)
                    return
                }
            }
            
            guard let authSession = authSession else {
                let errorObject = generateErrorObject(error: "NativeComponentNotInstantiated", responseCode: nil, url: nil, parameters: nil)
                reject("NativeAuthSessionError", "See user info", errorObject)
                return
            }
            authSession.prefersEphemeralWebBrowserSession = !shareiOSCookies
            authSession.presentationContextProvider = self
            authSession.start()
        }
        else{
            let errorObject = generateErrorObject(error: "iOSVersionNotSupported", responseCode: nil, url: nil, parameters: nil)
            reject("NativeAuthSessionError", "See user info", errorObject)
            return
        }
        
    }
}
@available(iOS 13.0, *)
extension IoLoginUtils: ASWebAuthenticationPresentationContextProviding {
    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        guard let keyWindow = UIApplication.shared.keyWindow else {
            return DispatchQueue.main.sync { UIWindow() }
        }
        return keyWindow
    }
    
}

class RedirectDelegate: NSObject, URLSessionTaskDelegate {
    var redirects: [String] = []
    let callback: String
    let reject: RCTPromiseRejectBlock
        
    init(callback: String, reject: @escaping RCTPromiseRejectBlock) {
        self.callback = callback
        self.reject = reject
    }

    deinit {
        #if DEBUG
        print("RedirectDelegate cleaned up!")
        #endif
    }
    
    func urlSession(_ session: URLSession, task: URLSessionTask, willPerformHTTPRedirection response: HTTPURLResponse, newRequest request: URLRequest, completionHandler: @escaping (URLRequest?) -> Void) {
        if response.statusCode >= 300 && response.statusCode <= 399 {
            guard let newUrl = request.url?.absoluteString else {
                let errorObject = generateErrorObject(error: "RedirectingErrorMissingURL", responseCode: nil, url: nil, parameters: nil)
                reject("NativeRedirectError","See user info",errorObject)
                return
            }
            redirects.append(newUrl)
            if let headerFields = response.allHeaderFields as? [String: String],
               let url = response.url {
              
              let cookies = HTTPCookie.cookies(withResponseHeaderFields: headerFields, for: url)
              if cookies.isEmpty {
                if getUrlQueryParameters(url: newUrl).contains(self.callback) {
                  completionHandler(nil)
                } else {
                  completionHandler(request)
                }
                return
              }
              
              let dispatchGroup = DispatchGroup()
              // [WKWebsiteDataStore httpCookieStore] must be used from main thread only
              DispatchQueue.main.async {
                let cookieStore = WKWebsiteDataStore.default().httpCookieStore
                for cookie in cookies {
                  dispatchGroup.enter()
                  cookieStore.setCookie(cookie) {
                    dispatchGroup.leave()
                  }
                }
              }
              
              // Wait for all cookies to be set (on the main thread)
              // and then execute the notify block (also on the main thread)/ e poi esegui il blocco notify (anch'esso sul main thread)
              dispatchGroup.notify(queue: .main) {
                if getUrlQueryParameters(url: newUrl).contains(self.callback) {
                  completionHandler(nil)
                } else {
                  completionHandler(request)
                }
              }
              return
            } else {
              // Se headerFields o url non sono disponibili, gestisci come se non ci fossero cookie.
              // La logica originale qui non chiamava completionHandler, il che è un bug.
              // Correggiamolo chiamando completionHandler.
              if getUrlQueryParameters(url: newUrl).contains(self.callback) {
                completionHandler(nil)
              } else {
                completionHandler(request)
              }
              return
            }
        } else if response.statusCode >= 400{
            let urlParameters = getUrlQueryParameters(url: redirects.last ?? "")
            let urlNoQuery = getUrlNoQuery(url: redirects.last ?? "")
            let errorObject = generateErrorObject(error: "RedirectingError", responseCode: response.statusCode, url: urlNoQuery, parameters: urlParameters)
            reject("NativeRedirectError","See user info",errorObject)
            completionHandler(nil)
            return
        }
        else {
            completionHandler(nil)
            return
        }
    }
}

func getUrlNoQuery(url: String) -> String {
    guard let urlAsURL =  URLComponents(string: url),
          let scheme = urlAsURL.scheme,
          let host = urlAsURL.host
    else {
        return ""
    }
    return "\(scheme)://\(host)\(urlAsURL.path)"
    
}

func getUrlQueryParameters(url: String) -> [String] {
    var parameters: [String] = []
    
    if let urlComponents = URLComponents(string: url), let queryItems = urlComponents.queryItems {
            for queryItem in queryItems {
                parameters.append(queryItem.name)
            }
        }
    
    return parameters
}

func generateErrorObject(error: String, responseCode: Int?, url: String?, parameters: [String]?) -> NSError {
    var errorObject = [String: Any]()
    errorObject["error"] = error
    if let responseCode = responseCode {
        errorObject["statusCode"] = responseCode
    }
    if let url = url {
        errorObject["url"] = url
        if let parameters = parameters {
            var writableArray = [String]()
            for str in parameters {
                writableArray.append(str)
            }
            errorObject["parameters"] = writableArray
        }
    }
    
    return nativeRedirectError(errorObject: errorObject)
}

func nativeRedirectError(errorObject: [String:Any]) -> NSError {
    return NSError(domain: "", code: -1, userInfo: errorObject)
}
