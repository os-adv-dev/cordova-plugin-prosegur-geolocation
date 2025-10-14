package cordova.plugin.prosegur.battery;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.Manifest.permission;
import android.content.pm.ServiceInfo;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.location.LocationManager.GPS_PROVIDER;


public class GeoListenerService extends Service {

    static final String TAG = GeoListenerService.class.getSimpleName();
    static final String LOG_TAG = TAG + " - GEO_LISTENER - ";

    private int serviceId = -2;

    private GeoListenerServiceCallback callback = null;
    private APIService mAPIService;
    private GeoListenerServiceInterface geoListenerServiceInterface = new GeoListenerServiceInterface();


    private String tokenListener;
    private String dirListener;
    private String countryListener;
    private String imeiListener;
    private String userListener;
    private String centerListener;
	private int provenanceListener;
	private int geoLocationTypeListener;
    private Integer timeListener = 300;

    private boolean aborted = false;
    private boolean listening = false;
    private String lang;
    private String keyWord;
    private TimerTask timerTask;
    private Timer timer;
    final Handler handlerT = new Handler();
    private Handler mHandler = new Handler();
    private long time = System.currentTimeMillis();
    ArrayList<GeoPosittion> coords = new ArrayList<GeoPosittion>();

    private int PERMISSION_ID = 1;



    private AudioManager mAudioManager;
    private int mStreamVolume = 0;

    private LocationManager locationManager;
    private double longitude;
    private double latitude;
    private Location location;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
	
	private Notification notification;
	private static final int foregroundId = 55544433; //Unique for each Service


    private Runnable runnableGeo = new Runnable() {
        public void run() {
            long result = System.currentTimeMillis() - time;

            testGeo();

        }
    };


    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
		
		notification = createNotification();
		
