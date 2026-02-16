import Foundation
#if canImport(shared)
import shared
#endif

class InspectorURLProtocol: URLProtocol, URLSessionTaskDelegate {
    private static let handledKey = "CapHttpInspectorHandled"
    private var urlTask: URLSessionDataTask?
    private var negotiatedProtocol: String?

    override class func canInit(with request: URLRequest) -> Bool {
        if URLProtocol.property(forKey: handledKey, in: request) != nil {
            return false
        }
        return true
    }

    override class func canonicalRequest(for request: URLRequest) -> URLRequest {
        return request
    }

    override func startLoading() {
        guard let client = client else { return }

        #if canImport(shared)
        let requestId = InspectorLogger.shared.logStart(request: request)
        #else
        let requestId = UUID().uuidString
        #endif

        let mutableRequest = (request as NSURLRequest).mutableCopy() as! NSMutableURLRequest
        URLProtocol.setProperty(true, forKey: InspectorURLProtocol.handledKey, in: mutableRequest)

        let session = URLSession(configuration: .default, delegate: self, delegateQueue: nil)
        urlTask = session.dataTask(with: mutableRequest as URLRequest) { data, response, error in
            if let error = error {
                #if canImport(shared)
                InspectorLogger.shared.logFinish(id: requestId, response: response as? HTTPURLResponse, data: data, error: error, protocol: self.negotiatedProtocol)
                #endif
                client.urlProtocol(self, didFailWithError: error)
                return
            }

            if let response = response {
                client.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
            }

            if let data = data {
                client.urlProtocol(self, didLoad: data)
            }

            #if canImport(shared)
            InspectorLogger.shared.logFinish(id: requestId, response: response as? HTTPURLResponse, data: data, error: nil, protocol: self.negotiatedProtocol)
            #endif

            client.urlProtocolDidFinishLoading(self)
        }
        urlTask?.resume()
    }

    override func stopLoading() {
        urlTask?.cancel()
        urlTask = nil
    }


    func urlSession(_ session: URLSession, task: URLSessionTask, didFinishCollecting metrics: URLSessionTaskMetrics) {
        guard let transaction = metrics.transactionMetrics.last else { return }
        if #available(iOS 13.0, *) {
            negotiatedProtocol = transaction.networkProtocolName
        }
    }
}
