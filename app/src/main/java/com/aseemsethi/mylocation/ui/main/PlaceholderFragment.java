package com.aseemsethi.mylocation.ui.main;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
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

import com.aseemsethi.mylocation.NotificationWorker;
import com.aseemsethi.mylocation.R;
import com.aseemsethi.mylocation.databinding.FragmentMainBinding;

import java.util.concurrent.TimeUnit;

/**
 * A placeholder fragment containing a simple view.
 */
public class PlaceholderFragment extends Fragment {
    private static final String TAG = "MyLocation PF";
    private static final String ARG_SECTION_NUMBER = "section_number";

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
                new PeriodicWorkRequest.Builder(NotificationWorker.class, 15, TimeUnit.MINUTES)
                        .addTag("TAG_GET_GPS_DATA")
                        .setConstraints(constraints)
                        // setting a backoff on case the work needs to retry
                        .setBackoffCriteria(BackoffPolicy.LINEAR, PeriodicWorkRequest.MIN_BACKOFF_MILLIS, TimeUnit.MILLISECONDS)
                        .build();
        btnS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //mWorkManager.enqueue(mRequest);
                Log.d(TAG, "Starting periodic task");
                btnS.setClickable(false);
                mWorkManager.enqueueUniquePeriodicWork(
                        "GPS_DATA_WORK",
                        //ExistingPeriodicWorkPolicy.KEEP, //Existing Periodic Work policy
                        ExistingPeriodicWorkPolicy.REPLACE, //Existing Periodic Work policy
                        periodicSyncDataWork //work request
                );
                Toast.makeText(getContext(),
                        "Starting GPS periodic task", Toast.LENGTH_LONG).show();
            }
        });

        btnC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                    textView.append(v);
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
}