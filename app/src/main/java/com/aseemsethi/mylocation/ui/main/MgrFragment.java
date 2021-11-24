package com.aseemsethi.mylocation.ui.main;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.aseemsethi.mylocation.databinding.MgrFragmentBinding;
import com.aseemsethi.mylocation.myMqttService;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * A placeholder fragment containing a simple view.
 */
public class MgrFragment extends Fragment implements OnMapReadyCallback {
    private static final String TAG = "MyLocation MgrFrag";
    private static final String ARG_SECTION_NUMBER = "section_number";
    BroadcastReceiver myRecv = null;
    MapView mapView;
    GoogleMap map;

    private PageViewModel pageViewModel;
    private MgrFragmentBinding binding;

    public static MgrFragment newInstance(int index) {
        MgrFragment fragment = new MgrFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, index);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "OnCreate");
        super.onCreate(savedInstanceState);
        pageViewModel = new ViewModelProvider(this).get(PageViewModel.class);
        int index = 3;
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_SECTION_NUMBER);
        }
        pageViewModel.setIndex(index);
        registerServices();
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        binding = MgrFragmentBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        final TextView textView = binding.loc;

        mapView = (MapView) binding.mapview;
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);
        MapsInitializer.initialize(this.getActivity());

        pageViewModel.getText().observe(getViewLifecycleOwner(),
                new Observer<String>() {
                    @Override
                    public void onChanged(@Nullable String s) {
                        textView.setText(s);
                    }
                });

        final Button btn = binding.buttonMgr;
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick");
            }
        });
        return root;
    }

    private String readFromFile(Context context) {
        String ret = "";
        Log.d(TAG, "Read from file....");
        File file = context.getFileStreamPath("mylocation.txt");
        if(file == null || !file.exists()) {
            Log.d(TAG, "File not found !!!");
            return "true";
        }
        try {
            InputStream inputStream = context.openFileInput("mylocation.txt");
            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();
                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append("\n").append(receiveString);
                    Log.d(TAG, "Read: " + receiveString);
                    String[] arrOfStr = receiveString.split(":", 4);
                    if (arrOfStr.length < 3) {
                        Log.d(TAG, "Length = " + arrOfStr.length);
                        continue;
                    }
                    Log.d(TAG, "Parsed..." + arrOfStr[0] + " : " + arrOfStr[1] +
                            " : " + arrOfStr[2]);
                    float lat = Float.parseFloat(arrOfStr[1]);
                    float lon = Float.parseFloat(arrOfStr[2]);
                    updateMap(arrOfStr[0], lat, lon);
                }
                inputStream.close();
                //ret = stringBuilder.toString();
                return "true";
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return ret;
    }

    @Override
    public void onMapReady(GoogleMap mGoogleMap) {
        Log.d(TAG, "onMapReady called.....");
        map = mGoogleMap;
        mGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);
        if (ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(),
                        Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "No location permissions");
            return;
        }
        mGoogleMap.setMyLocationEnabled(true);
        mGoogleMap.getUiSettings().setZoomControlsEnabled(true);
        Marker m = mGoogleMap.addMarker(new MarkerOptions()
                .visible(true).title("nil").position(new LatLng(43.1, -87.9)));
        m.showInfoWindow();
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(43.1, -87.9), 10));
        readFromFile(getContext());
    }
    public void updateMap(String name, float lat, float lon) {
        // Updates the location and zoom of the MapView
        Marker m = map.addMarker(new MarkerOptions().
                visible(true).title(name).position(new LatLng(lat, lon)));
        m.showInfoWindow();
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng
                (lat, lon), 10);
        map.animateCamera(cameraUpdate);
    }

    void registerServices() {
        Log.d(TAG, "registerServices called filter2");
        IntentFilter filter2 = new IntentFilter("com.aseemsethi.mylocation.IdStatus");
        myRecv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "registerServices: IdStatus:" +
                        intent.getStringExtra("name") + " : " +
                        intent.getStringExtra("lat") + " : " +
                        intent.getStringExtra("long"));
                pageViewModel.setTextOp( intent.getStringExtra("name")
                        + ":" + intent.getStringExtra("lat") +
                        ":" + intent.getStringExtra("long"));
                float lat = Float.parseFloat(intent.getStringExtra("lat"));
                float lon = Float.parseFloat(intent.getStringExtra("long"));
                updateMap(intent.getStringExtra("name"), lat, lon);
            }
        };
        getContext().getApplicationContext().registerReceiver(myRecv, filter2);
    }

    @Override
    public void onResume() {
        mapView.onResume();
        super.onResume();
        Log.d(TAG, "OnResume - Register BroadcastReceiver");
        //registerServices();
    }
    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "OnStart - Register BroadcastReceiver");
        registerServices();
    }
    @Override
    public void onDestroyView() {
        mapView.onResume();
        super.onDestroyView();
        binding = null;
    }
}