package com.aseemsethi.mylocation;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import kotlin.random.URandomKt;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE;
import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;

/*
In manifest file, ensure that the service name starts with lower case -
If the name assigned to this attribute begins with a colon (':'), a new process,
private to the application, is created when it's needed and the service runs in
that process. If the process name begins with a lowercase character, the service
will run in a global process of that name, provided that it has permission to
do so. This allows components in different applications to share a process,
reducing resource usage.
*/
public class myMqttService extends Service {
    final String TAG = "MyLocation: MQTT";
    String CHANNEL_ID = "default";
    //String CHANNEL_URG = "urgent";
    NotificationManager mNotificationManager;
    Notification notification;
    int incr = 100;
    int counter = 1;
    MqttHelper mqttHelper;
    final static String MQTTSUBSCRIBE_ACTION = "MQTTSUBSCRIBE_ACTION";
    final static String MQTT_SEND_LOC = "MQTT_SEND_LOC";
    public final static String MQTT_SEND_NAME = "MQTT_SEND_NAME";
    boolean running = false;
    String name = null;
    String role;
    String lineSeparator;
    String topic = null;
    String filename = "mylocation.txt";

    public myMqttService() {
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = null;
        Log.d(TAG, "onStartCommand mqttService");
        lineSeparator = System.getProperty("line.separator");
        if (intent == null) {
            Log.d(TAG, "Intent is null..possible due to app restart");
            //return START_STICKY;
            //action = MQTTMSG_ACTION;
            //return android.app.Service.START_REDELIVER_INTENT;
        }
        action = intent.getAction();
        Log.d(TAG, "ACTION: " + action);
        if (action == "MQTTSUBSCRIBE_ACTION") {
            Bundle extras = intent.getExtras();
            if(extras == null) {
                Log.d(TAG,"null MQTTSUBSCRIBE_ACTION");
            } else {
                topic = extras.getString("topic");
                role = extras.getString("role");
                name = extras.getString("name");
                this.deleteFile("svcdata.txt");
                writeToFile(topic + ":" + role + ":" + name,
                        getApplicationContext(), "svcdata.txt");
                Log.d(TAG, "MQTT_SUBSCRIBE - Role: " + role + ", " +
                        "topic:" + topic + ", name:" + name);
            }
        }
        if (action == "MQTT_SEND_NAME") {
            Bundle extras = intent.getExtras();
            if(extras == null) {
                Log.d(TAG,"null MQTT_SEND_NAME");
            } else {
                name = extras.getString("name");
                Log.d(TAG, "MQTT_SEND_NAME name: " + name);
            }
            // Good time to write these to a file, so we can retrive them in case
            // of service restart
            this.deleteFile("svcdata.txt");
            writeToFile(topic + ":" + role + ":" + name,
                    getApplicationContext(), "svcdata.txt");
            Log.d(TAG, "Role: " + role + ", " +
                    "topic:" + topic + ", name:" + name);
        }
        if (action == "MQTT_SEND_LOC") {
            Log.d(TAG, "Recvd MQTT Send LOC message.....................");
            Bundle extras = intent.getExtras();
            if(extras == null) {
                Log.d(TAG,"null MQTT_SEND_LOC");
            } else {
                Log.d(TAG, "MQTT_SEND_LOC");
                Float lat = (Float) extras.getFloat("lat");
                Float lon = (Float) extras.getFloat("lon");
                publish(topic, name + ":" + lat + ":" + lon);
            }
        }
        // If we are here without the following values set, we are in trouble.
        // Default these values for now.
        if ((role == null) || (topic == null) || (name == null)) {
            Log.d(TAG, "Null Error !!!! - role: " + role + ", " +
                    "topic:" + topic + ", name:" + name);
            readSvcData();
        }

        mNotificationManager = (NotificationManager) this.getSystemService(
                Context.NOTIFICATION_SERVICE);
        NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID,
                "my_channel",
                NotificationManager.IMPORTANCE_LOW);
        mChannel.enableLights(true);
        mChannel.setLightColor(Color.GREEN);
        mChannel.setSound(null, null);
        //mChannel.setVibrationPattern(new long[] { 0, 400, 200, 400});
        mNotificationManager.createNotificationChannel(mChannel);

        if (running == true) {
            Log.d(TAG, "MQTT Service is already running");
            //mqttHelper.subscribeToTopic("aseemsethi");
            //mqttHelper.connect();
        }
        if ((running == true) && mqttHelper.isConnected()) {
            Log.d(TAG, "MQTT Service is already connected");
            return START_STICKY;
        }
        Log.d(TAG, "restarting MQTT Service");
        try {
            startMqtt(topic);
            running = true;
        } catch (MqttException e) {
            e.printStackTrace();
        }

