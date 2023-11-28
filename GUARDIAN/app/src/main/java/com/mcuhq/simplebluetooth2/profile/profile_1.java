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
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import androidx.lifecycle.ViewModelProvider;

import com.mcuhq.simplebluetooth2.R;

import com.mcuhq.simplebluetooth2.viewmodel.SharedViewModel;

public class profile_1 extends Fragment {

    SharedViewModel viewModel;
    String myEmail;

    private Button saveButton;
    private SharedPreferences sharedPreferences;

    private DatePickerDialog.OnDateSetListener dateSetListener;

    private EditText profile1_name,profile1_number,profile1_height,profile1_weight,profile1_sleep1,profile1_sleep2;
    private boolean profile1check = false;

    ScrollView sv;

    public profile_1() {
        // Required empty public constructor
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_profile1,container,false);

        sv = view.findViewById(R.id.scrollView1);

        Button saveButton = view.findViewById(R.id.profile1_save);

        // 계정 정보
        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        myEmail = viewModel.getMyEmail().getValue();

        //개인정보 edittext

        profile1_number = view.findViewById(R.id.profile1_number);

        Bundle args = getArguments();

        //SharedPreferences에서 개인정보 불러오기
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(myEmail, Context.MODE_PRIVATE);
        String savedText2 = sharedPreferences.getString("number","01012345678");

        profile1check = sharedPreferences.getBoolean("profile1check",false);
        profile1_number.setText(savedText2);


        //개인정보 저장버튼
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String textToSave2 = profile1_number.getText().toString();

                // SharedPreferences를 이용하여 데이터 저장
                SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserDetails", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("number", textToSave2);
                editor.putBoolean("profile1check",true);
                editor.apply();

                viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);


                // 저장한 후, 필요한 작업 수행 (예: 토스트 메시지 표시 등)
                Toast.makeText(getActivity(), getResources().getString(R.string.saveData), Toast.LENGTH_SHORT).show();



            }
        });

        //입력할때 키보드에 대한 높이조절
        profile1_number.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {

                KeyboardUp(200);
            }

        });



        return view;
    }

    public void KeyboardUp(int size) {
        sv.postDelayed(new Runnable() {
            @Override
            public void run() {
                sv.smoothScrollTo(0, size);
            }
        }, 200);
    }
}
