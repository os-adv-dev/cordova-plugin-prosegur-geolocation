# Cordova Plugin Prosegur Geolocation

Cordova plugin for continuous geolocation tracking in OutSystems Native Mobile applications, with support for foreground, background, and screen-off scenarios.

## Features

- **Continuous tracking**: Captures location in foreground, background, and with screen off
- **Foreground Service**: Android service with persistent notification for background operation
- **Automatic API submission**: Sends coordinates to server at configurable intervals
- **Offline caching**: Stores data locally when offline, syncs when online
- **Android 14+ optimized**: Dedicated HandlerThread for background precision
- **iOS support**: Full implementation with CLLocationManager

## Supported Platforms

- Android (SDK 24+, optimized for Android 14+)
- iOS

## Installation

```bash
cordova plugin add cordova-plugin-prosegur-geolocation
```

Or via repository URL:

```bash
cordova plugin add https://github.com/os-adv-dev/cordova-plugin-prosegur-geolocation.git
```

## API

### initGeo

Starts geolocation tracking.

```javascript
cordova.plugins.GeolocationProsegur.initGeo(
    token,             // String: Authentication token for API
    dir,               // String: API endpoint URL
    country,           // String: Country code
    imei,              // String: Device identifier
    time,              // Int: Interval in seconds between submissions
    center,            // String: Center/unit
    user,              // String: User identifier
    provenance,        // Int: Provenance code
    geoLocationTypeId, // Int: Geolocation type
    successCallback,
    errorCallback
);
```

### stopGeo

Stops geolocation tracking.

```javascript
cordova.plugins.GeolocationProsegur.stopGeo(
    successCallback,
    errorCallback
);
```

### validateGeo

Validates the current tracking status.

```javascript
cordova.plugins.GeolocationProsegur.validateGeo(
    token,
    dir,
    country,
    imei,
    center,
    user,
    provenance,
    successCallback,
    errorCallback
);
```

## Permissions

### Android

The plugin automatically requests the following permissions:

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

### iOS

Automatically configured in Info.plist:

- `NSLocationAlwaysAndWhenInUseUsageDescription`
- `NSLocationWhenInUseUsageDescription`
- `NSLocationAlwaysUsageDescription`
- `UIBackgroundModes`: location

## Recommended Configuration

For best background precision, it is recommended to:

1. **Exclude the app from battery optimization** (Android)
2. **Keep location permissions set to "Always"**
3. **Minimum recommended interval**: 30 seconds

## Architecture (Android)

```
┌─────────────────────────────────────────────────────┐
│                  GeolocationProsegur                │
│              (Main Plugin - Kotlin)                 │
└─────────────────────┬───────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────┐
│                GeoListenerService                   │
│            (Foreground Service - Kotlin)            │
│                                                     │
│  ┌─────────────────────────────────────────────┐   │
│  │         HandlerThread (dedicated)           │   │
│  │   - Not throttled in background             │   │
│  │   - Receives location callbacks             │   │
│  └─────────────────────────────────────────────┘   │
│                                                     │
│  ┌─────────────────────────────────────────────┐   │
│  │         FusedLocationProviderClient         │   │
│  │   - PRIORITY_HIGH_ACCURACY                  │   │
│  │   - fastestInterval: 5s                     │   │
│  │   - smallestDisplacement: 0                 │   │
│  └─────────────────────────────────────────────┘   │
│                                                     │
│  ┌─────────────────────────────────────────────┐   │
│  │              Timer + Coroutines             │   │
│  │   - Triggers API submission                 │   │
│  │   - Persists offline data                   │   │
│  └─────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
```

## Changelog

### v0.0.2 (2026-01-20)

**Fix: Background location accuracy on Android 14+**

- Added dedicated `HandlerThread` for location callbacks (not throttled in background)
- Added `fastestInterval` (5s) and `smallestDisplacement` (0) to `LocationRequest`
- Improved service lifecycle cleanup
- Fixed location precision degradation when app is in background/standby

**Issue resolved:** Location became imprecise (50-500m error) on Android 14+ devices when app was in background. After fix, precision maintains ~1-2m accuracy in both foreground and background.

### v0.0.1

- Initial release
- Android implementation with Foreground Service
- iOS implementation with CLLocationManager
- Offline caching support
- API integration with Retrofit

## Project Structure

```
cordova-plugin-prosegur-geolocation/
├── plugin.xml                    # Plugin configuration
├── package.json                  # NPM metadata
├── www/
│   └── GeolocationProsegur.js   # JavaScript interface
├── src/
│   ├── android/
│   │   ├── GeolocationProsegur.kt    # Main plugin
│   │   ├── GeoListenerService.kt     # Foreground Service
│   │   ├── GeoPosittion.kt           # Data model
│   │   ├── APIService.kt             # Retrofit interface
│   │   ├── ApiUtils.kt               # API utilities
│   │   ├── RetrofitClient.kt         # HTTP client
│   │   └── prosegurgeolocation.gradle
│   └── ios/
│       ├── GeolocationProsegur.swift # Main plugin
│       ├── LocationService.swift     # Location service
│       ├── GeoPosition.swift         # Data model
│       └── APIClient.swift           # HTTP client
└── hooks/
    ├── install_prerequisites.js
    └── add_swift_support.js
```

## Support

To report issues or request features, open an issue on the GitHub repository.

## License

Proprietary - Prosegur / OutSystems
