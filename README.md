# Cordova Plugin Prosegur Geolocation

Plugin Cordova para tracking contínuo de geolocalização em aplicações OutSystems Native Mobile, com suporte a foreground, background e screen-off.

## Funcionalidades

- **Tracking contínuo**: Captura localização em foreground, background e com tela desligada
- **Foreground Service**: Serviço Android com notificação persistente para operação em background
- **Envio automático para API**: Envia coordenadas para servidor em intervalos configuráveis
- **Cache offline**: Armazena dados localmente quando offline, sincroniza quando online
- **Android 14+ otimizado**: HandlerThread dedicada para precisão em background
- **iOS suporte**: Implementação completa com CLLocationManager

## Plataformas Suportadas

- Android (SDK 24+, otimizado para Android 14+)
- iOS

## Instalação

```bash
cordova plugin add cordova-plugin-prosegur-geolocation
```

Ou via URL do repositório:

```bash
cordova plugin add https://github.com/os-adv-dev/cordova-plugin-prosegur-geolocation.git
```

## API

### initGeo

Inicia o tracking de geolocalização.

```javascript
cordova.plugins.GeolocationProsegur.initGeo(
    token,           // String: Token de autenticação para API
    dir,             // String: URL do endpoint da API
    country,         // String: Código do país
    imei,            // String: Identificador do dispositivo
    time,            // Int: Intervalo em segundos entre envios
    center,          // String: Centro/unidade
    user,            // String: Identificador do usuário
    provenance,      // Int: Código de proveniência
    geoLocationTypeId, // Int: Tipo de geolocalização
    successCallback,
    errorCallback
);
```

### stopGeo

Para o tracking de geolocalização.

```javascript
cordova.plugins.GeolocationProsegur.stopGeo(
    successCallback,
    errorCallback
);
```

### validateGeo

Valida o status atual do tracking.

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

## Permissões

### Android

O plugin solicita automaticamente as seguintes permissões:

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

### iOS

Configurado automaticamente no Info.plist:

- `NSLocationAlwaysAndWhenInUseUsageDescription`
- `NSLocationWhenInUseUsageDescription`
- `NSLocationAlwaysUsageDescription`
- `UIBackgroundModes`: location

## Configuração Recomendada

Para melhor precisão em background, recomenda-se:

1. **Excluir o app da otimização de bateria** (Android)
2. **Manter permissões de localização "Sempre"** ativas
3. **Intervalo mínimo recomendado**: 30 segundos

## Arquitetura (Android)

```
┌─────────────────────────────────────────────────────┐
│                  GeolocationProsegur                │
│              (Plugin Principal - Kotlin)            │
└─────────────────────┬───────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────┐
│                GeoListenerService                   │
│            (Foreground Service - Kotlin)            │
│                                                     │
│  ┌─────────────────────────────────────────────┐   │
│  │         HandlerThread (dedicada)            │   │
│  │   - Não throttled em background             │   │
│  │   - Recebe callbacks de localização         │   │
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
│  │   - Dispara envio para API                  │   │
│  │   - Persiste dados offline                  │   │
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

## Estrutura do Projeto

```
cordova-plugin-prosegur-geolocation/
├── plugin.xml                    # Configuração do plugin
├── package.json                  # Metadados NPM
├── www/
│   └── GeolocationProsegur.js   # Interface JavaScript
├── src/
│   ├── android/
│   │   ├── GeolocationProsegur.kt    # Plugin principal
│   │   ├── GeoListenerService.kt     # Foreground Service
│   │   ├── GeoPosittion.kt           # Model de dados
│   │   ├── APIService.kt             # Interface Retrofit
│   │   ├── ApiUtils.kt               # Utilitários API
│   │   ├── RetrofitClient.kt         # Cliente HTTP
│   │   └── prosegurgeolocation.gradle
│   └── ios/
│       ├── GeolocationProsegur.swift # Plugin principal
│       ├── LocationService.swift     # Serviço de localização
│       ├── GeoPosition.swift         # Model de dados
│       └── APIClient.swift           # Cliente HTTP
└── hooks/
    ├── install_prerequisites.js
    └── add_swift_support.js
```

## Suporte

Para reportar issues ou solicitar features, abra uma issue no repositório GitHub.

## Licença

Proprietary - Prosegur / OutSystems
