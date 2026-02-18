import Foundation
#if canImport(PicaNetworkLoggerShared)
import PicaNetworkLoggerShared
#endif

class InspectorURLProtocol: URLProtocol, URLSessionDataDelegate {
    private static let handledKey = "CapHttpInspectorHandled"
    private var urlSession: URLSession?
    private var urlTask: URLSessionDataTask?
    private var negotiatedProtocol: String?
    private var receivedData = Data()
    private var receivedResponse: HTTPURLResponse?
    private var requestId: String = ""

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

        #if canImport(PicaNetworkLoggerShared)
        requestId = InspectorLogger.shared.logStart(request: request)
        #endif

        let mutableRequest = (request as NSURLRequest).mutableCopy() as! NSMutableURLRequest
        URLProtocol.setProperty(true, forKey: InspectorURLProtocol.handledKey, in: mutableRequest)

        let session = URLSession(configuration: .default, delegate: self, delegateQueue: nil)
        self.urlSession = session
        urlTask = session.dataTask(with: mutableRequest as URLRequest)
        urlTask?.resume()
    }

    override func stopLoading() {
        urlTask?.cancel()
        urlTask = nil
        urlSession?.invalidateAndCancel()
        urlSession = nil
    }

    // MARK: - URLSessionDataDelegate

    func urlSession(_ session: URLSession, dataTask: URLSessionDataTask, didReceive response: URLResponse, completionHandler: @escaping (URLSession.ResponseDisposition) -> Void) {
        receivedResponse = response as? HTTPURLResponse
        client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
        completionHandler(.allow)
    }

    func urlSession(_ session: URLSession, dataTask: URLSessionDataTask, didReceive data: Data) {
        receivedData.append(data)
        client?.urlProtocol(self, didLoad: data)
    }

    func urlSession(_ session: URLSession, task: URLSessionTask, didCompleteWithError error: Error?) {
        if let error = error {
            #if canImport(PicaNetworkLoggerShared)
            InspectorLogger.shared.logFinish(id: requestId, response: receivedResponse, data: receivedData, error: error, protocol: negotiatedProtocol)
            #endif
            client?.urlProtocol(self, didFailWithError: error)
        } else {
            #if canImport(PicaNetworkLoggerShared)
            InspectorLogger.shared.logFinish(id: requestId, response: receivedResponse, data: receivedData, error: nil, protocol: negotiatedProtocol)
            #endif
            client?.urlProtocolDidFinishLoading(self)
        }
    }

    func urlSession(_ session: URLSession, task: URLSessionTask, didFinishCollecting metrics: URLSessionTaskMetrics) {
        guard let transaction = metrics.transactionMetrics.last else { return }
        if #available(iOS 13.0, *) {
            negotiatedProtocol = transaction.networkProtocolName
        }
    }
}
