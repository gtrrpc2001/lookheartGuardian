package com.mcuhq.simplebluetooth2.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;

public class SharedViewModel extends ViewModel {

    // Arr 갱신
    private final MutableLiveData<ArrayList<String>> arrList = new MutableLiveData<>(new ArrayList<>());

    // fragment 갱신
    private final MutableLiveData<Boolean> summaryRefreshCheck = new MutableLiveData<>();

    private final MutableLiveData<String> email = new MutableLiveData<>();

    // set
    public void addArrList(String arrDate) {
        ArrayList<String> currentList = arrList.getValue();
        if (currentList != null) {
            currentList.add(arrDate);
            arrList.postValue(currentList);
        }
    }

    public void setSummaryRefreshCheck(Boolean check) {
        summaryRefreshCheck.setValue(check);
    }
    public void setEmail(String myEmail) {
        email.setValue(myEmail);
    }

    // get
    public MutableLiveData<ArrayList<String>> getArrList() {
        return arrList;
    }

    public LiveData<Boolean> getSummaryRefreshCheck() {
        return summaryRefreshCheck;
    }
    public LiveData<String> getMyEmail() {
        return email;
    }
}