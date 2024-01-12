package com.mcuhq.simplebluetooth2.fragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.library.lookheartLibrary.server.UserProfileManager;
import com.library.lookheartLibrary.server.UserProfile;

import com.mcuhq.simplebluetooth2.R;
import com.mcuhq.simplebluetooth2.profile.profile_1;
import com.mcuhq.simplebluetooth2.viewmodel.SharedViewModel;

public class ProfileFragment extends Fragment {

    SharedViewModel viewModel;
    String myEmail;

    private TextView profile_name,profile_email,profile_day;

    private Button profile_logout;

    public ProfileFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile,container,false);

        // 계정 정보
        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        myEmail = viewModel.getMyEmail().getValue();

        profile_name = view.findViewById(R.id.profile_name);
        profile_email = view.findViewById(R.id.profile_email);
        profile_day = view.findViewById(R.id.profile_day);


        profile_logout = view.findViewById(R.id.profile_logout_btn);


        //sharedpreferneces에서 불러오기 (이름,이메일)
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(myEmail, Context.MODE_PRIVATE);
        String name = UserProfileManager.getInstance().getUserProfile().getName();
        String email = UserProfileManager.getInstance().getUserProfile().getEmail();
        String joinDate = UserProfileManager.getInstance().getUserProfile().getJoinDate();

        profile_name.setText(name);
        profile_email.setText(email);
        profile_day.setText(joinDate);


        // 프래그먼트 전환 버튼
        Button btn1 = view.findViewById(R.id.profile_information);



        //기본정보 프래그먼트(profile_1) 기본으로
        profile_1 childFragment = new profile_1();
        FragmentManager fragmentManager = getParentFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.inner_fragment_container, childFragment);
        fragmentTransaction.commit();
        btn1.setTextColor(Color.BLACK);
        btn1.setTextSize(20);


        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                profile_1 childFragment = new profile_1();
                FragmentManager fragmentManager = getParentFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.inner_fragment_container, childFragment);
                fragmentTransaction.commit();


                btn1.setTextColor(Color.BLACK);
                btn1.setTextSize(20);
            }
        });


        //로그아웃버튼 눌렀을때
        profile_logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                SharedPreferences sharedPreferences = getActivity().getSharedPreferences("autologin", Context.MODE_PRIVATE);
                boolean autoLoginCheck = sharedPreferences.getBoolean("autologin", false);

                if (autoLoginCheck) {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean("autologin", false);
                    editor.apply();
                }

                // 다이얼로그 창을 띄웁니다.
                showConfirmationDialog();


            }
        });
        // Inflate the layout for this fragment
        return view;
    }

    //로그아웃 버튼으로 로그인페이지 이동
    private void showConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getResources().getString(R.string.logout))
                .setMessage(getResources().getString(R.string.logoutHelp))
                .setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getActivity().finish();
                    }
                })
                .setNegativeButton(getResources().getString(R.string.rejectLogout), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // 취소 버튼을 눌렀을 때 아무 작업 없이 다이얼로그 창을 닫습니다.
                        dialog.dismiss();
                    }
                });

        Dialog dialog = builder.create();
        dialog.show();
    }
}