package cordova.plugin.prosegur.geolocation;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.StrictMode;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;



public class BatteryListenerService extends Service {

    static final String TAG = BatteryListenerService.class.getSimpleName();
    static final String LOG_TAG = TAG + " - ";

    private int serviceId = -1;

    private BatteryListenerServiceCallback callback = null;
    public APIService mAPIService;
    private BatteryListenerServiceInterface batteryListenerServiceInterface = new BatteryListenerServiceInterface();

    /*****************************************************SPEECH****************************************************/

    public String tokenListener;
    public String dirListener;
    public String countryListener;
    public String imeiListener;
    private Integer timeListener = 1800;

    private boolean aborted = false;

    private TimerTask timerTask;
    private Timer timer;
    final Handler handlerT = new Handler();
    private Handler mHandler = new Handler();
    private long time = System.currentTimeMillis();


    private Runnable runnableBattery =new Runnable() {
        public void run() {
            long result = System.currentTimeMillis() - time;
            testBattery();

        }
    };



    @Override
    public void onCreate() {
        super.onCreate();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        serviceId = startId;

        final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        //
        // Intent batteryStatus =  getBaseContext().registerReceiver(null, ifilter);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return batteryListenerServiceInterface;
    }



    private class MyThread implements Runnable {
        @Override
        public void run() {
            try {
                // BatteryLevel tasks = call.execute().body();
                int level = 59;
                BatteryLevel tasks = ApiUtils.getAPIService(dirListener).sendPostOff(tokenListener, new BatteryLevel(countryListener, imeiListener, level)).execute().body();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {


        Log.i("Battery","Destruyendo aplicacion");
        //mAPIService = ApiUtils.getAPIService(dirListener);
        Log.i("Battery","dirListener:"+dirListener);
        Log.i("Battery","tokenListener:"+tokenListener);
        Log.i("Battery","imeiListener:"+imeiListener);
        Log.i("Battery","mAPIService:"+mAPIService);

        int level = 39;

        //Call<BatteryLevel> call= mAPIService.sendPostOff(tokenListener,new BatteryLevel(countryListener, imeiListener, level));
        Log.i("Battery","Lanzando REST de Destroy");
        Log.i("Battery","level:"+level);
       /*APIService taskService = ApiUtils.getAPIService(dirListener);
        Call<BatteryLevel> call = taskService.sendPostOff(tokenListener,new BatteryLevel(countryListener, imeiListener, level));*/


       // new Thread(new MyThread()).start();
        try {
            // BatteryLevel tasks = call.execute().body();
            BatteryLevel tasks = ApiUtils.getAPIService(dirListener).sendPostOff(tokenListener, new BatteryLevel(countryListener, imeiListener, level)).execute().body();
        } catch (IOException e) {
            e.printStackTrace();
        }

       /* mAPIService.sendPostOff(tokenListener,new BatteryLevel(countryListener, imeiListener, level)).enqueue(new Callback<BatteryLevel>() {
            @Override
            public void onResponse(Call<BatteryLevel> call, Response<BatteryLevel> response) {
                Log.i("Battery","Respuesta REST de Destroy");
                if(response.isSuccessful()) {

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
            public void onFailure(Call<BatteryLevel> call, Throwable t) {
                Log.e("Battery", "Unable to submit post to API.");
                Log.e("Battery", t.getMessage());
            }
        });*/

        stop(true);
        if(timer != null) {
            timer.cancel();
        }
        if(timerTask != null) {
            timerTask.cancel();
        }
        if(handlerT != null) {
            handlerT.removeCallbacks(runnableBattery);
        }
        Thread.currentThread().interrupt();

        /*Intent restartService = new Intent(getApplicationContext(),this.getClass());
        PendingIntent pendingIntent = PendingIntent.getService(getApplicationContext(),1,restartService,PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME,5000,pendingIntent);*/

        Log.i("Battery","Finalizada la destruccion");
        super.onDestroy();

    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {

        Log.i("Battery","TaskRemoved");
        //mAPIService = ApiUtils.getAPIService(dirListener);
        Log.i("Battery","dirListener:"+dirListener);
        Log.i("Battery","tokenListener:"+tokenListener);
        Log.i("Battery","imeiListener:"+imeiListener);
        Log.i("Battery","mAPIService:"+mAPIService);
        Log.i("Battery", "END");
        //Call<BatteryLevel> call= mAPIService.sendPostOff(tokenListener, imeiListener);
        int level = 49;

        //Call<BatteryLevel> call= mAPIService.sendPostOff(tokenListener,new BatteryLevel(countryListener, imeiListener, level));
        Log.i("Battery","Lanzando REST de Taskremoved");
        Log.i("Battery","level:"+level);

        /*APIService taskService = ApiUtils.getAPIService(dirListener);
        Call<BatteryLevel> call = taskService.sendPostOff(tokenListener,new BatteryLevel(countryListener, imeiListener, level));*/

        //new Thread(new MyThread()).start();

        try {
            // BatteryLevel tasks = call.execute().body();
            //int level = 59;
            BatteryLevel tasks = ApiUtils.getAPIService(dirListener).sendPostOff(tokenListener, new BatteryLevel(countryListener, imeiListener, level)).execute().body();
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*mAPIService.sendPostOff(tokenListener,new BatteryLevel(countryListener, imeiListener, level)).enqueue(new Callback<BatteryLevel>() {
            @Override
            public void onResponse(Call<BatteryLevel> call, Response<BatteryLevel> response) {
                Log.i("Battery","Respuesta REST de Destroy");
                if(response.isSuccessful()) {

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
            public void onFailure(Call<BatteryLevel> call, Throwable t) {
                Log.e("Battery", "Unable to submit post to API.");
                Log.e("Battery", t.getMessage());
            }
        });*/

        stop(true);
        if(timer != null) {
            timer.cancel();
        }
        if(timerTask != null) {
            timerTask.cancel();
        }
        if(handlerT != null) {
            handlerT.removeCallbacks(runnableBattery);
        }
        Thread.currentThread().interrupt();

        stopSelf();
        super.onTaskRemoved(rootIntent);
    }

    class BatteryListenerServiceInterface extends Binder {
        void setCallback(BatteryListenerServiceCallback batteryListenerServiceCallback, String token, String dir, String country, String imei, Integer time) {
            callback = batteryListenerServiceCallback;
            //callback.onSuccess();
            tokenListener = token;
            dirListener = dir;
            countryListener = country;
            imeiListener = imei;
            timeListener = time;

          //  testBattery();
            startTimer();

        }


        void stopService() {
            stop(true);
            if(timer != null) {
                timer.cancel();
            }
            if(timerTask != null) {
                timerTask.cancel();
            }
            handlerT.removeCallbacks(runnableBattery);

            if(serviceId != -1) {
                stopSelf(serviceId);
            }
        }


    }

    private void testBattery() {

        mAPIService = ApiUtils.getAPIService(dirListener);
        Log.i("IMEI_RECEIVE", imeiListener);
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Context context = getApplicationContext();
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);


        Call<BatteryLevel> call= mAPIService.sendPost(tokenListener,new BatteryLevel(countryListener, imeiListener, level));

        call.enqueue(new Callback<BatteryLevel>() {
            @Override
            public void onResponse(Call<BatteryLevel> call, Response<BatteryLevel> response) {

                if(response.isSuccessful()) {

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
            public void onFailure(Call<BatteryLevel> call, Throwable t) {
                Log.e(TAG, "Unable to submit post to API.");
            }
        });


    }


    public interface BatteryListenerServiceCallback {


        void onSuccess();

        void activateBattery();

        void onError(int error);

    }



    private void stop(boolean abort) {
        this.aborted = abort;
        /*Handler loopHandler = new Handler(Looper.getMainLooper());
        loopHandler.post(new Runnable() {

            @Override
            public void run() {
                if(recognizer != null) {
                    recognizer.stopListening();
                }
            }

        });*/
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
        timer.schedule(timerTask, 0, timeListener*1000); //
    }

    public void initializeTimerTask() {

        timerTask = new TimerTask() {
            public void run() {

                //use a handler to run a toast that shows the current timestamp
                handlerT.post(runnableBattery);
            }
        };
    }



}
