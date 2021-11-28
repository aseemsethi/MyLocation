package com.aseemsethi.mylocation;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;

/**
 * Created by wildan on 3/19/2017.
 */
public class MqttHelper {
    public MqttAndroidClient mqttAndroidClient;
    final String TAG = "MyLocation MQTTHelper";
    final String serverUri = "tcp://mqtt.eclipseprojects.io:1883";
    final String clientId = UUID.randomUUID().toString();
    final String subscriptionTopic;
    String role;

    public MqttHelper(Context context, String topic, String role1){
        Log.d(TAG, "MQTT Helper called with topic: " + topic + ", Role: "
        + role1);
        role = role1;
        subscriptionTopic = topic;
        mqttAndroidClient = new MqttAndroidClient(context, serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable throwable) {
                //Log.d(TAG, "22 MQTT connection lost");
            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                Log.d(TAG, "22 MQTT Msg recvd: " + mqttMessage.toString());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
        connect();
    }

    public boolean isConnected() {
        if (mqttAndroidClient.isConnected() == true) {
            return true;
        } else {
            return false;
        }
    }

    public void connect(){
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setCleanSession(true); // was false
        mqttConnectOptions.setKeepAliveInterval(20);
        mqttConnectOptions.setConnectionTimeout(0);
        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "connect succeed with role: " + role);
                    if (role.equals("MGR")) {
                        //Log.d(TAG, "Role is Mgr...");
                        subscribeToTopic(subscriptionTopic);
                    } else {
                        //Log.d(TAG, "Role is Engr....");
                    }
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d(TAG, "Failed to connect to: " + serverUri + exception.toString());
                    try {
                        TimeUnit.SECONDS.sleep(2);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    connect();
                }
            });
        } catch (MqttException ex){
            ex.printStackTrace();
        }
    }

    public void subscribeToTopic(final String subscriptionTopic) {
        Log.d(TAG, "subscribeToTopic: " + subscriptionTopic);
        try {
            mqttAndroidClient.subscribe(subscriptionTopic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG,"Subscribed to: " + subscriptionTopic);
                }
                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d(TAG, "Subscribed fail!");
                }
            });
        } catch (MqttException ex) {
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }
    }

    public void unsubscribeToTopic(final String subscriptionTopic) {
        Log.d(TAG, "Unsubscribe: " + subscriptionTopic);
        try {
            mqttAndroidClient.unsubscribe(subscriptionTopic);
        } catch (MqttException ex) {
            System.err.println("Exception while unsubscribing");
            ex.printStackTrace();
        }
    }
}