import AuthenticationServices

@objc(IoLoginUtils)
class IoLoginUtils: NSObject {

 @objc(getRedirects:withHeaders:withResolver:withRejecter:)
    func getRedirects(for url: String, headers:[String: String],resolve:@escaping RCTPromiseResolveBlock,
                        reject:@escaping RCTPromiseRejectBlock) -> Void {
        var session: URLSession?
        let parsedUrl = URL(string: url)!
        let delegate = RedirectDelegate()
        session = URLSession(configuration: .default, delegate: delegate, delegateQueue: nil)
        
        var request = URLRequest(url: parsedUrl)
        request.httpMethod = "GET"
        
        for (key,value) in headers {
            request.addValue(value, forHTTPHeaderField: key)
        }
        
        
        session!.dataTask(with: request) { data, response, error in
            resolve(delegate.redirects)
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
    
    func urlSession(_ session: URLSession, task: URLSessionTask, willPerformHTTPRedirection response: HTTPURLResponse, newRequest request: URLRequest, completionHandler: @escaping (URLRequest?) -> Void) {
        if response.statusCode >= 300 && response.statusCode <= 399 {
            redirects.append(request.url!.absoluteString)
            completionHandler(request)
        } else {
            completionHandler(nil)
        }
    }
}
