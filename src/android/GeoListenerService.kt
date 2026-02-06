package cordova.plugin.prosegur.geolocation

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnSuccessListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import cordova.plugin.prosegur.geolocation.ApiUtils.getAPIService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.nio.file.Files
import java.util.Calendar
import java.util.Timer
import java.util.TimerTask

class GeoListenerService : Service() {
    private var serviceId = -2
    private var mAPIService: APIService? = null
    private var callback: GeoListenerServiceCallback? = null
    private val geoListenerServiceInterface = GeoListenerServiceInterface()
    private var tokenListener: String? = null
    private var dirListener: String? = null
    private var countryListener: String? = null
    private var imeiListener: String? = null
    private var userListener: String? = null
    private var centerListener: String? = null
    private var provenanceListener = 0
    private var geoLocationTypeListener = 0
    private var timeListener = 300
    private var timerTask: TimerTask? = null
    private var timer: Timer? = null

    // Dedicated background thread for location updates (not throttled like MainLooper)
    private var locationHandlerThread: HandlerThread? = null
    private var locationHandler: Handler? = null
    private val handlerT: Handler = Handler(Looper.getMainLooper())

    // Maximum age for location data (30 seconds)
    private val MAX_LOCATION_AGE_MS = 30000L

    // Maximum acceptable accuracy in meters (reject locations with worse accuracy)
    private val MAX_ACCURACY_METERS = 100f

    // CancellationTokenSource for getCurrentLocation requests
    private var cancellationTokenSource: CancellationTokenSource? = null

