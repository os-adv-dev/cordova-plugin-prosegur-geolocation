import Foundation
import CoreLocation

@available(iOS 14.0, *)
@objc(GeolocationProsegur)
class GeolocationProsegur: CDVPlugin {

    private var locationService: LocationService?

    override func pluginInitialize() {
        super.pluginInitialize()
        locationService = LocationService()
        locationService?.delegate = self
        NSLog("[GeolocationProsegur] Plugin initialized")
    }

    @objc(initGeo:)
    func initGeo(command: CDVInvokedUrlCommand) {
        NSLog("[GeolocationProsegur] initGeo called")
        NSLog("[GeolocationProsegur] Arguments count: \(command.arguments.count)")
        NSLog("[GeolocationProsegur] locationService is nil? \(locationService == nil)")

        guard command.arguments.count >= 9 else {
            NSLog("[GeolocationProsegur] ❌ FAILED: Not enough arguments (need 9, got \(command.arguments.count))")
            let result = CDVPluginResult(
                status: CDVCommandStatus_ERROR,
                messageAs: "Missing required parameters - need 9, got \(command.arguments.count)"
            )
            self.commandDelegate?.send(result, callbackId: command.callbackId)
            return
        }

        NSLog("[GeolocationProsegur] ✅ Argument count OK, parsing parameters...")

        guard let token = command.arguments[0] as? String,
              let url = command.arguments[1] as? String,
              let country = command.arguments[2] as? String,
              let imei = command.arguments[3] as? String,
              let updateInterval = command.arguments[4] as? Int,
              let centerId = command.arguments[5] as? String,
              let userId = command.arguments[6] as? String,
              let provenanceId = command.arguments[7] as? Int,
              let geoLocationTypeId = command.arguments[8] as? Int else {
            NSLog("[GeolocationProsegur] ❌ FAILED: Parameter type casting failed")
            NSLog("[GeolocationProsegur] Args: \(command.arguments)")
            let result = CDVPluginResult(
                status: CDVCommandStatus_ERROR,
                messageAs: "Invalid parameter types"
            )
            self.commandDelegate?.send(result, callbackId: command.callbackId)
            return
        }

        NSLog("[GeolocationProsegur] ✅ Parameters parsed successfully")
        NSLog("[GeolocationProsegur] Configuring locationService...")

        locationService?.configure(
            token: token,
            url: url,
            country: country,
            imei: imei,
            updateInterval: updateInterval,
            centerId: centerId,
            userId: userId,
            provenanceId: provenanceId,
            geoLocationTypeId: geoLocationTypeId
        )

        NSLog("[GeolocationProsegur] ✅ Configuration complete, starting tracking...")
        locationService?.startTracking()
        NSLog("[GeolocationProsegur] ✅ startTracking() called")

        let result = CDVPluginResult(
            status: CDVCommandStatus_OK,
            messageAs: "Geolocation tracking started"
        )
        self.commandDelegate?.send(result, callbackId: command.callbackId)

        NSLog("[GeolocationProsegur] Configuration: token=\(token), url=\(url), country=\(country), imei=\(imei), interval=\(updateInterval)s")
    }

    @objc(stopGeo:)
    func stopGeo(command: CDVInvokedUrlCommand) {
        NSLog("[GeolocationProsegur] stopGeo called")

        locationService?.stopTracking()

        let result = CDVPluginResult(
            status: CDVCommandStatus_OK,
            messageAs: "Geolocation tracking stopped"
        )
        self.commandDelegate?.send(result, callbackId: command.callbackId)
    }

    @objc(checkGeo:)
    func checkGeo(command: CDVInvokedUrlCommand) {
        NSLog("[GeolocationProsegur] checkGeo called")

        guard command.arguments.count >= 7 else {
            let result = CDVPluginResult(
                status: CDVCommandStatus_ERROR,
                messageAs: "Missing required parameters"
            )
            self.commandDelegate?.send(result, callbackId: command.callbackId)
            return
        }

        guard let token = command.arguments[0] as? String,
              let url = command.arguments[1] as? String,
              let country = command.arguments[2] as? String,
              let imei = command.arguments[3] as? String,
              let centerId = command.arguments[4] as? String,
              let userId = command.arguments[5] as? String,
              let provenanceId = command.arguments[6] as? Int else {
            let result = CDVPluginResult(
                status: CDVCommandStatus_ERROR,
                messageAs: "Invalid parameter types"
            )
            self.commandDelegate?.send(result, callbackId: command.callbackId)
            return
        }

        // Configure with minimal settings for validation
        locationService?.configure(
            token: token,
            url: url,
            country: country,
            imei: imei,
            updateInterval: 300,
            centerId: centerId,
            userId: userId,
            provenanceId: provenanceId,
            geoLocationTypeId: 1
        )

        let isValid = locationService?.validateGeoLocation() ?? false

        if isValid {
            let result = CDVPluginResult(
                status: CDVCommandStatus_OK,
                messageAs: "Geolocation configuration is valid"
            )
            self.commandDelegate?.send(result, callbackId: command.callbackId)
        } else {
            let result = CDVPluginResult(
                status: CDVCommandStatus_ERROR,
                messageAs: "Geolocation configuration is invalid or permissions missing"
            )
            self.commandDelegate?.send(result, callbackId: command.callbackId)
        }

        NSLog("[GeolocationProsegur] Validation result: \(isValid)")
    }

    @objc(stop:)
    func stop(command: CDVInvokedUrlCommand) {
        NSLog("[GeolocationProsegur] stop called (legacy method)")
        stopGeo(command: command)
    }

    override func onAppTerminate() {
        NSLog("[GeolocationProsegur] App terminating, stopping location service")
        locationService?.stopTracking()
        super.onAppTerminate()
    }

    override func onReset() {
        NSLog("[GeolocationProsegur] Plugin reset")
        super.onReset()
    }
}

// MARK: - LocationServiceDelegate
@available(iOS 14.0, *)
extension GeolocationProsegur: LocationServiceDelegate {

    func locationService(_ service: LocationService, didUpdateLocation location: CLLocation) {
        // Can send events to JavaScript if needed
        NSLog("[GeolocationProsegur] Location updated: \(location.coordinate.latitude), \(location.coordinate.longitude)")
    }

    func locationService(_ service: LocationService, didFailWithError error: Error) {
        NSLog("[GeolocationProsegur] Location service error: \(error.localizedDescription)")
    }
}
