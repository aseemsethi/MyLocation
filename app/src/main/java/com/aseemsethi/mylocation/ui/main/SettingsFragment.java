package com.aseemsethi.mylocation.ui.main;

import android.content.Intent;
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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.aseemsethi.mylocation.SingleShotLocationProvider;
import com.aseemsethi.mylocation.databinding.SettingsFragmentBinding;
import com.aseemsethi.mylocation.myMqttService;

import java.io.File;

/**
 * A placeholder fragment containing a simple view.
 */
public class SettingsFragment extends Fragment {
    private static final String TAG = "MyLocation SetFrag";
    private static final String ARG_SECTION_NUMBER = "section_number";
    private AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.1F);

    private PageViewModel pageViewModel;
    private SettingsFragmentBinding binding;

    public static SettingsFragment newInstance(int index) {
        SettingsFragment fragment = new SettingsFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, index);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageViewModel = new ViewModelProvider(this).get(PageViewModel.class);
        int index = 2;
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_SECTION_NUMBER);
        }
        pageViewModel.setIndex(index);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        binding = SettingsFragmentBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final Button btnDF = binding.buttonDF;
        btnDF.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(buttonClick);
                getContext().deleteFile("mylocation.txt");
                getContext().deleteFile("svcdata.txt");
            }
        });

        final Button btn = binding.buttonSF;
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(buttonClick);
                Log.d(TAG, "Save Name: " + binding.nameansSF.getText().toString());
                pageViewModel.setName(binding.nameansSF.getText().toString());
                Intent serviceIntent = new Intent(getContext(),
                        myMqttService.class);
                serviceIntent.setAction(myMqttService.MQTT_SEND_NAME);
                serviceIntent.putExtra("name", pageViewModel.getName());
                getContext().startService(serviceIntent);
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