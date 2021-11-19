package com.aseemsethi.mylocation;

import android.Manifest;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
//import androidx.viewpager.widget.ViewPager;
//import androidx.viewpager2.widget.ViewPager2;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.aseemsethi.mylocation.ui.main.SectionsPagerAdapter;
import com.aseemsethi.mylocation.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    final String TAG = "MyLocation";
    public static final String MESSAGE_STATUS = "message_status";
    LocationManager locationManager;
    String provider;
    BroadcastReceiver myReceiverMqtt = null;
    BroadcastReceiver myReceiverMqttStatus = null;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = binding.viewPager;
        viewPager.setAdapter(sectionsPagerAdapter);
        TabLayout tabs = binding.tabs;
        tabs.setupWithViewPager(viewPager);
        FloatingActionButton fab = binding.fab;

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "call getLocation() ");
                getLocation(getApplicationContext(), view);
                Snackbar.make(view, "Getting coordinates..", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

            }
        });
        Intent serviceIntent = new Intent(getApplicationContext(),
                myMqttService.class);
        serviceIntent.setAction(myMqttService.MQTTSUBSCRIBE_ACTION);
        serviceIntent.putExtra("topic", "pmoa");
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
                serviceIntent.setAction(myMqttService.MQTTSUBSCRIBE_ACTION);
                serviceIntent.putExtra("topic", "pmoa");
                startService(serviceIntent);
            }
        };
        registerReceiver(myReceiverMqtt, filter1);

        Log.d(TAG, "registerServices called filter2");
        IntentFilter filter2 = new IntentFilter("com.aseemsethi.mylocation.IdStatus");
        myReceiverMqttStatus = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "registerServices: IdStatus:" +
                        intent.getStringExtra("Id") + " : " +
                        intent.getStringExtra("Status"));
                //pageViewModel.setStatus( intent.getStringExtra("Id") +
                //        ":" + intent.getStringExtra("Status"));
            }
        };
        registerReceiver(myReceiverMqttStatus, filter2);
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
        Log.d(TAG, "OnResume - Register BroadcastReceiver");
        //registerServices();
    }
    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "OnStart - Register BroadcastReceiver");
        registerServices();
    }
}