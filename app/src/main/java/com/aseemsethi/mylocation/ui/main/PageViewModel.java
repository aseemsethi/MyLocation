package com.aseemsethi.mylocation.ui.main;

import androidx.arch.core.util.Function;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

public class PageViewModel extends ViewModel {
    String name;
    public void setName(String nameTxt) {
        name = nameTxt;
    }
    public String getName() { return name;    }

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