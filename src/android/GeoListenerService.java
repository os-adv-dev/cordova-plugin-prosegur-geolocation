package cordova.plugin.prosegur.geolocation;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.content.pm.ServiceInfo;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GeoListenerService extends Service {

    static final String TAG = GeoListenerService.class.getSimpleName();
    static final String LOG_TAG = TAG + " - GEO_LISTENER - ";
    private int serviceId = -2;
    private APIService mAPIService;
    private GeoListenerServiceCallback callback = null;
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
    private TimerTask timerTask;
    private Timer timer;
    final Handler handlerT = new Handler();
    ArrayList<GeoPosittion> coords = new ArrayList<>();
    private double longitude;
    private double latitude;
    private Location location;
    private LocationCallback locationCallback;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private static final int foregroundId = 55544433;
    private static final String GEO_BUFFER_FILE = "geo_buffer.json";

    private final Runnable runnableGeo = this::testGeo;

    @Override
    public void onCreate() {
        super.onCreate();
        loadPersistedCoords();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(LOG_TAG, "🚀 Starting GeoListenerService");

        Notification notification = createNotification();

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                startForeground(foregroundId, notification);
            } else {
                startForeground(foregroundId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "⚠️ startForeground failed: " + e.getMessage());
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.notify(foregroundId, notification);
        }

        serviceId = startId;
        initGPS();

        return START_STICKY;
    }

    private Notification createNotification() {
        final String CHANNEL_ID = "geo_listener_channel";
        final String CHANNEL_NAME = "Geolocation Tracking";

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel existingChannel = nm.getNotificationChannel(CHANNEL_ID);
            if (existingChannel == null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription("Geolocation service running");
                nm.createNotificationChannel(channel);
            }
        }

        Notification.Builder builder = new Notification.Builder(this)
                .setContentTitle("Service is running")
                .setContentText("Service enabled")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID);
        }

        return builder.build();
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
        persistCoords();
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
        persistCoords();
        Thread.currentThread().interrupt();

        stopSelf();
    }

    class GeoListenerServiceInterface extends Binder {
        void setCallback(GeoListenerServiceCallback geoListenerServiceCallback, String token, String dir, String country, String imei, int time, String center, String user, int provenance, int geoLocationType) {
            callback = geoListenerServiceCallback;
            tokenListener = token;
            dirListener = dir;
            countryListener = country;
            imeiListener = imei;
            timeListener = time;
            centerListener = center;
            userListener = user;
            provenanceListener = provenance;
            geoLocationTypeListener = geoLocationType;
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
            persistCoords();

            if (serviceId != -1) {
                stopSelf(serviceId);
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void initGPS() {
        Context context = getApplicationContext();
        Log.i(LOG_TAG + "IMEI_RECEIVE", "INICIANDO gps interval: " + timeListener);
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(timeListener * 1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        Log.i(LOG_TAG + "GEO_LAT init gps", String.valueOf(latitude));
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
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    @SuppressLint("MissingPermission")
    protected void saveGeoLocation(Location location) {
        try {
            if (location != null) {
                longitude = location.getLongitude();
                latitude = location.getLatitude();
                Date currentTime = Calendar.getInstance().getTime();

                GeoPosittion geo = coords.stream()
                        .filter(geop -> currentTime.equals(geop.getDateTimeMobile()))
                        .findFirst()
                        .orElse(null);
                if (geo != null) {
                    Log.e(LOG_TAG, "Repeated geoposition, coords: " + coords.size());
                    return;
                }

                coords.add(new GeoPosittion(currentTime, latitude, longitude, userListener, centerListener, provenanceListener, geoLocationTypeListener));
                persistCoords();
                Log.i(LOG_TAG, "NEW geoposition added, coords: " + coords.size());

                Call<GeoPosittion> call = mAPIService.sendPostGeo(tokenListener, countryListener, imeiListener, coords);
                call.enqueue(new Callback<GeoPosittion>() {
                    @Override
                    public void onResponse(Call<GeoPosittion> call, Response<GeoPosittion> response) {
                        if (response.isSuccessful()) {
                            coords.clear();
                            persistCoords();
                            Log.i(LOG_TAG, "post OK received from API: " + response.body().getMessage());
                        } else {
                            String msg = null;
                            try {
                                msg = response.errorBody().string();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Log.i(LOG_TAG, "post ERROR received from API: " + msg);
                        }
                    }

                    @Override
                    public void onFailure(Call<GeoPosittion> call, Throwable t) {
                        Log.e(LOG_TAG, "Unable to submit post to API (kept in buffer).");
                        persistCoords(); // 🔹 mantém os dados
                    }
                });
            } else {
                Log.i(LOG_TAG, "requiriendo actualización");
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            }
        } catch (Exception ex) {
            Log.e(LOG_TAG, "Exception saveGeoLocation: " + ex.getMessage());
        } finally {
            Log.i(LOG_TAG, "saveGeoLocation finish");
        }
    }

    @SuppressLint("MissingPermission")
    private void testGeo() {
        Log.i(LOG_TAG, "GeoTimer called");

        if (dirListener == null || dirListener.isEmpty()) {
            Log.w(LOG_TAG, "❌ dirListener is null or empty — skipping GeoTimer execution.");
            return;
        }

        if (mAPIService == null) {
            mAPIService = ApiUtils.getAPIService(dirListener);
            if (mAPIService == null) {
                Log.e(LOG_TAG, "❌ API Service could not be initialized — skipping execution.");
                return;
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        Log.i(LOG_TAG + "IMEI_RECEIVE geo", imeiListener);
        saveGeoLocation(location);
        Log.i(LOG_TAG, "GeoTimer finish");
    }

    public interface GeoListenerServiceCallback {
        void onSuccess();
        void activateGeo();
        void onError(int error);
    }

    public void startTimer() {
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        initializeTimerTask();
        timer.schedule(timerTask, 0, timeListener * 1000);
        Log.i(LOG_TAG, "Timer iniciado, interval: " + timeListener);
    }

    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                handlerT.post(runnableGeo);
            }
        };
    }

    private void persistCoords() {
        try {
            File file = new File(getFilesDir(), GEO_BUFFER_FILE);
            String json = new Gson().toJson(coords);
            FileWriter writer = new FileWriter(file);
            writer.write(json);
            writer.close();
            Log.i(LOG_TAG, "✅ Coords saved locally (" + coords.size() + ")");
        } catch (IOException e) {
            Log.e(LOG_TAG, "❌ Error saving coords: " + e.getMessage());
        }
    }

    private void loadPersistedCoords() {
        try {
            File file = new File(getFilesDir(), GEO_BUFFER_FILE);
            if (!file.exists()) return;
            String json = new String(Files.readAllBytes(file.toPath()));
            Type listType = new TypeToken<ArrayList<GeoPosittion>>(){}.getType();
            coords = new Gson().fromJson(json, listType);
            Log.i(LOG_TAG, "📂 Loaded persisted coords: " + coords.size());
        } catch (Exception e) {
            Log.e(LOG_TAG, "⚠️ Error loading persisted coords: " + e.getMessage());
        }
    }
}