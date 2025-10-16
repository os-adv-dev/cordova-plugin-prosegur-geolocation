import Foundation
import CoreLocation

@available(iOS 14.0, *)
protocol LocationServiceDelegate: AnyObject {
    func locationService(_ service: LocationService, didUpdateLocation location: CLLocation)
    func locationService(_ service: LocationService, didFailWithError error: Error)
}

@available(iOS 14.0, *)
class LocationService: NSObject {

    // MARK: - Properties
    private let locationManager: CLLocationManager
    private var timer: Timer?
    private var apiClient: APIClient?
    private var positionsBuffer: [GeoPosition] = []
    private var latestLocation: CLLocation?

    // Configuration
    private var token: String?
    private var baseURL: String?
    private var country: String?
    private var imei: String?
    private var userId: String?
    private var centerId: String?
    private var provenanceId: Int?
    private var geoLocationTypeId: Int?
    private var updateInterval: TimeInterval = 300 // Default 300 seconds

    weak var delegate: LocationServiceDelegate?

    // MARK: - Initialization
    override init() {
        self.locationManager = CLLocationManager()
        super.init()

        locationManager.delegate = self
        locationManager.desiredAccuracy = kCLLocationAccuracyBest
        locationManager.distanceFilter = kCLDistanceFilterNone
        locationManager.allowsBackgroundLocationUpdates = true
        locationManager.pausesLocationUpdatesAutomatically = false
        locationManager.showsBackgroundLocationIndicator = true
    }

    // MARK: - Public Methods
    func configure(
        token: String,
        baseURL: String,
        country: String,
        imei: String,
        updateInterval: Int,
        centerId: String,
        userId: String,
        provenanceId: Int,
        geoLocationTypeId: Int
    ) {
        self.token = token
        self.baseURL = baseURL
        self.country = country
        self.imei = imei
        self.userId = userId
        self.centerId = centerId
        self.provenanceId = provenanceId
        self.geoLocationTypeId = geoLocationTypeId
        self.updateInterval = TimeInterval(updateInterval)

        self.apiClient = APIClient(baseURL: baseURL)
    }

    func startTracking() {
        requestLocationPermissions()
        startLocationUpdates()
        startTimer()
        NSLog("[LocationService] Started tracking with interval: \(updateInterval)s")
    }

    func stopTracking() {
        stopTimer()
        stopLocationUpdates()
        positionsBuffer.removeAll()
        NSLog("[LocationService] Stopped tracking")
    }

    func validateGeoLocation() -> Bool {
        guard token != nil,
              baseURL != nil,
              country != nil,
              imei != nil,
              userId != nil,
              centerId != nil else {
            NSLog("[LocationService] Validation failed: Missing required parameters")
            return false
        }

        guard locationManager.authorizationStatus == .authorizedAlways ||
              locationManager.authorizationStatus == .authorizedWhenInUse else {
            NSLog("[LocationService] Validation failed: Location permission not granted")
            return false
        }

        return true
    }

    // MARK: - Private Methods
    private func requestLocationPermissions() {
        let status = locationManager.authorizationStatus

        switch status {
        case .notDetermined:
            locationManager.requestAlwaysAuthorization()
        case .restricted, .denied:
            NSLog("[LocationService] Location permission denied or restricted")
        case .authorizedWhenInUse:
            locationManager.requestAlwaysAuthorization()
        case .authorizedAlways:
            NSLog("[LocationService] Location permission granted")
        @unknown default:
            break
        }
    }

    private func startLocationUpdates() {
        locationManager.startUpdatingLocation()
    }

    private func stopLocationUpdates() {
        locationManager.stopUpdatingLocation()
    }

    private func startTimer() {
        stopTimer()

        timer = Timer.scheduledTimer(withTimeInterval: updateInterval, repeats: true) { [weak self] _ in
            self?.testGeo()  // Matches Android behavior - call testGeo on timer tick
        }

        // Ensure timer runs in background
        if let timer = timer {
            RunLoop.current.add(timer, forMode: .common)
        }

        NSLog("[LocationService] Timer started with interval: \(updateInterval)s")
    }

    private func stopTimer() {
        timer?.invalidate()
        timer = nil
    }

    private func saveGeoLocation(_ location: CLLocation) {
        guard let userId = userId,
              let centerId = centerId,
              let provenanceId = provenanceId,
              let geoLocationTypeId = geoLocationTypeId else {
            NSLog("[LocationService] Cannot save location: Missing configuration")
            return
        }

        let position = GeoPosition(
            dateTimeMobile: Date(),
            latitude: location.coordinate.latitude,
            longitude: location.coordinate.longitude,
            userId: userId,
            centerId: centerId,
            provenanceId: provenanceId,
            geoLocationTypeId: geoLocationTypeId
        )

        positionsBuffer.append(position)
        NSLog("[LocationService] NEW geoposition added, coords: \(positionsBuffer.count)")
        NSLog("[LocationService] Lat: \(location.coordinate.latitude), Lon: \(location.coordinate.longitude)")

        // Immediately send buffer (matching Android behavior at line 302)
        sendBufferedLocations()
    }

    // Matches Android's testGeo() method
    private func testGeo() {
        NSLog("[LocationService] GeoTimer called")
        NSLog("[LocationService] Buffer size before adding: \(positionsBuffer.count)")

        guard let location = latestLocation else {
            NSLog("[LocationService] No location available")
            return
        }

        // Save current location to buffer and send immediately (Android behavior)
        saveGeoLocation(location)

        NSLog("[LocationService] GeoTimer finish")
    }

    private func sendBufferedLocations() {
        guard !positionsBuffer.isEmpty else {
            NSLog("[LocationService] No positions to send")
            return
        }

        guard let apiClient = apiClient,
              let token = token,
              let country = country,
              let imei = imei else {
            NSLog("[LocationService] Cannot send: Missing API configuration")
            return
        }

        let positionsToSend = positionsBuffer
        NSLog("[LocationService] Sending \(positionsToSend.count) positions to API")

        apiClient.sendGeoLocations(token: token, country: country, imei: imei, positions: positionsToSend) { [weak self] result in
            switch result {
            case .success(let message):
                NSLog("[LocationService] API Success: \(message)")
                self?.positionsBuffer.removeAll()
                NSLog("[LocationService] Buffer cleared. Current size: 0")
            case .failure(let error):
                NSLog("[LocationService] API Error: \(error.localizedDescription)")
                // Keep positions in buffer for retry on next timer tick
                NSLog("[LocationService] Buffer kept for retry. Current size: \(self?.positionsBuffer.count ?? 0)")
            }
        }
    }
}

// MARK: - CLLocationManagerDelegate
@available(iOS 14.0, *)
extension LocationService: CLLocationManagerDelegate {

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else { return }

        // Store latest location (matching Android behavior - don't save to buffer yet)
        latestLocation = location
        NSLog("[LocationService] Location updated: \(location.coordinate.latitude), \(location.coordinate.longitude)")
        delegate?.locationService(self, didUpdateLocation: location)
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        NSLog("[LocationService] Location error: \(error.localizedDescription)")
        delegate?.locationService(self, didFailWithError: error)
    }

    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        let status = manager.authorizationStatus
        NSLog("[LocationService] Authorization changed: \(status.rawValue)")

        switch status {
        case .authorizedAlways, .authorizedWhenInUse:
            //startLocationUpdates()
            NSLog("[LocationService] Location permission granted")
        case .denied, .restricted:
            NSLog("[LocationService] Location access denied")
        case .notDetermined:
            requestLocationPermissions()
        @unknown default:
            break
        }
    }
}
