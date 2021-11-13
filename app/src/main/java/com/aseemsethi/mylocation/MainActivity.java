package com.aseemsethi.mylocation;

import android.content.Context;
import android.location.LocationManager;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;

import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.widget.ViewPager2;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.aseemsethi.mylocation.ui.main.SectionsPagerAdapter;
import com.aseemsethi.mylocation.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    final String TAG = "MyLocation";
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
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();

            }
        });
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
}