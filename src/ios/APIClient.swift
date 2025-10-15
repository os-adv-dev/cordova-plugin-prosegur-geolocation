import Foundation

class APIClient {

    private let baseURL: String
    private let session: URLSession

    init(baseURL: String) {
        self.baseURL = baseURL

        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 30
        config.timeoutIntervalForResource = 60
        self.session = URLSession(configuration: config)
    }

    func sendGeoLocations(
        token: String,
        country: String,
        imei: String,
        positions: [GeoPosition],
        completion: @escaping (Result<String, Error>) -> Void
    ) {
        guard let url = buildGeoLocationURL(country: country, imei: imei) else {
            completion(.failure(APIError.invalidURL))
            return
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue(token, forHTTPHeaderField: "Token")

        do {
            let encoder = JSONEncoder()
            request.httpBody = try encoder.encode(positions)
        } catch {
            completion(.failure(APIError.encodingFailed(error)))
            return
        }

        let task = session.dataTask(with: request) { data, response, error in
            if let error = error {
                completion(.failure(APIError.networkError(error)))
                return
            }

            guard let httpResponse = response as? HTTPURLResponse else {
                completion(.failure(APIError.invalidResponse))
                return
            }

            guard (200...299).contains(httpResponse.statusCode) else {
                if let data = data, let errorMessage = String(data: data, encoding: .utf8) {
                    completion(.failure(APIError.httpError(statusCode: httpResponse.statusCode, message: errorMessage)))
                } else {
                    completion(.failure(APIError.httpError(statusCode: httpResponse.statusCode, message: "Unknown error")))
                }
                return
            }

            if let data = data {
                do {
                    let decoder = JSONDecoder()
                    let response = try decoder.decode(GeoPositionResponse.self, from: data)
                    completion(.success(response.message ?? "Success"))
                } catch {
                    // If decoding fails, still consider it success if we got 2xx status
                    completion(.success("Success"))
                }
            } else {
                completion(.success("Success"))
            }
        }

        task.resume()
    }

    private func buildGeoLocationURL(country: String, imei: String) -> URL? {
        guard var components = URLComponents(string: baseURL) else { return nil }

        if components.path.isEmpty {
            components.path = "/GeoLocation_API/rest/DeviceGeoLocations/SetDeviceGeoLocations"
        } else if !components.path.hasSuffix("/SetDeviceGeoLocations") {
            components.path += "/GeoLocation_API/rest/DeviceGeoLocations/SetDeviceGeoLocations"
        }

        components.queryItems = [
            URLQueryItem(name: "country", value: country),
            URLQueryItem(name: "imei", value: imei)
        ]

        return components.url
    }
}

enum APIError: LocalizedError {
    case invalidURL
    case encodingFailed(Error)
    case networkError(Error)
    case invalidResponse
    case httpError(statusCode: Int, message: String)

    var errorDescription: String? {
        switch self {
        case .invalidURL:
            return "Invalid URL"
        case .encodingFailed(let error):
            return "Encoding failed: \(error.localizedDescription)"
        case .networkError(let error):
            return "Network error: \(error.localizedDescription)"
        case .invalidResponse:
            return "Invalid response from server"
        case .httpError(let statusCode, let message):
            return "HTTP error \(statusCode): \(message)"
        }
    }
}
