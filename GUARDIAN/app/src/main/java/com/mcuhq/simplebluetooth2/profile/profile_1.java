package com.mcuhq.simplebluetooth2.profile;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import androidx.lifecycle.ViewModelProvider;

import com.library.lookheartLibrary.server.UserProfile;
import com.library.lookheartLibrary.server.UserProfileManager;
import com.mcuhq.simplebluetooth2.R;

import com.mcuhq.simplebluetooth2.viewmodel.SharedViewModel;

public class profile_1 extends Fragment {
    ScrollView sv;
    View view;

    EditText name, phoneNumber, height, weight, bedTime, wakeup;
    TextView age, gender, birthday;

    public profile_1() {
        // Required empty public constructor
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.activity_profile1,container,false);

        sv = view.findViewById(R.id.scrollView1);

        setFindViewById();

        setText();

        return view;
    }

    private void setText() {
        UserProfile userProfile = UserProfileManager.getInstance().getUserProfile();

        name.setText(userProfile.getName());
        phoneNumber.setText(userProfile.getPhone());
        birthday.setText(userProfile.getBirthday());
        height.setText(userProfile.getHeight());
        weight.setText(userProfile.getWeight());
        bedTime.setText(userProfile.getSleepStart());
        wakeup.setText(userProfile.getSleepEnd());

        age.setText(userProfile.getAge());
        gender.setText(userProfile.getGender().equals("남자") ? getResources().getString(R.string.male_Label) : getResources().getString(R.string.female_Label));

    }

    private void setFindViewById() {
        // EditText
        name = view.findViewById(R.id.EditText_name);
        phoneNumber = view.findViewById(R.id.EditText_PhoneNumber);
        height = view.findViewById(R.id.EditText_Height);
        weight = view.findViewById(R.id.EditText_Weight);
        bedTime = view.findViewById(R.id.EditText_BedTime);
        wakeup = view.findViewById(R.id.EditText_Wakeup);

        // TextView
        age = view.findViewById(R.id.TextView_age);
        gender = view.findViewById(R.id.TextView_Gender);
        birthday = view.findViewById(R.id.EditText_Birthday);
    }

}
