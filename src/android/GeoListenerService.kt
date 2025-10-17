package cordova.plugin.prosegur.geolocation

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
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
    private val handlerT: Handler = Handler()
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
        if (handlerT != null) {
            handlerT.removeCallbacks(runnableGeo)
        }
        persistCoords()
        Thread.currentThread().interrupt()

        Log.i(LOG_TAG, "Finalizada la destruccion")

        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.i(LOG_TAG, "END")

        if (timer != null) {
            timer!!.cancel()
        }
        if (timerTask != null) {
            timerTask!!.cancel()
        }
        if (handlerT != null) {
            handlerT.removeCallbacks(runnableGeo)
        }
        persistCoords()
        Thread.currentThread().interrupt()

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
            if (timer != null) {
                timer!!.cancel()
            }
            if (timerTask != null) {
                timerTask!!.cancel()
            }
            handlerT.removeCallbacks(runnableGeo)
            persistCoords()

            if (serviceId != -1) {
                stopSelf(serviceId)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun initGPS() {
        val context = applicationContext
        Log.i(LOG_TAG + "IMEI_RECEIVE", "INICIANDO gps interval: " + timeListener)
        locationRequest = LocationRequest.create()
        locationRequest!!.interval = (timeListener * 1000).toLong()
        locationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        fusedLocationClient!!.lastLocation
            .addOnSuccessListener(OnSuccessListener { location: Location? ->
                if (location != null) {
                    latitude = location.latitude
                    longitude = location.longitude
                    Log.i(LOG_TAG + "GEO_LAT init gps", latitude.toString())
                }
            })

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                location = result.lastLocation
                latitude = location!!.latitude
                longitude = location!!.longitude
                Log.i(LOG_TAG + "GEO_LAT0", latitude.toString())
            }
        }

        fusedLocationClient!!.requestLocationUpdates(
            locationRequest!!,
            locationCallback!!,
            Looper.getMainLooper()
        )
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
                Log.i(LOG_TAG, "NEW geoposition added, coords: " + coords.size)

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
                    Looper.getMainLooper()
                )
            }
        } catch (ex: Exception) {
            Log.e(LOG_TAG, "Exception saveGeoLocation: " + ex.message)
        } finally {
            Log.i(LOG_TAG, "saveGeoLocation finish")
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

        fusedLocationClient!!.requestLocationUpdates(
            locationRequest!!,
            locationCallback!!,
            Looper.getMainLooper()
        )
        Log.i(LOG_TAG + "IMEI_RECEIVE geo", imeiListener!!)
        saveGeoLocation(location)
        Log.i(LOG_TAG, "GeoTimer finish")
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