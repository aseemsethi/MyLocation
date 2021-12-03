package com.aseemsethi.mylocation;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import com.google.android.gms.auth.api.signin.GoogleSignIn;

import com.aseemsethi.mylocation.ui.main.PageViewModel;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;
import android.util.Log;
import android.view.View;

import com.aseemsethi.mylocation.ui.main.SectionsPagerAdapter;
import com.aseemsethi.mylocation.databinding.ActivityMainBinding;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {
    final String TAG = "MyLocation";
    public static final String MESSAGE_STATUS = "message_status";
    //public static final String ROLE = "ENG";
    public static final String ROLE = "MGR";
    LocationManager locationManager;
    String provider;
    BroadcastReceiver myReceiverMqtt = null;
    BroadcastReceiver myReceiverMqttStatus = null;
    private ActivityMainBinding binding;
    private PageViewModel pageViewModel;
    String name, topic;
    //String topicG = "myGroup";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        pageViewModel = new ViewModelProvider(this).get(PageViewModel.class);
        //pageViewModel.setRole(ROLE);
        pageViewModel.roleSet = ROLE;
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = binding.viewPager;
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = binding.tabs;
        tabs.setupWithViewPager(viewPager);
        if (ROLE.equals("MGR")) {
            tabs.setSelectedTabIndicatorColor(Color.parseColor("#FF0000"));
            tabs.setSelectedTabIndicatorHeight((int) (5 * getResources().getDisplayMetrics().density));
            //tabs.setTabTextColors(Color.parseColor("#727272"), Color.parseColor("#ffffff"));
        }
        Log.d(TAG, "Role: " + ROLE);
        readSvcData();
        Intent serviceIntent = new Intent(getApplicationContext(),
                myMqttService.class);
        serviceIntent.setAction(myMqttService.MQTTSUBSCRIBE_ACTION);
        serviceIntent.putExtra("topic", topic);
        serviceIntent.putExtra("role", ROLE);
        serviceIntent.putExtra("name", name);
        startService(serviceIntent);
    }

    private void getLocation(Context context, View view) {
        Log.d(TAG, "getLocation() ");

        SingleShotLocationProvider.requestSingleUpdate(context,
            new SingleShotLocationProvider.LocationCallback() {
                @Override
                public void onNewLocationAvailable(SingleShotLocationProvider.GPSCoordinates loc) {
                    SingleShotLocationProvider.GPSCoordinates location = loc;
                    Log.d(TAG, "getLocation() LAT: " + location.latitude + ", LON: " + location.longitude);
                    Snackbar.make(view, "lat:"+location.latitude+", " +
                            "lon:" + location.longitude, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }
            });
    }

    void registerServices() {
        Log.d(TAG, "registerServices called filter1");
        IntentFilter filter1 = new IntentFilter("RestartMqtt");
        myReceiverMqtt = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (isMyServiceRunning(myMqttService.class)) {
                    Log.d(TAG, "registerServices: svc is already running");
                    return;
                }
                Log.d(TAG, "registerServices: restart mqttService");
                Intent serviceIntent = new Intent(context, myMqttService.class);
                readSvcData();
                serviceIntent.setAction(myMqttService.MQTTSUBSCRIBE_ACTION);
                serviceIntent.putExtra("topic", topic);
                serviceIntent.putExtra("role", ROLE);
                serviceIntent.putExtra("name", name);
                startService(serviceIntent);
            }
        };
        registerReceiver(myReceiverMqtt, filter1);
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

    @Override
    protected void onResume() {
        super.onResume();
        pageViewModel = new ViewModelProvider(this).get(PageViewModel.class);
        pageViewModel.setRole(ROLE);
        Log.d(TAG, "OnResume - Register BroadcastReceiver");
        //registerServices();
    }
    @Override
    protected void onStart() {
        super.onStart();
        pageViewModel = new ViewModelProvider(this).get(PageViewModel.class);
        pageViewModel.setRole(ROLE);
        Log.d(TAG, "OnStart - Register BroadcastReceiver");
        registerServices();
    }

    private boolean readSvcData() {
        Log.d(TAG, "Read SvcData from file....");
        File file = getApplicationContext().getFileStreamPath(
                "svcdata.txt");
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
                    String role = arrOfStr[1] != null ? arrOfStr[1] : null;
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
}