    private var coords: ArrayList<GeoPosittion?> = arrayListOf()
    private var longitude = 0.0
    private var latitude = 0.0
    private var location: Location? = null
    private var locationCallback: LocationCallback? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationRequest: LocationRequest? = null
    private val runnableGeo = Runnable { this.testGeo() }
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO) + SupervisorJob()

    override fun onCreate() {
        super.onCreate()
        loadPersistedCoords()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(LOG_TAG, "🚀 Starting GeoListenerService")

        val notification = createNotification()

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                startForeground(FOREGROUND_ID, notification)
            } else {
                startForeground(
                    FOREGROUND_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                )
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "⚠️ startForeground failed: " + e.message)
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager?
            if (nm != null) nm.notify(FOREGROUND_ID, notification)
        }

        serviceId = startId
        initGPS()

        return START_STICKY
    }

    private fun createNotification(): Notification {
        val CHANNEL_ID = "geo_listener_channel"
        val CHANNEL_NAME = "Geolocation Tracking"

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existingChannel = nm.getNotificationChannel(CHANNEL_ID)
            if (existingChannel == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                )
                channel.description = "Geolocation service running"
                nm.createNotificationChannel(channel)
            }
        }

        val builder = Notification.Builder(this)
            .setContentTitle("Service is running")
            .setContentText("Service enabled")
            .setOngoing(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID)
        }

        return builder.build()
    }

    override fun onBind(intent: Intent?): IBinder {
        return geoListenerServiceInterface
    }

    override fun onDestroy() {
        Log.i(LOG_TAG, "Destruyendo aplicacion")

        if (timer != null) {
            timer!!.cancel()
        }
        if (timerTask != null) {
            timerTask!!.cancel()
        }
        handlerT.removeCallbacks(runnableGeo)

        // Cancel any pending getCurrentLocation request
        cancellationTokenSource?.cancel()
        cancellationTokenSource = null

        // Remove location updates and stop dedicated thread
        try {
            fusedLocationClient?.removeLocationUpdates(locationCallback!!)
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Error removing location updates: ${e.message}")
        }

        // Stop the dedicated HandlerThread
        locationHandlerThread?.quitSafely()
        locationHandlerThread = null
        locationHandler = null
        Log.i(LOG_TAG, "✅ LocationHandlerThread stopped")

        persistCoords()

        Log.i(LOG_TAG, "Finalizada la destruccion")

        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.i(LOG_TAG, "END - Task removed")

        if (timer != null) {
            timer!!.cancel()
        }
        if (timerTask != null) {
            timerTask!!.cancel()
        }
        handlerT.removeCallbacks(runnableGeo)

        // Cancel any pending getCurrentLocation request
        cancellationTokenSource?.cancel()
        cancellationTokenSource = null

        // Remove location updates and stop dedicated thread
        try {
            fusedLocationClient?.removeLocationUpdates(locationCallback!!)
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Error removing location updates: ${e.message}")
        }

        // Stop the dedicated HandlerThread
        locationHandlerThread?.quitSafely()
        locationHandlerThread = null
        locationHandler = null

        persistCoords()

        stopSelf()
    }

    internal inner class GeoListenerServiceInterface : Binder() {
        fun setCallback(
            geoListenerServiceCallback: GeoListenerServiceCallback?,
            token: String,
            dir: String?,
            country: String,
            imei: String,
            time: Int,
            center: String?,
            user: String?,
            provenance: Int,
            geoLocationType: Int
        ) {
            callback = geoListenerServiceCallback
            tokenListener = token
            dirListener = dir
            countryListener = country
            imeiListener = imei
            timeListener = time
            centerListener = center
            userListener = user
            provenanceListener = provenance
            geoLocationTypeListener = geoLocationType
            startTimer()
        }

        fun stopService() {
            Log.i(LOG_TAG, "stopService called")

            if (timer != null) {
                timer!!.cancel()
            }
            if (timerTask != null) {
                timerTask!!.cancel()
            }
            handlerT.removeCallbacks(runnableGeo)

            // Cancel any pending getCurrentLocation request
            cancellationTokenSource?.cancel()
            cancellationTokenSource = null

            // Remove location updates
            try {
                fusedLocationClient?.removeLocationUpdates(locationCallback!!)
            } catch (e: Exception) {
                Log.w(LOG_TAG, "Error removing location updates: ${e.message}")
            }

            // Stop the dedicated HandlerThread
            locationHandlerThread?.quitSafely()
            locationHandlerThread = null
            locationHandler = null

            persistCoords()

            if (serviceId != -1) {
                stopSelf(serviceId)
            }
            Log.i(LOG_TAG, "✅ Service stopped")
        }
    }

    @SuppressLint("MissingPermission")
    private fun initGPS() {
        val context = applicationContext
        Log.i(LOG_TAG + "IMEI_RECEIVE", "INICIANDO gps interval: " + timeListener)

        // Create dedicated HandlerThread for location updates (not throttled in background)
        locationHandlerThread = HandlerThread("LocationHandlerThread").apply {
            start()
        }
        locationHandler = Handler(locationHandlerThread!!.looper)
        Log.i(LOG_TAG, "✅ Dedicated LocationHandlerThread started")

        // Configure LocationRequest with fastestInterval for more frequent updates
        locationRequest = LocationRequest.create().apply {
            interval = (timeListener * 1000).toLong()
            fastestInterval = 5000L  // Accept updates as fast as every 5 seconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            smallestDisplacement = 0f  // Get updates even without movement
        }
        Log.i(LOG_TAG, "✅ LocationRequest configured: interval=${timeListener}s, fastestInterval=5s, priority=HIGH_ACCURACY")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient!!.lastLocation
            .addOnSuccessListener(OnSuccessListener { location: Location? ->
                if (location != null) {
                    this.location = location
                    latitude = location.latitude
                    longitude = location.longitude
                    Log.i(LOG_TAG + "GEO_LAT init gps", latitude.toString())
                }
            })

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                location = result.lastLocation
                if (location != null) {
                    latitude = location!!.latitude
                    longitude = location!!.longitude
                    Log.i(LOG_TAG + "GEO_LAT_UPDATE", "lat=$latitude, lng=$longitude, accuracy=${location!!.accuracy}m")
                }
            }
        }

        // Use dedicated Looper instead of MainLooper (not throttled in background)
        fusedLocationClient!!.requestLocationUpdates(
            locationRequest!!,
            locationCallback!!,
            locationHandlerThread!!.looper
        )
        Log.i(LOG_TAG, "✅ Location updates registered on dedicated thread")
    }

    @SuppressLint("MissingPermission")
    private fun saveGeoLocation(location: Location?) {
        try {
            if (location != null) {
                longitude = location.longitude
                latitude = location.latitude
                val currentTime = Calendar.getInstance().getTime()

                val geo = coords.stream()
                    .filter { geop: GeoPosittion? -> currentTime == geop!!.getDateTimeMobile() }
                    .findFirst()
                    .orElse(null)
                if (geo != null) {
                    Log.e(LOG_TAG, "Repeated geoposition, coords: " + coords.size)
                    return
                }

                coords.add(
                    GeoPosittion(
                        currentTime,
                        latitude,
                        longitude,
                        userListener,
                        centerListener,
                        provenanceListener,
                        geoLocationTypeListener
                    )
                )
                persistCoords()
                val appState = if (isAppInForeground()) "FOREGROUND" else "BACKGROUND"
                Log.i(LOG_TAG, "📍 NEW geoposition [$appState] - lat=$latitude, lng=$longitude, accuracy=${location.accuracy}m, total=${coords.size}")

                scope.launch(Dispatchers.IO) {
                    try {
                        val response = mAPIService?.sendPostGeo(
                            fullUrl = dirListener ?: "",
                            token = tokenListener ?: "",
                            country = countryListener ?: "",
                            imei = imeiListener ?: "",
                            body = coords
                        )

                        if (response != null) {
                            coords.clear()
                            persistCoords()
                            Log.i(LOG_TAG, "post OK received from API ")
                        } else {
                            Log.w(LOG_TAG, "⚠️ Empty response from API.")
                        }
                    } catch (e: retrofit2.HttpException) {
                        Log.e(LOG_TAG, "post ERROR received from API: ${e.message()}")
                    } catch (e: Exception) {
                        Log.e(LOG_TAG, "Unable to submit post to API (kept in buffer): ${e.message}")
                        persistCoords()
                    }
                }
            } else {
                Log.i(LOG_TAG, "requiriendo actualización")
                fusedLocationClient!!.requestLocationUpdates(
                    locationRequest!!,
                    locationCallback!!,
                    locationHandlerThread?.looper ?: Looper.getMainLooper()
                )
            }
        } catch (ex: Exception) {
            Log.e(LOG_TAG, "Exception saveGeoLocation: " + ex.message)
        } finally {
            Log.i(LOG_TAG, "saveGeoLocation finish")
        }
    }

    /**
     * Check if the app is in foreground or background
     */
    private fun isAppInForeground(): Boolean {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val appProcesses = activityManager.runningAppProcesses ?: return false
        val packageName = packageName
        for (appProcess in appProcesses) {
            if (appProcess.processName == packageName &&
                appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return true
            }
        }
        return false
    }

    /**
     * Get location age in milliseconds
     */
    private fun getLocationAgeMs(loc: Location?): Long {
        if (loc == null) return Long.MAX_VALUE

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SystemClock.elapsedRealtime() - (loc.elapsedRealtimeNanos / 1_000_000)
        } else {
            System.currentTimeMillis() - loc.time
        }
    }

    @SuppressLint("MissingPermission")
    private fun testGeo() {
        Log.i(LOG_TAG, "GeoTimer called")

        if (dirListener == null || dirListener!!.isEmpty()) {
            Log.w(LOG_TAG, "❌ dirListener is null or empty — skipping GeoTimer execution.")
            return
        }

        if (mAPIService == null) {
            mAPIService = getAPIService()
            if (mAPIService == null) {
                Log.e(LOG_TAG, "❌ API Service could not be initialized — skipping execution.")
                return
            }
        }

        Log.i(LOG_TAG + "IMEI_RECEIVE geo", imeiListener!!)

        // Check if current cached location is good enough
        val ageMs = getLocationAgeMs(location)
        val accuracy = location?.accuracy ?: Float.MAX_VALUE
        Log.i(LOG_TAG, "Cached location - age: ${ageMs}ms, accuracy: ${accuracy}m")

        if (location != null && ageMs < MAX_LOCATION_AGE_MS && accuracy <= MAX_ACCURACY_METERS) {
            // Cached location is fresh and accurate - use it
            Log.i(LOG_TAG, "✅ Using cached location (accurate)")
            saveGeoLocation(location)
        } else {
            // Cached location is stale or inaccurate - force fresh GPS reading
            Log.w(LOG_TAG, "⚠️ Cached location not good enough, forcing fresh GPS reading...")
            requestFreshHighAccuracyLocation()
        }
        Log.i(LOG_TAG, "GeoTimer finish")
    }

    /**
     * Force a fresh high-accuracy GPS location reading.
     * Uses getCurrentLocation() which actively turns on GPS hardware even in background.
     */
    @SuppressLint("MissingPermission")
    private fun requestFreshHighAccuracyLocation() {
        // Cancel any previous request
        cancellationTokenSource?.cancel()
        cancellationTokenSource = CancellationTokenSource()

        val currentLocationRequest = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setGranularity(Granularity.GRANULARITY_FINE)
            .setMaxUpdateAgeMillis(0) // Force fresh location, no cache
            .setDurationMillis(15000) // Wait up to 15 seconds for GPS fix
            .build()

        Log.i(LOG_TAG, "📡 Requesting fresh GPS location (getCurrentLocation)...")

        fusedLocationClient?.getCurrentLocation(
            currentLocationRequest,
            cancellationTokenSource!!.token
        )?.addOnSuccessListener { freshLocation: Location? ->
            if (freshLocation != null) {
                val freshAccuracy = freshLocation.accuracy
                Log.i(LOG_TAG, "📍 Fresh location received: accuracy=${freshAccuracy}m")

                if (freshAccuracy <= MAX_ACCURACY_METERS) {
                    // Good accuracy - save it
                    location = freshLocation
                    latitude = freshLocation.latitude
                    longitude = freshLocation.longitude
                    Log.i(LOG_TAG, "✅ Fresh location is accurate, saving...")
                    saveGeoLocation(freshLocation)
                } else {
                    // Still not accurate enough, but save with warning
                    Log.w(LOG_TAG, "⚠️ Fresh location still not ideal (${freshAccuracy}m > ${MAX_ACCURACY_METERS}m), saving anyway...")
                    location = freshLocation
                    latitude = freshLocation.latitude
                    longitude = freshLocation.longitude
                    saveGeoLocation(freshLocation)
                }
            } else {
                Log.e(LOG_TAG, "❌ getCurrentLocation returned null")
                // Fallback: save whatever we have (better than nothing)
                if (location != null) {
                    Log.w(LOG_TAG, "⚠️ Using cached location as fallback")
                    saveGeoLocation(location)
                }
            }
        }?.addOnFailureListener { e ->
            Log.e(LOG_TAG, "❌ getCurrentLocation failed: ${e.message}")
            // Fallback: save cached location
            if (location != null) {
                Log.w(LOG_TAG, "⚠️ Using cached location as fallback after failure")
                saveGeoLocation(location)
            }
        }
    }

    interface GeoListenerServiceCallback {
        fun onSuccess()
        fun activateGeo()
        fun onError(error: Int)
    }

    fun startTimer() {
        if (timer != null) {
            timer!!.cancel()
        }
        timer = Timer()
        initializeTimerTask()
        timer!!.schedule(timerTask, 0, (timeListener * 1000).toLong())
        Log.i(LOG_TAG, "Timer iniciado, interval: " + timeListener)
    }

    fun initializeTimerTask() {
        timerTask = object : TimerTask() {
            override fun run() {
                handlerT.post(runnableGeo)
            }
        }
    }

    private fun persistCoords() {
        try {
            val file = File(filesDir, GEO_BUFFER_FILE)
            val json = Gson().toJson(coords)
            val writer = FileWriter(file)
            writer.write(json)
            writer.close()
            Log.i(LOG_TAG, "✅ Coords saved locally (" + coords.size + ")")
        } catch (e: IOException) {
            Log.e(LOG_TAG, "❌ Error saving coords: " + e.message)
        }
    }

    private fun loadPersistedCoords() {
        try {
            val file = File(filesDir, GEO_BUFFER_FILE)
            if (!file.exists()) return
            val json = String(Files.readAllBytes(file.toPath()))
            val listType = object : TypeToken<ArrayList<GeoPosittion?>?>() {}.type
            coords = Gson().fromJson(json, listType)
            Log.i(LOG_TAG, "📂 Loaded persisted coords: " + coords.size)
        } catch (e: Exception) {
            Log.e(LOG_TAG, "⚠️ Error loading persisted coords: " + e.message)
        }
    }

    companion object {
        val TAG: String = GeoListenerService::class.java.getSimpleName()
        val LOG_TAG: String = "$TAG - GEO_LISTENER - "
        private const val FOREGROUND_ID = 55544433
        private const val GEO_BUFFER_FILE = "geo_buffer.json"
    }
}