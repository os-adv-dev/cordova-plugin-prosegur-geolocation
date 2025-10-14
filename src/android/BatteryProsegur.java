package cordova.plugin.prosegur.battery;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import androidx.core.app.ActivityCompat; //import android.support.v4.app.ActivityCompat;
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.tasks.OnSuccessListener;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

import static android.content.Context.BIND_AUTO_CREATE;

/**
 * This class echoes a string called from JavaScript.
 */
public class BatteryProsegur extends CordovaPlugin {

    static final String TAG = BatteryProsegur.class.getSimpleName();
    static final String LOG_TAG = TAG + " - ";

    private static final String INIT_LISTENER = "init";
    private static final String CHECKING = "checkBattery";
    private static final String STOP_SERVICE = "stop";
    private static final String INIT_LISTENER_GEO = "initGeo";
    private static final String CHECKING_GEO = "checkGeo";
    private static final String STOP_SERVICE_GEO = "stopGeo";

    public static final int PARAMS_ERROR = 1001;


    private static boolean logEnable = true;
    private APIService mAPIService;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationManager locationManager;
    private double longitude;
    private double latitude;
    private Location location;
    private LocationCallback locationCallback;



    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        try {
            if (INIT_LISTENER.equals(action)) {
                final String token = args.getString(0);
                final String dir = args.getString(1);
                final String country = args.getString(2);
                final String imei = args.getString(3);
                final Integer time = args.getInt(4);
                initBatteryListener(callbackContext, token, dir, country, imei, time);

                return true;

            } else if (CHECKING.equals(action)) {
                final String token = args.getString(0);
                final String dir = args.getString(1);
                final String country = args.getString(2);
                final String imei = args.getString(3);
                validate(callbackContext, token, dir, country, imei);

                return true;

            } else if (STOP_SERVICE.equals(action)) {
                stopListenerService(callbackContext);

                return true;

            } else if (INIT_LISTENER_GEO.equals(action)) {
                final String token = args.getString(0);
                final String dir = args.getString(1);
                final String country = args.getString(2);
                final String imei = args.getString(3);
                final Integer time = args.getInt(4);
                final String center = args.getString(5);
                final String user = args.getString(6);
				final int provenance = args.getInt(7);
				final int geoLocationType = args.getInt(8);
                initGeoListener(callbackContext, token, dir, country, imei, time, center, user, provenance, geoLocationType);

                return true;

            } else if (CHECKING_GEO.equals(action)) {
                final String token = args.getString(0);
                final String dir = args.getString(1);
                final String country = args.getString(2);
                final String imei = args.getString(3);
                final String center = args.getString(4);
                final String user = args.getString(5);
				final int provenance = args.getInt(6);
				final int geoLocationType = args.getInt(7);
                validateGeo(callbackContext, token, dir, country, imei, center, user, provenance, geoLocationType);

                return true;

            } else if (STOP_SERVICE_GEO.equals(action)) {
                stopGeoListenerService(callbackContext);

                return true;

            } else {

                return false;
            }

        } catch (JSONException e) {
            pluginLog(TAG, "PARAMS ERROR");
            PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, PARAMS_ERROR);
            callbackContext.sendPluginResult(pluginResult);
            return false;
        }
    }

    private void initBatteryListener(final CallbackContext callbackContext, final String token, final String dir, final String country, final String imei, final Integer time) {

        final Context context = this.cordova.getActivity().getApplicationContext();

        final Intent serviceIntent = new Intent(context, BatteryListenerService.class);

        context.startService(serviceIntent);


        Log.i("Battery", "inicia listener bateria");

        context.bindService(
                serviceIntent,
                new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        if (BatteryListenerService.class.getName().equals(name.getClassName())) {
                            BatteryListenerService.BatteryListenerServiceInterface batteryListenerServiceInterface = (BatteryListenerService.BatteryListenerServiceInterface) service;
                            pluginLog(TAG, "Listener Service binder onServiceConnected: " + service);

                            batteryListenerServiceInterface.setCallback(new BatteryListenerService.BatteryListenerServiceCallback() {


                                @Override
                                public void onSuccess() {


                                    Log.i("BATERIA", "devuelvo OK");
                                    sendSuccessResult(callbackContext);
                                }

                                @Override
                                public void activateBattery() {


                                    Log.i("CUIDAME", "init Battery");
                                    sendSuccessResult(callbackContext);
                                }

                                @Override
                                public void onError(int error) {

                                    pluginLog(TAG, "init Battery onError: " + error);
                                    sendErrorResult(error, callbackContext);
                                }
                            }, token, dir, country, imei, time);
                            context.unbindService(this);
                            sendSuccessResult(callbackContext);

                        }
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                        pluginLog(TAG, "audio Service binder disconnected");
                    }
                },
                BIND_AUTO_CREATE);

    }



    private void stopListenerService(final CallbackContext callbackContext) {

        final Context context = this.cordova.getActivity().getApplicationContext();

        final Intent serviceIntent = new Intent(context, BatteryListenerService.class);


        context.bindService(
                serviceIntent,
                new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {

                        if (BatteryListenerService.class.getName().equals(name.getClassName())) {
                            BatteryListenerService.BatteryListenerServiceInterface batteryListenerServiceInterface = (BatteryListenerService.BatteryListenerServiceInterface) service;
                            pluginLog(TAG, "stopService Service binder onServiceConnected");

                            batteryListenerServiceInterface.stopService();
                            context.unbindService(this);
                            context.stopService(serviceIntent);

                            sendSuccessResult(callbackContext);

                        }
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                        pluginLog(TAG, "stopService Service binder disconnected");
                    }
                },
                BIND_AUTO_CREATE);

    }

    private void initGeoListener(final CallbackContext callbackContext, final String token, final String dir, final String country, final String imei, final int time, final String center, final String user, final int provenance, final int geoLocationType) {

        final Context context = this.cordova.getActivity().getApplicationContext();

        final Intent serviceIntent = new Intent(context, GeoListenerService.class);

        //context.startService(serviceIntent);
		context.startForegroundService(serviceIntent);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions();

            return;
        }

        Log.i("GeoListener", "inicia listener geoposicionamiento");

        context.bindService(
                serviceIntent,
                new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {
                        if (GeoListenerService.class.getName().equals(name.getClassName())) {
                            GeoListenerService.GeoListenerServiceInterface geoListenerServiceInterface = (GeoListenerService.GeoListenerServiceInterface) service;
                            pluginLog(TAG, "Listener Service binder onServiceConnected: " + service);

                            geoListenerServiceInterface.setCallback(new GeoListenerService.GeoListenerServiceCallback() {


                                @Override
                                public void onSuccess() {


                                    Log.i("GEO", "devuelvo OK");
                                    sendSuccessResult(callbackContext);
                                }

                                @Override
                                public void activateGeo() {


                                    Log.i("CUIDAME", "init Battery");
                                    sendSuccessResult(callbackContext);
                                }

                                @Override
                                public void onError(int error) {

                                    pluginLog(TAG, "init Battery onError: " + error);
                                    sendErrorResult(error, callbackContext);
                                }
                            }, token, dir, country, imei, time, center, user, provenance, geoLocationType);
                            context.unbindService(this);
                            sendSuccessResult(callbackContext);

                        }
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                        pluginLog(TAG, "audio Service binder disconnected");
                    }
                },
                BIND_AUTO_CREATE);

    }

    private void stopGeoListenerService(final CallbackContext callbackContext) {

        final Context context = this.cordova.getActivity().getApplicationContext();

        final Intent serviceIntent = new Intent(context, GeoListenerService.class);


        context.bindService(
                serviceIntent,
                new ServiceConnection() {
                    @Override
                    public void onServiceConnected(ComponentName name, IBinder service) {

                        if (GeoListenerService.class.getName().equals(name.getClassName())) {
                            GeoListenerService.GeoListenerServiceInterface geoListenerServiceInterface = (GeoListenerService.GeoListenerServiceInterface) service;
                            pluginLog(TAG, "stopService Service binder onServiceConnected");

                            geoListenerServiceInterface.stopService();
                            context.unbindService(this);
                            context.stopService(serviceIntent);

                            sendSuccessResult(callbackContext);

                        }
                    }

                    @Override
                    public void onServiceDisconnected(ComponentName name) {
                        pluginLog(TAG, "stopService Service binder disconnected");
                    }
                },
                BIND_AUTO_CREATE);

    }

    private void validate(final CallbackContext callbackContext, final String token, final String dir, final String country, final String imei) {
        mAPIService = ApiUtils.getAPIService(dir);
        Log.i("IMEI_RECEIVE", imei);
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Context context = cordova.getActivity().getApplication().getApplicationContext();
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        //int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        //float batteryPct = level / (float)scale;




        Call<BatteryLevel> call = mAPIService.sendPost(token, new BatteryLevel(country, imei, level));

        call.enqueue(new Callback<BatteryLevel>() {
            @Override
            public void onResponse(Call<BatteryLevel> call, Response<BatteryLevel> response) {

                if (response.isSuccessful()) {
                    PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
                    pluginResult.setKeepCallback(true);
                    callbackContext.sendPluginResult(pluginResult);
                    Log.i(TAG, "post OK received from API." + response.body().getMessage());
                } else {
                    String msg = null;
                    try {
                        msg = response.errorBody().string();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.i(TAG, "post ERROR received from API." + msg);
                }


            }

            @Override
            public void onFailure(Call<BatteryLevel> call, Throwable t) {
                Log.e(TAG, "Unable to submit post to API.");
            }
        });


    }


    private void requestPermissions(){
        ActivityCompat.requestPermissions(
                this.cordova.getActivity(),
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                44
        );
    }

    private void validateGeo(final CallbackContext callbackContext, final String token, final String dir, final String country, final String imei, final String center, final String user, final int provenance, final int geoLocationType) {
        mAPIService = ApiUtils.getAPIService(dir);
        Log.i("IMEI_RECEIVE", imei);
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
         //       .addLocationRequest(locationRequest);

        Context context = this.cordova.getActivity().getApplicationContext();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
       // locationManager = (LocationManager) cordova.getActivity().getSystemService(Context.LOCATION_SERVICE);



        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(cordova.getActivity(), new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                            Log.i("GEO_LAT", String.valueOf(latitude));
                        }
                    }
                });

        locationCallback = new LocationCallback() {

            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                location = locationResult.getLastLocation();

                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    Log.i("GEO_LAT0", String.valueOf(latitude));

            }
        };



        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions();

            return;
        }

        if(location != null) {
            longitude = location.getLongitude();
            latitude = location.getLatitude();
            Date currentTime = Calendar.getInstance().getTime();
            Log.i("GEO_LAT", String.valueOf(latitude));

            ArrayList<GeoPosittion> coords = new ArrayList<GeoPosittion>();
            coords.add(new GeoPosittion(currentTime, latitude, longitude, user, center, provenance, geoLocationType));
            Call<GeoPosittion> call = mAPIService.sendPostGeo(token, country, imei, coords);

            call.enqueue(new Callback<GeoPosittion>() {
                @Override
                public void onResponse(Call<GeoPosittion> call, Response<GeoPosittion> response) {

                    if (response.isSuccessful()) {

                        Log.i(TAG, "post OK received from API." + response.body().getMessage());
                        // Log.i(TAG, "post OK received from API." + response.message());
                    } else {
                        String msg = null;
                        try {
                            msg = response.errorBody().string();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Log.i(TAG, "post ERROR received from API." + msg);
                        //Log.i(TAG, "post ERROR received from API." + response.message());
                    }
                }

                @Override
                public void onFailure(Call<GeoPosittion> call, Throwable t) {
                    Log.e(TAG, "Unable to submit post to API.");
                }
            });
        }else{
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.getMainLooper());

        }


    }





    public static void pluginLog(String tag, String description) {

        if (logEnable) Log.d(LOG_TAG + tag, description);

    }

    private void sendSuccessResult(int level, CallbackContext callbackContext) {
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, level);
        pluginResult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginResult);
    }

    private void sendSuccessResult(CallbackContext callbackContext) {
        PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
        pluginResult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginResult);
    }

    private void sendErrorResult(int error, CallbackContext callbackContext) {
        PluginResult pluginResult = new PluginResult(PluginResult.Status.ERROR, error);
        pluginResult.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginResult);
    }



}