		//Log.i(LOG_TAG + "ANDROID", "SDK_INT: " + Build.VERSION.SDK_INT);
		//Log.i(LOG_TAG + "ANDROID", "TIRAMISU: " + Build.VERSION_CODES.TIRAMISU);
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
			Log.i(LOG_TAG + "ANDROID", "Menor que tiramisú: " + Build.VERSION_CODES.TIRAMISU);
			startForeground(foregroundId, notification);
		} else {
			Log.i(LOG_TAG + "ANDROID", "Mayor que tiramisú: " + Build.VERSION_CODES.TIRAMISU);
			startForeground(foregroundId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
		}
		
        serviceId = startId;
        initGPS();

        super.onStartCommand(intent, flags, startId);
		return Service.START_STICKY;
    }
	
	private Notification createNotification() {
		final String CHANNELID = "Foreground Service ID";
		NotificationChannel channel = new NotificationChannel(
             CHANNELID,
             CHANNELID,
             NotificationManager.IMPORTANCE_LOW
		);

		getSystemService(NotificationManager.class).createNotificationChannel(channel);
		Notification.Builder notification = new Notification.Builder(this, CHANNELID)
             .setContentText("Service is running")
             .setContentTitle("Service enabled");
             //.setSmallIcon(R.drawable.ic_launcher_background);
			 
		return notification.build();
	}

    @Override
    public IBinder onBind(Intent intent) {
        return geoListenerServiceInterface;
    }

    @Override
    public void onDestroy() {
        Log.i(LOG_TAG, "Destruyendo aplicacion");

        if (timer != null) {
            timer.cancel();
        }
        if (timerTask != null) {
            timerTask.cancel();
        }
        if (handlerT != null) {
            handlerT.removeCallbacks(runnableGeo);
        }
        Thread.currentThread().interrupt();

        Log.i(LOG_TAG, "Finalizada la destruccion");

        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.i(LOG_TAG, "END");

        if (timer != null) {
            timer.cancel();
        }
        if (timerTask != null) {
            timerTask.cancel();
        }
        if (handlerT != null) {
            handlerT.removeCallbacks(runnableGeo);
        }
        Thread.currentThread().interrupt();

        stopSelf();
    }


    class GeoListenerServiceInterface extends Binder {
        void setCallback(GeoListenerServiceCallback geoListenerServiceCallback, String token, String dir, String country, String imei, int time, String center, String user, int provenance, int geoLocationType) {
            callback = geoListenerServiceCallback;
            //callback.onSuccess();
            tokenListener = token;
            dirListener = dir;
            countryListener = country;
            imeiListener = imei;
            timeListener = time;
            centerListener = center;
            userListener = user;
			provenanceListener = provenance;
			geoLocationTypeListener = geoLocationType;

            //  testGeo();
            startTimer();

        }


        void stopService() {

            if (timer != null) {
                timer.cancel();
            }
            if (timerTask != null) {
                timerTask.cancel();
            }
            handlerT.removeCallbacks(runnableGeo);

            if (serviceId != -1) {
                stopSelf(serviceId);
            }
        }
    }

    private void initGPS(){

        Context context = getApplicationContext();
        //  Intent geoStatus = context.registerReceiver(null, ifilter);
        Log.i(LOG_TAG + "IMEI_RECEIVE", "INICIANDO gps interval: " + timeListener);
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(timeListener*1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        // locationManager = (LocationManager) cordova.getActivity().getSystemService(Context.LOCATION_SERVICE);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener( new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                            Log.i(LOG_TAG + "GEO_LAT init gps", String.valueOf(latitude));
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
                Log.i(LOG_TAG + "GEO_LAT0", String.valueOf(latitude));
				
				//saveGeoLocation(location);

            }
        };
		
		fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());

    }
	
	protected void saveGeoLocation(Location location) {
	try {
		if(location != null) {
            longitude = location.getLongitude();
            latitude = location.getLatitude();
            Date currentTime = Calendar.getInstance().getTime();

            //Añade al buffer
            //ArrayList<GeoPosittion> coords = new ArrayList<GeoPosittion>();
			//If exists, not add again
			GeoPosittion geo = coords.stream().filter(geop -> currentTime.equals(geop.getDateTimeMobile()))
            .findFirst().orElse(null);
			if (geo != null) {
				Log.e(LOG_TAG, "Repeated geopositio, coords: " + coords.size());
				return;
			}
			
            coords.add(new GeoPosittion(currentTime, latitude, longitude, userListener, centerListener, provenanceListener, geoLocationTypeListener));
			Log.i(LOG_TAG, "NEW geoposition added, coords: " + coords.size());
            Call<GeoPosittion> call = mAPIService.sendPostGeo(tokenListener,countryListener,imeiListener,coords);

            call.enqueue(new Callback<GeoPosittion>() {
                @Override
                public void onResponse(Call<GeoPosittion> call, Response<GeoPosittion> response) {

                    if (response.isSuccessful()) {
						coords.clear(); //limpia el buffer
                        Log.i(LOG_TAG, "post OK received from API: " + response.body().getMessage());
                        // Log.i(TAG, "post OK received from API: " + response.message());
                    } else {
                        String msg = null;
                        try {
                            msg = response.errorBody().string();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Log.i(LOG_TAG, "post ERROR received from API: " + msg);
                        //Log.i(LOG_TAG, "post ERROR received from API: " + response.message());
                    }
                }

                @Override
                public void onFailure(Call<GeoPosittion> call, Throwable t) {
                    Log.e(LOG_TAG, "Unable to submit post to API.");
                }
            });
        }else{
            Log.i(LOG_TAG, "requiriendo actualización");
            fusedLocationClient.requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.getMainLooper());

        }
	} catch(Exception ex){
		Log.e(LOG_TAG, "Exception saveGeoLocation: " + ex.getMessage());
	} finally {
		Log.i(LOG_TAG, "saveGeoLocation finish");
	}
	}

    @SuppressLint("MissingPermission")
    private void testGeo() {
		
		Log.i(LOG_TAG, "GeoTimer called");
		
		fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

        mAPIService = ApiUtils.getAPIService(dirListener);
        Log.i(LOG_TAG + "IMEI_RECEIVE geo", imeiListener);
		//IntentFilter ifilter = new IntentFilter(Intent.ACTION_LOCALE_CHANGED);
        //Context context = getApplicationContext();
		//Intent geoStatus = context.registerReceiver(null, ifilter
		
        //LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		//@SuppressLint("MissingPermission") Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

		saveGeoLocation(location);
        
		Log.i(LOG_TAG, "GeoTimer finish");
    }



    public interface GeoListenerServiceCallback {


        void onSuccess();

        void activateGeo();

        void onError(int error);

    }


    public void startTimer() {

        if(timer != null){
            timer.cancel();
        }
        //set a new Timer
        timer = new Timer();

        //initialize the TimerTask's job
        initializeTimerTask();

        //schedule the timer, after the first 5000ms the TimerTask will run every 10000ms
        timer.schedule(timerTask, 0, timeListener*1000);
		
		Log.i(LOG_TAG, "Timer iniciado, interval: " + timeListener);
    }

    public void initializeTimerTask() {

        timerTask = new TimerTask() {
            public void run() {

                //use a handler to run a toast that shows the current timestamp
                handlerT.post(runnableGeo);
            }
        };
    }
}
