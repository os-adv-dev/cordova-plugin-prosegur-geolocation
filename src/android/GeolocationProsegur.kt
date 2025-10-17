package cordova.plugin.prosegur.geolocation

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import cordova.plugin.prosegur.geolocation.GeoListenerService.GeoListenerServiceCallback
import cordova.plugin.prosegur.geolocation.GeoListenerService.GeoListenerServiceInterface
import org.apache.cordova.CallbackContext
import org.apache.cordova.CordovaPlugin
import org.apache.cordova.PluginResult
import org.json.JSONArray
import org.json.JSONException

/**
 * This class echoes a string called from JavaScript.
 */
class GeolocationProsegur : CordovaPlugin() {


    override fun execute(
        action: String?,
        args: JSONArray,
        callbackContext: CallbackContext
    ): Boolean {
        try {
            if (INIT_LISTENER_GEO == action) {
                val token = args.getString(0)
                val dir = args.getString(1)
                val country = args.getString(2)
                val imei = args.getString(3)
                val time = args.getInt(4)
                val center = args.getString(5)
                val user = args.getString(6)
                val provenance = args.getInt(7)
                val geoLocationType = args.getInt(8)
                initGeoListener(
                    callbackContext,
                    token,
                    dir,
                    country,
                    imei,
                    time,
                    center,
                    user,
                    provenance,
                    geoLocationType
                )
                return true
            } else if (STOP_SERVICE_GEO == action) {
                stopGeoListenerService(callbackContext)
                return true
            } else {
                return false
            }
        } catch (e: JSONException) {
            pluginLog(TAG, "PARAMS ERROR: " + e.message)
            val pluginResult = PluginResult(PluginResult.Status.ERROR, PARAMS_ERROR)
            callbackContext.sendPluginResult(pluginResult)
            return false
        }
    }

    private fun initGeoListener(
        callbackContext: CallbackContext,
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
        val context = this.cordova.getActivity().applicationContext
        val serviceIntent = Intent(context, GeoListenerService::class.java)
        context.startForegroundService(serviceIntent)

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions()
            return
        }

        Log.i("GeoListener", "inicia listener geoposicionamiento")

        context.bindService(
            serviceIntent,
            object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, service: IBinder?) {
                    if (GeoListenerService::class.java.name == name.className) {
                        val geoListenerServiceInterface = service as GeoListenerServiceInterface
                        pluginLog(TAG, "Listener Service binder onServiceConnected: $service")

                        geoListenerServiceInterface.setCallback(
                            object : GeoListenerServiceCallback {
                                override fun onSuccess() {
                                    Log.i("GEO", "devuelvo OK")
                                    sendSuccessResult(callbackContext)
                                }

                                override fun activateGeo() {
                                    Log.i("CUIDAME", "init")
                                    sendSuccessResult(callbackContext)
                                }

                                override fun onError(error: Int) {
                                    pluginLog(TAG, "init onError: $error")
                                    sendErrorResult(error, callbackContext)
                                }
                            },
                            token,
                            dir,
                            country,
                            imei,
                            time,
                            center,
                            user,
                            provenance,
                            geoLocationType
                        )
                        context.unbindService(this)
                        sendSuccessResult(callbackContext)
                    }
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    pluginLog(TAG, "audio Service binder disconnected")
                }
            },
            Context.BIND_AUTO_CREATE
        )
    }

    private fun stopGeoListenerService(callbackContext: CallbackContext) {
        val context = this.cordova.getActivity().applicationContext
        val serviceIntent = Intent(context, GeoListenerService::class.java)

        context.bindService(
            serviceIntent,
            object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, service: IBinder?) {
                    if (GeoListenerService::class.java.name == name.className) {
                        val geoListenerServiceInterface = service as GeoListenerServiceInterface
                        pluginLog(TAG, "stopService Service binder onServiceConnected")

                        geoListenerServiceInterface.stopService()
                        context.unbindService(this)
                        context.stopService(serviceIntent)

                        sendSuccessResult(callbackContext)
                    }
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    pluginLog(TAG, "stopService Service binder disconnected")
                }
            },
            Context.BIND_AUTO_CREATE
        )
    }

    private fun requestPermissions() {
        val permissionsList = ArrayList<String?>()

        permissionsList.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        permissionsList.add(Manifest.permission.ACCESS_FINE_LOCATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+
            permissionsList.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsList.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        ActivityCompat.requestPermissions(
            this.cordova.getActivity(),
            permissionsList.toTypedArray<String?>(),
            44
        )
    }

    private fun sendSuccessResult(callbackContext: CallbackContext) {
        val pluginResult = PluginResult(PluginResult.Status.OK)
        pluginResult.keepCallback = true
        callbackContext.sendPluginResult(pluginResult)
    }

    private fun sendErrorResult(error: Int, callbackContext: CallbackContext) {
        val pluginResult = PluginResult(PluginResult.Status.ERROR, error)
        pluginResult.keepCallback = true
        callbackContext.sendPluginResult(pluginResult)
    }

    companion object {
        val TAG: String = GeolocationProsegur::class.java.getSimpleName()
        val LOG_TAG: String = "$TAG - "
        private const val INIT_LISTENER_GEO = "initGeo"
        private const val STOP_SERVICE_GEO = "stopGeo"
        const val PARAMS_ERROR: Int = 1001
        private const val LOG_ENABLE = true

        fun pluginLog(tag: String?, description: String) {
            if (LOG_ENABLE) Log.d(LOG_TAG + tag, description)
        }
    }
}