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
            reject("NativeRedirectError","",generateErrorObject(error: "InvalidURL",responseCode: nil,url: nil,parameters: nil))
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
            if (error != nil) {
                reject("NativeRedirectError","",generateErrorObject(error: "RequestError",responseCode: nil,url: nil,parameters: nil))
                return
            }
            guard let httpResponse = response as? HTTPURLResponse else {
                reject("NativeRedirectError","",generateErrorObject(error: "InvalidResponse",responseCode: nil,url: nil,parameters: nil))
                    return
                }
            if httpResponse.statusCode >= 400 {
                let urlParameters = getUrlQueryParameters(url: parsedUrl.absoluteString)
                let urlNoQuery = getUrlNoQuery(url: parsedUrl.absoluteString)
                let errorObject = generateErrorObject(error: "RedirectingError", responseCode: httpResponse.statusCode, url: urlNoQuery, parameters: urlParameters)
                reject("NativeRedirectError","",errorObject)
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
                reject("NativeAuthSessionError", "", errorObject)
                return
            }
            
            authSession = ASWebAuthenticationSession(url: authUrl, callbackURLScheme: callbackScheme) { (url, error) in
                authSession = nil
                if let error = error {
                    guard let url = url else {
                        let nsError = error as NSError
                        if nsError.code == 1 {
                            let errorObject = generateErrorObject(error: "NativeAuthSessionClosed", responseCode: nil, url: nil, parameters: nil)
                            reject("NativeAuthSessionError", "", errorObject)
                            return
                        }
                        
                        let errorObject = generateErrorObject(error: "MissingResponseURL", responseCode: nil, url: nil, parameters: nil)
                        reject("NativeAuthSessionError", "", errorObject)
                        return
                    }
                    let urlParameters = getUrlQueryParameters(url: url.absoluteString)
                    let urlNoQuery = getUrlNoQuery(url: url.absoluteString)
                    let errorObject = generateErrorObject(error: "ErrorOnResponseOrNativeComponent", responseCode: nil, url: urlNoQuery, parameters: urlParameters)
                    reject("NativeAuthSessionError", "", errorObject)
                    return
                } else if let url = url {
                    resolve(url.absoluteString)
                    return
                } else {
                    let errorObject = generateErrorObject(error: "GenericErrorOnResponse", responseCode: nil, url: nil, parameters: nil)
                    reject("NativeAuthSessionError", "", errorObject)
                    return
                }
            }
            
            guard let authSession = authSession else {
                let errorObject = generateErrorObject(error: "NativeComponentNotInstantiated", responseCode: nil, url: nil, parameters: nil)
                reject("NativeAuthSessionError", "", errorObject)
                return
            }
            authSession.prefersEphemeralWebBrowserSession = !shareiOSCookies
            authSession.presentationContextProvider = self
            authSession.start()
        }
        else{
            let errorObject = generateErrorObject(error: "iOSVersionNotSupported", responseCode: nil, url: nil, parameters: nil)
            reject("NativeAuthSessionError", "", errorObject)
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

    
    func urlSession(_ session: URLSession, task: URLSessionTask, willPerformHTTPRedirection response: HTTPURLResponse, newRequest request: URLRequest, completionHandler: @escaping (URLRequest?) -> Void) {
        if response.statusCode >= 300 && response.statusCode <= 399 {
            guard let newUrl = request.url?.absoluteString else {
                let errorObject = generateErrorObject(error: "RedirectingErrorMissingURL", responseCode: nil, url: nil, parameters: nil)
                reject("NativeRedirectError","",errorObject)
                return
            }
            redirects.append(newUrl)
            if getUrlQueryParameters(url: newUrl).contains(callback) {
                completionHandler(nil)
                return
            };
            completionHandler(request)
            return
        } else if response.statusCode >= 400{
            let urlParameters = getUrlQueryParameters(url: redirects.last ?? "")
            let urlNoQuery = getUrlNoQuery(url: redirects.last ?? "")
            let errorObject = generateErrorObject(error: "RedirectingError", responseCode: response.statusCode, url: urlNoQuery, parameters: urlParameters)
            reject("NativeRedirectError","",errorObject)
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
