package com.aseemsethi.mylocation;

import static java.lang.Thread.sleep;

import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStore;
import androidx.work.ListenableWorker;
import androidx.work.Worker;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.telecom.Call;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import androidx.work.impl.utils.futures.SettableFuture;

import com.aseemsethi.mylocation.ui.main.PageViewModel;
import com.google.android.gms.common.api.Response;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;

import javax.security.auth.callback.Callback;

/**
 * Created on : Mar 26, 2019
 * Author     : AndroidWave
 */
public class NotificationWorker extends ListenableWorker {
    private static final String WORK_RESULT = "MyLocation work";
    private static final String TAG = "MyLocation Worker";

    public NotificationWorker(@NonNull Context context,
                              @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    interface MyCallback{
        void onFailure();
        void onResponse(Data oData);
    }

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        return CallbackToFutureAdapter.getFuture(completer -> {
            final Data[] out = new Data[1];
            MyCallback callback = new MyCallback() {
                public void onFailure() {
                    Log.d(TAG, "Listenable startWork failure");
                    completer.set(Result.failure());
                }
                //@Override
                public void onResponse(Data oData) {
                    Log.d(TAG, "Listenable startWork success");
                    completer.set(Result.success(oData));
                    out[0] = oData;
                }
            };
            //xyz(completer, callback);
            SingleShotLocationProvider.requestSingleUpdate(getApplicationContext(),
                    new SingleShotLocationProvider.LocationCallback() {
                        @Override
                        public void onNewLocationAvailable(
                                SingleShotLocationProvider.GPSCoordinates loc) {
                            Log.d(TAG, "onNewLocationAvailable");
                            Data taskData = getInputData();
                            String taskDataString = taskData.getString(MainActivity.MESSAGE_STATUS);
                            SingleShotLocationProvider.GPSCoordinates location = loc;
                            Log.d(TAG, "getLocation() LAT: " + location.latitude +
                                    ", LON: " + location.longitude + ": " + taskDataString);
                            //String l = Double.toString(location.latitude);
                            Data oData = new Data.Builder()
                                    .putFloat("LAT", location.latitude)
                                    .putFloat("LON", location.longitude)
                                    .build();
                            completer.set(Result.success(oData));
                            callback.onResponse(oData);
                            sendLocToService(location);
                        }
                    });
            return ListenableWorker.Result.success(out[0]);
            //return callback;
        });
    }
    public void xyz(CallbackToFutureAdapter.Completer completer, MyCallback callback) {
        Log.d(TAG, "Listenable startWork...");
        Data input = getInputData();
        Context context = getApplicationContext();
        SingleShotLocationProvider.requestSingleUpdate(context,
                new SingleShotLocationProvider.LocationCallback() {
                    @Override
                    public void onNewLocationAvailable(SingleShotLocationProvider.GPSCoordinates loc) {
                        Log.d(TAG, "onNewLocationAvailable");
                        Data taskData = getInputData();
                        String taskDataString = taskData.getString(MainActivity.MESSAGE_STATUS);
                        SingleShotLocationProvider.GPSCoordinates location = loc;
                        Log.d(TAG, "getLocation() LAT: " + location.latitude +
                                ", LON: " + location.longitude + ": " + taskDataString);
                        //String l = Double.toString(location.latitude);
                        Data oData = new Data.Builder()
                                .putFloat("LAT", location.latitude)
                                .putFloat("LON", location.longitude)
                                .build();
                        completer.set(Result.success(oData));
                        callback.onResponse(oData);
                        sendLocToService(location);
                    }
                });
    }

    public void sendLocToService(SingleShotLocationProvider.GPSCoordinates location) {
        Intent serviceIntent = new Intent(getApplicationContext(),
                myMqttService.class);
        serviceIntent.setAction(myMqttService.MQTT_SEND_LOC);
        serviceIntent.putExtra("lat", location.latitude);
        serviceIntent.putExtra("lon", location.longitude);
        getApplicationContext().startService(serviceIntent);
    }
    /*
    @NonNull
    public Result doWork() {
        Log.d(TAG, "doWork called");
        Data taskData = getInputData();
        String taskDataString = taskData.getString(MainActivity.MESSAGE_STATUS);
        showNotification("WorkManager", taskDataString != null ? taskDataString : "Message has been Sent");
        Data outputData = new Data.Builder().putString(WORK_RESULT, "Jobs Finished").build();
        return Result.success(outputData);
    }
     */

    private void showNotification(String task, String desc) {
        Log.d(TAG, "show notif");
        NotificationManager manager = (NotificationManager) getApplicationContext().
                getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "task_channel";
        String channelName = "task_name";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "SDK_INT > Build");
            NotificationChannel channel = new
                    NotificationChannel(channelId, channelName,
                    NotificationManager.IMPORTANCE_LOW); // meaqns no sound
            manager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(getApplicationContext(), channelId)
                //.setContentTitle(task)
                .setContentText(desc).setSound(null)
                .setSmallIcon(R.mipmap.ic_launcher);
        manager.notify(1, builder.build());
    }
}
