import AuthenticationServices

@objc(IoLoginUtils)
class IoLoginUtils: NSObject {

    @objc(getRedirects:withHeaders:withCallbackUrlParameter:withResolver:withRejecter:)
    func getRedirects(for url: String, headers:[String: String],callbackUrlParameter: String,resolve:@escaping RCTPromiseResolveBlock,
                        reject:@escaping RCTPromiseRejectBlock) -> Void {
        var session: URLSession
        guard let parsedUrl = URL(string: url) else {
            reject("NativeRedirectError","",generateErrorObject(error: "Invalid URL",responseCode: nil,url: nil,parameters: nil))
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
                reject("NativeRedirectError","",generateErrorObject(error: "\(String(describing: error))",responseCode: nil,url: nil,parameters: nil))
                return
            }
            guard let httpResponse = response as? HTTPURLResponse else {
                reject("NativeRedirectError","",generateErrorObject(error: "Invalid response",responseCode: nil,url: nil,parameters: nil))
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
    
    @objc(openAuthenticationSession:withCallbackScheme:withResolver:withRejecter:)
    func openAuthenticationSession(for url: String, callbackScheme: String, resolve: @escaping RCTPromiseResolveBlock, reject: @escaping RCTPromiseRejectBlock) -> Void {
        
        if #available(iOS 13.0, *) {
            var authSession: ASWebAuthenticationSession?
            guard let authUrl = URL(string: url) else {
                reject("INVALID_URL", "Invalid URL", nil)
                return
            }
            
            authSession = ASWebAuthenticationSession(url: authUrl, callbackURLScheme: callbackScheme) { (url, error) in
                authSession = nil
                if let error = error {
                    let description = "\(error)"
                    reject("AUTH_ERROR",description,nil)
                } else if let url = url {
                    resolve(url.absoluteString)
                } else {
                    reject("UNKNOWN_ERROR", "Unknown error occurred", nil)
                }
            }
            
            authSession?.prefersEphemeralWebBrowserSession = true
            authSession!.presentationContextProvider = self
            authSession!.start()
        }
        else{
            reject("VERSION_ERROR","This iOS version is not supported",nil)
        }
        
    }
}
@available(iOS 13.0, *)
extension IoLoginUtils: ASWebAuthenticationPresentationContextProviding {
    
    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        return UIApplication.shared.keyWindow!
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
            guard let newUrl = request.url else {
                reject("ErrorNativeRedirect","Invalid redirectUrl",nil)
                    return
                }
            redirects.append(newUrl.absoluteString)
            if getUrlQueryParameters(url: redirects.last!).contains(callback) {
                completionHandler(nil)
                return
            };
            completionHandler(request)
            return
        } else if response.statusCode >= 400{
            
            reject("ErrorNativeRedirect","\(response.statusCode) \(redirects.last ?? "")",nil);
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
    errorObject["Error"] = error
    if let responseCode = responseCode {
        errorObject["StatusCode"] = responseCode
    }
    if let url = url {
        errorObject["URL"] = url
        if let parameters = parameters {
            var writableArray = [String]()
            for str in parameters {
                writableArray.append(str)
            }
            errorObject["Parameters"] = writableArray
        }
    }
    
    return nativeRedirectError(errorObject: errorObject)
}

func nativeRedirectError(errorObject: [String:Any]) -> NSError {
    return NSError(domain: "", code: -1, userInfo: errorObject)
}
