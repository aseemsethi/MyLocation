package com.aseemsethi.mylocation.ui.main;

import android.util.Log;

import androidx.arch.core.util.Function;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

public class PageViewModel extends ViewModel {
    final String TAG = "MyLocation PV";
    String name, roleSet;

    public void setName(String nameTxt) {
        name = nameTxt;
    }
    public String getName() { return name;    }

    public void setRole(String role) {
        roleSet = role;
        Log.d(TAG, "Setting role: " + roleSet);
    }
    public String getRole() {
        Log.d(TAG, "Return role: " + roleSet);
        return roleSet;
    }

    private MutableLiveData<Integer> mIndex = new MutableLiveData<>();
    private MutableLiveData<String> mText = new MutableLiveData<>();
    /*
    private LiveData<String> mText = Transformations.map(mIndex, new Function<Integer,
            String>() {
        @Override
        public String apply(Integer input) {
            return "Coord: " + "\n";
        }
    });
     */

    public void setIndex(int index) {
        mIndex.setValue(index);
    }
    public void setTextOp(String text1) {
        mText.setValue(text1);
    }
    public LiveData<String> getText() {
        return mText;
    }

}