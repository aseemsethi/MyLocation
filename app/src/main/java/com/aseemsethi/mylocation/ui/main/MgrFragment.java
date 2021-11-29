package com.aseemsethi.mylocation.ui.main;

import static com.aseemsethi.mylocation.MainActivity.ROLE;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.ButtCap;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.google.maps.android.ui.IconGenerator;

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
    private static final String ROLE_SET = "role_set";
    private boolean MgrBroacastRegistred = false;
    BroadcastReceiver myRecv = null;
    MapView mapView;
    GoogleMap map;
    String role;
    IconGenerator iconFactory;
    private AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.1F);

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
        //role = pageViewModel.getRole();
        role = pageViewModel.roleSet;
        if (role == null) {
            Log.d(TAG, "OnCreateView: Role is null !!!!!!!!!!!!!!!!!!!!!!!11");
            role = ROLE;
        }
        if (role.equals("MGR")) {
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
                    v.startAnimation(buttonClick);
                    map.clear();
                    readFromFile(getContext(), binding.nameMgr.getText().toString(), false);
                }
            });
            final Button btnLast = binding.buttonLast;
            btnLast.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.startAnimation(buttonClick);
                    map.clear();
                    updateLastOnMap(getContext());
                }
            });
        }
        return root;
    }

    private void updateLastOnMap(Context context) {
        String nameC;
        String lastLoc = null;
        Boolean found = false;
        String[] arrOfStr1;

        File fileL = context.getFileStreamPath("mylocation.txt");
        if(fileL == null || !fileL.exists()) {
            Log.d(TAG, "GPS File not found !!!");
            return;
        }
        File fileC = context.getFileStreamPath("clients.txt");
        if(fileC == null || !fileC.exists()) {
            Log.d(TAG, "Clients File not found !!!");
            return;
        }

        try {
            InputStream inputStream = context.openFileInput("clients.txt");
            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();
                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    String[] arrOfStr = receiveString.split(":", 2);
                    Log.d(TAG, "clients Parsed..." + arrOfStr[0]);
                    nameC = arrOfStr[0];
                    try {
                        InputStream inputStream1 = context.openFileInput("mylocation.txt");
                        if ( inputStream != null ) {
                            InputStreamReader inputStreamReader1 = new InputStreamReader(inputStream1);
                            BufferedReader bufferedReader1 = new BufferedReader(inputStreamReader1);
                            String receiveString1 = "";
                            StringBuilder stringBuilder1 = new StringBuilder();
                            while ( (receiveString1 = bufferedReader1.readLine()) != null ) {
                                arrOfStr1 = receiveString1.split(":", 5);
                                //Log.d(TAG, "GPS Parsed..." + arrOfStr1[0]);
                                if (nameC.equals(arrOfStr1[0])) {
                                    lastLoc = receiveString1; found = true;
                                }
                            }
                            inputStream1.close();
                            if (found) {
                                arrOfStr1 = lastLoc.split(":", 5);
                                float lat = Float.parseFloat(arrOfStr1[1]);
                                float lon = Float.parseFloat(arrOfStr1[2]);
                                String currentTime = arrOfStr1[3];
                                Log.d(TAG, "Found lastLoc..." + arrOfStr1[0] + " : "
                                        + arrOfStr1[1] + " : " + arrOfStr1[2] + ":"
                                        + arrOfStr1[3]);
                                updateMap(arrOfStr1[0], lat, lon, currentTime);
                            }
                        }
                    }
                    catch (FileNotFoundException e) {
                        Log.e(TAG, "File Location not found: " + e.toString());
                    } catch (IOException e) {
                        Log.e(TAG, "Can not read Location file: " + e.toString());
                    }
                }
                inputStream.close();
                //ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e(TAG, "File clients not found: " + e.toString());
        } catch (IOException e) {
            Log.e(TAG, "Can not read clients file: " + e.toString());
        }
    }

    private String readFromFile(Context context, String nameS, Boolean finalLoc) {
        String ret = "";
        Log.d(TAG, "Read from file: " + nameS);
        File file = context.getFileStreamPath("mylocation.txt");
        if(file == null || !file.exists()) {
            Log.d(TAG, "File not found !!!");
            return "true";
        }
        try {
            InputStream inputStream = context.openFileInput("mylocation.txt");
            if ( inputStream != null ) {
                PolylineOptions options = new PolylineOptions();
                options.color(Color.RED);
                options.endCap(new ButtCap());
                options.endCap(new RoundCap());
                options.visible(true);
                options.jointType(JointType.ROUND);

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();
                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append("\n").append(receiveString);
                    //Log.d(TAG, "Read: " + receiveString);
                    String[] arrOfStr = receiveString.split(":", 5);
                    if (arrOfStr.length < 3) {
                        Log.d(TAG, "Length = " + arrOfStr.length);
                        continue;
                    }
                    Log.d(TAG, "Parsed..." + arrOfStr[0] + " : " + arrOfStr[1] +
                            " : " + arrOfStr[2] + ":" +
                            arrOfStr[3]);
                    float lat = Float.parseFloat(arrOfStr[1]);
                    float lon = Float.parseFloat(arrOfStr[2]);
                    String currentTime = arrOfStr[3];
                    if ((nameS == null) || (nameS.equals(""))) {
                        updateMap(arrOfStr[0], lat, lon, currentTime);
                    } else {
                        if (arrOfStr[0].equalsIgnoreCase(nameS)) {
                            updateMap(arrOfStr[0], lat, lon, currentTime);
                            options.add(new LatLng(lat, lon));
                            map.addPolyline(options);
                        }
                    }
                }
                inputStream.close();
                //ret = stringBuilder.toString();
                return "true";
            }
        }
        catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e(TAG, "Can not read file: " + e.toString());
        }

        return ret;
    }

    @Override
    public void onMapReady(GoogleMap mGoogleMap) {
        Log.d(TAG, "onMapReady called.....");
        iconFactory = new IconGenerator(getContext());
        map = mGoogleMap;
        map.clear();
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
        readFromFile(getContext(), null, false);
    }
    public void updateMap(String name, float lat, float lon, String currentTime) {
        if (map == null) {
            Log.d(TAG, "map is null...........................");
            return;
        }
        if (role.equals("ENG")) {
            return;
        }
        // Updates the location and zoom of the MapView
        Marker m = map.addMarker(new MarkerOptions().
                visible(true).
                title(name + ":" + currentTime).
                position(new LatLng(lat, lon)));
        m.setIcon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon(name +
                ":" + currentTime)));
        m.showInfoWindow();

        /*
        Marker mMarkerA = map.addMarker(new MarkerOptions().position(new LatLng(12, 34)));
        mMarkerA.setIcon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon("Marker A")));
        Marker mMarkerB = map.addMarker(new MarkerOptions().position(new LatLng(13, 35)));
        mMarkerB.setIcon(BitmapDescriptorFactory.fromBitmap(iconFactory.makeIcon("Marker B")));
        */
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(new LatLng
                (lat, lon), 10);
        map.animateCamera(cameraUpdate);
    }

    void registerServices() {
        Log.d(TAG, "registerServices called filter2");
        if (MgrBroacastRegistred == false) {
            MgrBroacastRegistred = true;
        } else {
            Log.d(TAG, "Recevier already registered");
            return;
        }
        IntentFilter filter2 = new IntentFilter("com.aseemsethi.mylocation.IdStatus");
        myRecv = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "registerServices: IdStatus:" +
                        intent.getStringExtra("name") + " : " +
                        intent.getStringExtra("lat") + " : " +
                        intent.getStringExtra("long") + ":" +
                        intent.getStringExtra("time"));
                pageViewModel.setTextOp( intent.getStringExtra("name")
                        + ":" + intent.getStringExtra("lat") +
                        ":" + intent.getStringExtra("long") +
                        ":" + intent.getStringExtra("time"));
                float lat = Float.parseFloat(intent.getStringExtra("lat"));
                float lon = Float.parseFloat(intent.getStringExtra("long"));
                String currentTime = intent.getStringExtra("time");
                updateMap(intent.getStringExtra("name"), lat, lon, currentTime);
            }
        };
        getContext().getApplicationContext().registerReceiver(myRecv, filter2);
    }

    @Override
    public void onResume() {
        super.onResume();
        pageViewModel = new ViewModelProvider(this).get(PageViewModel.class);
        role = pageViewModel.getRole();
        if (role == null) {
            Log.d(TAG, "OnResume Role is null !!!!!!!!!!!!!!!!!!!!!!!11");
            role = ROLE;
        }
        if (role.equals("MGR"))
            mapView.onResume();
        registerServices();
    }
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Unregister recv");
        try {
            getContext().unregisterReceiver(myRecv);
            MgrBroacastRegistred = false;
        } catch (Exception e){
            Log.d(TAG, "onPause: Already Unregistered recv");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        pageViewModel = new ViewModelProvider(this).get(PageViewModel.class);
        role = pageViewModel.getRole();
        if (role == null) {
            Log.d(TAG, "OnStart Role is null !!!!!!!!!!!!!!!!!!!!!!!11");
            role = ROLE;
        }
        registerServices();
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroy: Unregister recv");
        try {
            getContext().unregisterReceiver(myRecv);
            MgrBroacastRegistred = false;
        } catch (Exception e){
            Log.d(TAG, "onPause: Already Unregistered recv");
        }
        binding = null;
    }
}