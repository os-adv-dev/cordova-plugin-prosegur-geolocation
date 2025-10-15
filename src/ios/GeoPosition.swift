import Foundation

struct GeoPosition: Codable {
    let DateTime: String
    let Latitude: Double
    let Longitude: Double
    let UserId: String
    let CenterId: String
    let ProvenanceId: Int
    let GeoLocationTypeId: Int

    private static let dateFormatter: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.timeZone = TimeZone(identifier: "UTC")
        return formatter
    }()

    init(dateTimeMobile: Date, latitude: Double, longitude: Double, userId: String, centerId: String, provenanceId: Int, geoLocationTypeId: Int) {
        self.DateTime = GeoPosition.dateFormatter.string(from: dateTimeMobile)
        self.Latitude = latitude
        self.Longitude = longitude
        self.UserId = userId
        self.CenterId = centerId
        self.ProvenanceId = provenanceId
        self.GeoLocationTypeId = geoLocationTypeId
    }
}

struct GeoPositionResponse: Codable {
    let message: String?
}