        // The following "startForeground" with a notification is what makes
        // the service run in the background and not get killed, when the app gets
        // killed by the user.
        intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Notification noti = new Notification.Builder(this, CHANNEL_ID)
                //.setContentTitle("MQTT:")
                .setContentText("Start Svc: " + Calendar.getInstance().getTime())
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{1000, 1000, 1000, 1000, 1000})
                .build();
        startForeground(1, noti,
                FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE |
                        FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        // this is the noti that is shown when running in background
        return START_STICKY;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void sendNotification(String msg) {
        Notification noti;
        Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Log.d(TAG, "Send Notification...");
        /*
        String[] arrOfStr = msg.split(":", 4);
        String title = arrOfStr[0].trim();
        String body = arrOfStr[1].trim() + ":" + arrOfStr[2].trim();
         */

        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        noti = new Notification.Builder(this, CHANNEL_ID)
                //.setContentTitle(title + " : ")
                .setContentText(msg)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(pendingIntent)
                //.setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 })
                //.setSound(defaultSoundUri)
                .build();
        mNotificationManager.notify(incr++, noti);
    }

    private boolean readSvcData() {
        Log.d(TAG, "Read SvcData from file....");
        File file = getApplicationContext().getFileStreamPath("svcdata.txt");
        if(file == null || !file.exists()) {
            Log.d(TAG, "svcdata File not found !!!");
            return true;
        }
        try {
            InputStream inputStream = getApplicationContext().
                    openFileInput("svcdata.txt");
            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();
                if ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append("\n").append(receiveString);
                    Log.d(TAG, "Read: " + receiveString);
                    String[] arrOfStr = receiveString.split(":", 4);
                    if (arrOfStr.length < 3) {
                        Log.d(TAG, "Length = " + arrOfStr.length);
                        return true;
                    }
                    Log.d(TAG, "svcdata Parsed..." +
                            arrOfStr[0] + " : " + arrOfStr[1] +
                            " : " + arrOfStr[2]);
                    topic = arrOfStr[0] != null ? arrOfStr[0] : null;
                    role = arrOfStr[1] != null ? arrOfStr[1] : null;
                    name = arrOfStr[2] != null ? arrOfStr[2] : null;
                    Log.d(TAG, "Read svcdata file - role: " + role + ", " +
                            "topic:" + topic + ", name:" + name);
                } else {
                    Log.d(TAG, "No data in svcdata file");
                }
                inputStream.close();
                //ret = stringBuilder.toString();
                return true;
            }
        }
        catch (FileNotFoundException e) {
            Log.e(TAG, "File svcdata not found: " + e.toString());
        } catch (IOException e) {
            Log.e(TAG, "Can not read svcdata file: " + e.toString());
        }

        return true;
    }

    private void startMqtt(String topic) throws MqttException {
        Log.d(TAG, "startMqtt: role: " + role + ", topic: " + topic +
                ", name: " + name);
        mqttHelper = new MqttHelper(getApplicationContext(), topic, role);
        mqttHelper.mqttAndroidClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable throwable) {
                //Log.d(TAG, "MQTT connection lost !!");
                mqttHelper.connect();
            }

            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                String msg = mqttMessage.toString();
                Log.d(TAG, "MQTT Msg recvd: " + msg);
                String[] arrOfStr = msg.split(":", 4);
                Log.d(TAG, "MQTT Msg recvd...:" + arrOfStr[0] + " : " + arrOfStr[1] +
                        " : " + arrOfStr[2]);

                Intent intent = new Intent();
                intent.setAction("com.aseemsethi.mylocation.IdStatus");
                intent.putExtra("name", arrOfStr[0].trim());
                intent.putExtra("lat", arrOfStr[1].trim());
                intent.putExtra("long", arrOfStr[2].trim());
                String currentTime = new SimpleDateFormat("HH-mm",
                        Locale.getDefault()).format(new Date());
                intent.putExtra("time", currentTime.toString());
                String saveLine = arrOfStr[0].trim() + ":" + arrOfStr[1].trim()
                        + ":" + arrOfStr[2].trim() + ":" + currentTime;
                if (arrOfStr[0].equals("null")) {
                    Log.d(TAG, "Name is null..not saving or broadcasting");
                } else {
                    Log.d(TAG, "Bcat/Saving to file: " + saveLine);
                    writeToFile(saveLine, getApplicationContext(), filename);
                    writeToFile(lineSeparator, getApplicationContext(), filename);
                    sendBroadcast(intent);
                }
                sendNotification("Rec GPS:" + name + "/" + currentTime);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                //Log.d(TAG, "msg delivered");
            }
        });
    }

    private void writeToFile(String data, Context context, String filename) {
        try {
            try (OutputStreamWriter outputStreamWriter =
                         new OutputStreamWriter(context.openFileOutput
                                 (filename, Context.MODE_APPEND))) {
                outputStreamWriter.write(data);
                outputStreamWriter.close();
            }
        } catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    public void publish(String topic, String info)
    {
        byte[] encodedInfo = new byte[0];
        try {
            encodedInfo = info.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedInfo);
            mqttHelper.mqttAndroidClient.publish(topic, message);
            Log.d (TAG, "publish done from: " + role);
            String currentTime = new SimpleDateFormat("HH-mm",
                    Locale.getDefault()).format(new Date());
            sendNotification("Sent GPS: " + name + "/" + currentTime);
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
            Log.e (TAG, e.getMessage());
        }catch (Exception e) {
            Log.e (TAG, "general exception "+e.getMessage());
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "Mqtt Service task removed");
        super.onTaskRemoved(rootIntent);
        //running = false; - else we get multiple MSG RECVD for same message
        sendBroadcast(new Intent("RestartMqtt"));
/*
        Context context = getApplicationContext();
        Intent serviceIntent = new Intent(context, myMqttService.class);
        serviceIntent.setAction(myMqttService.MQTTSUBSCRIBE_ACTION);
        serviceIntent.putExtra("topic", "aseemsethi");
        startService(serviceIntent); */
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Mqtt Service task destroyed");
        running = false;
        mqttHelper.unsubscribeToTopic(topic);
        sendBroadcast(new Intent("RestartMqtt"));
        // The service is no longer used and is being destroyed
    }
}