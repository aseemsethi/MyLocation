package com.aseemsethi.mylocation.ui.main;

import static android.Manifest.permission.ACCESS_BACKGROUND_LOCATION;
import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.aseemsethi.mylocation.MainActivity;
import com.aseemsethi.mylocation.NotificationWorker;
import com.aseemsethi.mylocation.R;
import com.aseemsethi.mylocation.databinding.FragmentMainBinding;
import com.aseemsethi.mylocation.myMqttService;

import java.util.concurrent.TimeUnit;

/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceholderFragment extends Fragment {
    private static final String TAG = "MyLocation PF";
    private static final String ARG_SECTION_NUMBER = "section_number";
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.1F);

    private PageViewModel pageViewModel;
    private FragmentMainBinding binding;

    public static PlaceholderFragment newInstance(int index) {
        PlaceholderFragment fragment = new PlaceholderFragment();
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
        int index = 1;
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_SECTION_NUMBER);
        }
        pageViewModel.setIndex(index);
        checkLocationPermission();
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        binding = FragmentMainBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textView;
        pageViewModel.getText().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String s) {
                Log.d(TAG, "pageViewModel: onChangted");
                textView.setText(s);
            }
        });

        // Create Network constraint
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        final Button btnS = binding.startB;
        final Button btnC = binding.cancelB;
        final WorkManager mWorkManager = WorkManager.getInstance();
        //final OneTimeWorkRequest mRequest = new OneTimeWorkRequest.Builder(NotificationWorker.class).build();
        PeriodicWorkRequest periodicSyncDataWork =
                new PeriodicWorkRequest.Builder(NotificationWorker.class,
                        15, TimeUnit.MINUTES)
                        .addTag("TAG_GET_GPS_DATA")
                        .setConstraints(constraints)
                        // setting a backoff on case the work needs to retry
                        .setBackoffCriteria(BackoffPolicy.LINEAR, PeriodicWorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                        .build();
        PeriodicWorkRequest periodicSyncDataWork1 =
                new PeriodicWorkRequest.Builder(NotificationWorker.class,
                        15, TimeUnit.MINUTES)
                        .addTag("TAG_GET_GPS_DATA1")
                        .setConstraints(constraints)
                        .setInitialDelay(7, TimeUnit.MINUTES)
                        // setting a backoff on case the work needs to retry
                        .setBackoffCriteria(BackoffPolicy.LINEAR, PeriodicWorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                        .build();
        btnS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //mWorkManager.enqueue(mRequest);
                v.startAnimation(buttonClick);
                Log.d(TAG, "Starting periodic task");
                btnS.setClickable(false);
                mWorkManager.enqueueUniquePeriodicWork(
                        "GPS_DATA_WORK",
                        ExistingPeriodicWorkPolicy.KEEP, //Existing Periodic Work policy
                        //ExistingPeriodicWorkPolicy.REPLACE, //Existing Periodic Work policy
                        periodicSyncDataWork //work request
                );
                mWorkManager.enqueueUniquePeriodicWork(
                        "GPS_DATA_WORK1",
                        ExistingPeriodicWorkPolicy.KEEP, //Existing Periodic Work policy
                        periodicSyncDataWork1 //work request
                );
                Toast.makeText(getContext(),
                        "Starting GPS periodic task", Toast.LENGTH_LONG).show();
            }
        });

        btnC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(buttonClick);
                Log.d(TAG, "Cancelling periodic task");
                btnS.setClickable(true);
                WorkManager.getInstance(getContext()).cancelAllWorkByTag("TAG_GET_GPS_DATA");
                Toast.makeText(getContext(),
                        "Cancelling GPS periodic task", Toast.LENGTH_LONG).show();
            }
        });

        mWorkManager.getWorkInfoByIdLiveData(periodicSyncDataWork.getId()).
                observe(this, new Observer<WorkInfo>() {
            @Override
            public void onChanged(@Nullable WorkInfo workInfo) {
                float latitude = 0, longitude = 0;
                if (workInfo != null) {
                    latitude = workInfo.getOutputData().getFloat("LAT", 0);
                    longitude = workInfo.getOutputData().getFloat("LON", 0);
                    WorkInfo.State state = workInfo.getState();
                    String v = String.valueOf(latitude) + " : " + String.valueOf(longitude) + "\n";
                    //textView.append(state.toString());
                    // Some bug here TBD - get all 0s here everytime
                    //pageViewModel.setTextOp(v);
                    Log.d(TAG, "workMgr liveData.." + state.toString());
                    Log.d(TAG, "workMgr liveData.." + latitude + ":" + longitude);
                }
            }
        });
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(getContext(),
                ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getContext(),
                        ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getContext(),
                        ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                    ACCESS_BACKGROUND_LOCATION) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                            ACCESS_COARSE_LOCATION) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                            ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously
                new AlertDialog.Builder(getContext())
                        .setTitle("Location Permission")
                        .setMessage("Please approve location permissions")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Log.d(TAG, "Clicked..................");
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(getActivity(),
                                        new String[]{ACCESS_BACKGROUND_LOCATION,
                                                ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        }).create().show();
            } else {
                Log.d(TAG, "show the dialog..................");
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{ACCESS_BACKGROUND_LOCATION, ACCESS_BACKGROUND_LOCATION,
                                ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            Log.d(TAG, "Permissions already granted");
            Toast.makeText(getContext(),
                    "Permissions already granted", Toast.LENGTH_SHORT).show();
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && (grantResults[0] + grantResults[1] + grantResults[2]
                        == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(getContext(),
                            ACCESS_BACKGROUND_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        Log.d(TAG, "Permission granted !!!");
                    }

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.d(TAG, "Permission denied");
                }
                return;
            }

        }
    }
}