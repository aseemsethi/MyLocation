package com.aseemsethi.mylocation.ui.main;

import static com.aseemsethi.mylocation.MainActivity.ROLE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.aseemsethi.mylocation.SingleShotLocationProvider;
import com.aseemsethi.mylocation.databinding.SettingsFragmentBinding;
import com.aseemsethi.mylocation.myMqttService;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * A placeholder fragment containing a simple view.
 */
public class SettingsFragment extends Fragment {
    private static final String TAG = "MyLocation SetFrag";
    private static final String ARG_SECTION_NUMBER = "section_number";
    private AlphaAnimation buttonClick = new AlphaAnimation(1F, 0.1F);

    private PageViewModel pageViewModel;
    private SettingsFragmentBinding binding;
    Integer number = 0;

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
                getContext().deleteFile("svcdata.txt");
                Toast.makeText(getContext(),
                        "Deleting Cfg", Toast.LENGTH_SHORT).show();
            }
        });

        final Button btnDelLogs = binding.delGPS;
        btnDelLogs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(buttonClick);
                binding.gpsLogs.setText("");
                getContext().deleteFile("mylocation.txt");
                getContext().deleteFile("clients.txt");
                Toast.makeText(getContext(),
                        "Deleting logs", Toast.LENGTH_SHORT).show();
            }
        });

        SharedPreferences sharedPref = getActivity().
                getPreferences(Context.MODE_PRIVATE);
        String nm = sharedPref.getString("name", "abc");
        String grp = sharedPref.getString("group", "myGroup");
        binding.nameansSF.setText(nm);
        binding.groupSF.setText(grp);
        pageViewModel.setName(nm);
        Intent serviceIntent = new Intent(getContext(),
                myMqttService.class);
        serviceIntent.setAction(myMqttService.MQTT_SEND_NAME);
        serviceIntent.putExtra("name", nm);
        serviceIntent.putExtra("group", grp);
        getContext().startService(serviceIntent);

        final Button btn = binding.buttonSF;
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(buttonClick);
                String nm = binding.nameansSF.getText().toString();
                Log.d(TAG, "Save Name: " + nm);
                pageViewModel.setName(nm);

                String grp = binding.groupSF.getText().toString();
                Log.d(TAG, "Save Group: " + grp);

                SharedPreferences sharedPref = getActivity().getPreferences(
                        Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("name", nm);
                editor.putString("group", grp);
                editor.apply();

                Intent serviceIntent = new Intent(getContext(),
                        myMqttService.class);
                serviceIntent.setAction(myMqttService.MQTT_SEND_NAME);
                serviceIntent.putExtra("name", pageViewModel.getName());
                serviceIntent.putExtra("group", grp);
                getContext().startService(serviceIntent);
            }
        });

        final Button btnGPS = binding.buttonGPS;
        binding.gpsLogs.setMovementMethod(new ScrollingMovementMethod());
        btnGPS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                v.startAnimation(buttonClick);
                if (ROLE.equals("ENG")) {
                    Toast.makeText(getContext(),
                            "Logs disabled", Toast.LENGTH_LONG).show();
                    return;
                }
                String cl = readLogsFromFile(getContext(), "clients.txt");
                binding.gpsLogs.setText("\nNum Clients: " + number);
                binding.gpsLogs.append(cl);
                String str = readLogsFromFile(getContext(), "mylocation.txt");
                binding.gpsLogs.append("\nNum Logs: " + number);
                binding.gpsLogs.append(str);

            }
        });
        return root;
    }

    private String readLogsFromFile(Context context, String filename) {
        String ret = "";
        number = 0;
        File file = context.getFileStreamPath(filename);
        if(file == null || !file.exists()) {
            Log.d(TAG, "File not found: " + filename);
            return "\nLog not created..";
        }
        try {
            InputStream inputStream = context.openFileInput(filename);
            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();
                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append("\n").append(receiveString);
                    Log.d(TAG, "Read: " + receiveString);
                    number++;
                }
                inputStream.close();
                ret = stringBuilder.toString();
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
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}