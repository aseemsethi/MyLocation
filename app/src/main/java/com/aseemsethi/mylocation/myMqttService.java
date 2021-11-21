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

import java.io.UnsupportedEncodingException;
import java.util.Calendar;

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
    String CHANNEL_URG = "urgent";
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
        String topic = null;
        Log.d(TAG, "onStartCommand mqttService");
        if (intent == null) {
            Log.d(TAG, "Intent is null..possible due to app restart");
            //action = MQTTMSG_ACTION;
        }
        action = intent.getAction();
        Log.d(TAG, "ACTION: " + action);
        if (action == "MQTTSUBSCRIBE_ACTION") {
            Bundle extras = intent.getExtras();
            if(extras == null) {
                Log.d(TAG,"null MQTTSUBSCRIBE_ACTION");
            } else {
                topic = extras.getString("topic");
                Log.d(TAG, "MQTTSUBSCRIBE_ACTION topic: " + topic);
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
                publish("pmoa", name + ":" + lat + ":" + lon);
            }
        }

        mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID,
                "my_channel",
                NotificationManager.IMPORTANCE_LOW);
        mChannel.enableLights(true);
        mChannel.setLightColor(Color.GREEN);
        mChannel.setSound(null, null);
        //mChannel.setVibrationPattern(new long[] { 0, 400, 200, 400});
        mNotificationManager.createNotificationChannel(mChannel);

        NotificationChannel uChannel = new NotificationChannel(CHANNEL_URG,
                "urg_channel",
                NotificationManager.IMPORTANCE_LOW);
        Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        uChannel.enableLights(true);
        uChannel.setLightColor(Color.RED);
        uChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build();
        AudioAttributes att = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        uChannel.setSound(ringtoneUri, audioAttributes);
        uChannel.setVibrationPattern(new long[]{0, 400, 200, 400});
        mNotificationManager.createNotificationChannel(uChannel);

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
        Notification noti = new Notification.Builder(this, "default")
                .setContentTitle("MQTT:")
                .setContentText("Starting: " + Calendar.getInstance().getTime())
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
        String[] arrOfStr = msg.split(":", 4);
        String title = arrOfStr[0].trim();
        String body = arrOfStr[1].trim() + ":" + arrOfStr[2].trim();

        // Create an explicit intent for an Activity in your app
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        noti = new Notification.Builder(this, CHANNEL_ID)
                //.setContentTitle(title + " : ")
                .setContentText(arrOfStr[0].trim() +
                        "/" + arrOfStr[1].trim() + " :" + arrOfStr[2].trim())
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentIntent(pendingIntent)
                //.setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 })
                //.setSound(defaultSoundUri)
                .build();
        mNotificationManager.notify(incr++, noti);

        if (Double.parseDouble(arrOfStr[1].trim()) > 30.0) {
            Log.d(TAG, "Alarm !!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            noti = new Notification.Builder(this, CHANNEL_URG)
                    .setContentTitle(title + " : ")
                    .setContentText(arrOfStr[1].trim() +
                            "/" + arrOfStr[2].trim())
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentIntent(pendingIntent)
                    .setSound(ringtoneUri)
                    .setVibrate(new long[]{1000, 1000, 1000, 1000, 1000})
                    .build();
            mNotificationManager.notify(incr++, noti);
        }
    }

    private void startMqtt(String topic) throws MqttException {
        Log.d(TAG, "startMqtt");
        mqttHelper = new MqttHelper(getApplicationContext(), topic);
        mqttHelper.mqttAndroidClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable throwable) {
                Log.d(TAG, "MQTT connection lost !!");
                //onTaskRemoved(null);
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
                sendBroadcast(intent);
                Log.d(TAG, "Sent Broadcast.......");
                //publish("pmoa", "hello aseem: " + arrOfStr[0]);

                sendNotification(msg);
                if ((arrOfStr[1].trim()).equals("4ffe1a")) {
                    //Log.d(TAG, "MQTT Msg recvd from: 4ffe1a");
                    //TextView v1 = (TextView) root.findViewById(R.id.sensorValue1);
                    //v1.setText(arrOfStr[2]);
                    //sendNotification(msg);
                } else if ((arrOfStr[1].trim()).equals("f6e01a")) {
                    //Log.d(TAG, "MQTT Msg recvd from: f6e01a");
                    //TextView v1 = (TextView) root.findViewById(R.id.sensorValue2);
                    //v1.setText(arrOfStr[2]);
                    //sendNotification(msg);
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                Log.d(TAG, "msg delivered");
            }
        });
    }

    public void publish(String topic, String info)
    {
        byte[] encodedInfo = new byte[0];
        try {
            encodedInfo = info.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedInfo);
            mqttHelper.mqttAndroidClient.publish(topic, message);
            Log.d (TAG, "publish done");
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
        running = false;
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
        sendBroadcast(new Intent("RestartMqtt"));
        // The service is no longer used and is being destroyed
    }
}