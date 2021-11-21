package com.aseemsethi.mylocation.ui.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.aseemsethi.mylocation.databinding.MgrFragmentBinding;
import com.aseemsethi.mylocation.myMqttService;

/**
 * A placeholder fragment containing a simple view.
 */
public class MgrFragment extends Fragment {
    private static final String TAG = "MyLocation MgrFrag";
    private static final String ARG_SECTION_NUMBER = "section_number";
    BroadcastReceiver myRecv = null;

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
            }
        };
        getContext().getApplicationContext().registerReceiver(myRecv, filter2);
    }
    @Override
    public void onResume() {
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
        super.onDestroyView();
        binding = null;
    }
}