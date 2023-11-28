package com.mcuhq.simplebluetooth2.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextWatcher;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import android.util.Base64;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mcuhq.simplebluetooth2.R;
import com.mcuhq.simplebluetooth2.server.RetrofitServerManager;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;


public class Activity_Login extends AppCompatActivity {

    RetrofitServerManager retrofitServerManager;

    /*이메일/비밀번호 정규식*/
    //region
    final String emailPattern = "^[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,20}$";
    final String passwordPattern = "^(?=.*?[A-Z])(?=.*?[a-z])(?=.*?[0-9])(?=.*?[#?!@$%^&<>*~:`-]).{10,}$";
    final String phoneNumberPattern = "^[0-9]{9,11}$";
    private String email = "";
    private String password = "";
    private String guardian = "";
    //endregion

    /*editText*/
    //region
    EditText emailEditText;
    EditText passwordEditText;
    EditText guardianEditText;
    //endregion

    /*button*/
    //region
    Button autoLoginButton;
    Button loginButton;
    //endregion

    /*autologin*/
    //region
    boolean autoLoginCheck;
    boolean autoLogin;
    //endregion

    /*check*/
    //region
    Boolean emailCheck;
    Boolean passwordCheck;
    Boolean guardianCheck;
    Map<String, Boolean> dataCheck;
    //endregion

    ImageButton autoLoginImageButton;
    ScrollView sv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        retrofitServerManager = new RetrofitServerManager();

        setViewID();

        // 입력 데이터 초기화
        setDataCheckClear();

        // 초기화
        setCheckClear();

        // edit Text hint set
        setHintText();

        SharedPreferences autoLoginSP = getSharedPreferences("autologin", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = autoLoginSP.edit();

        autoLogin = autoLoginSP.getBoolean("autologin", false);
//        autoLogin = true;
        if (autoLogin) {
            Intent intent = new Intent(Activity_Login.this, Activity_Main.class);
//            Intent intent = new Intent(Activity_Login.this, OverviewActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }

        // email event
        setEmailEditTextEvent();

        // password event
        setPasswordEditTextEvent();

        setguardianEditTextEvent();

        setButtonEvent(editor);
    }

    void setguardianEditTextEvent(){
        guardianEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    // 포커스가 없어질 때
                    if (guardianCheck) {
                        // 입력 값이 유효한 경우
                    }
                    else {
                        // 입력 값이 유효하지 않은 경우
                    }
                } else {
                    // 포커스를 얻었을 때
                    KeyboardUp(400);
                }
            }
        });

        guardianEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable editable) {
                guardian = editable.toString();

                if (editable.toString().trim().matches(phoneNumberPattern)) {
                    // 매칭되는 경우
                    dataCheck.put("guardian", true);
                } else {
                    // 매칭되지 않는 경우
                    dataCheck.put("guardian", false);
                }
                // 유효성 체크
                guardianCheck = dataCheck.get("guardian");
            }
        });
    }

    void setPasswordEditTextEvent(){
        passwordEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    // 포커스가 없어질 때
                    if (passwordCheck) {
                        // 입력 값이 유효한 경우
                    }
                    else {
                        // 입력 값이 유효하지 않은 경우
                    }
                } else {
                    // 포커스를 얻었을 때
                    KeyboardUp(400);
                }
            }
        });

        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                password = s.toString();

                if (s.toString().trim().matches(passwordPattern)) {
                    // 매칭되는 경우
                    dataCheck.put("password", true);
                } else {
                    // 매칭되지 않는 경우
                    dataCheck.put("password", false);
                }
                // 유효성 체크
                passwordCheck = dataCheck.get("password");
            }
        });
    }

    void setButtonEvent(SharedPreferences.Editor editor){
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // null 값 확인
                if ( (email == null || email.isEmpty()) && (password == null || password.isEmpty()) && (guardian == null || guardian.isEmpty())) {
                    Toast.makeText(Activity_Login.this, getResources().getString(R.string.lignAlert), Toast.LENGTH_SHORT).show();
                    return;
                }
                else if (email == null || email.isEmpty()) {
                    Toast.makeText(Activity_Login.this, getResources().getString(R.string.email_Hint), Toast.LENGTH_SHORT).show();
                    return;
                }
                else if (password == null || password.isEmpty()) {
                    Toast.makeText(Activity_Login.this, getResources().getString(R.string.password_Hint), Toast.LENGTH_SHORT).show();
                    return;
                }
                else if (guardian == null || guardian.isEmpty()) {
                    Toast.makeText(Activity_Login.this, getResources().getString(R.string.guardian_Hint), Toast.LENGTH_SHORT).show();
                    return;
                }

                // 암호화
                String encPw = encryptECB("MEDSYSLAB.CO.KR.LOOKHEART.ENCKEY", password);

                retrofitServerManager.loginTask(email, encPw.trim(), guardian, null, new RetrofitServerManager.ServerTaskCallback() {
                    @Override
                    public void onSuccess(String result) {
                        if (result.toLowerCase().contains("true")){

                            SharedPreferences sharedPref = getSharedPreferences(email, Context.MODE_PRIVATE);
                            SharedPreferences.Editor userEditor = sharedPref.edit();

                            SharedPreferences emailSharedPreferences = getSharedPreferences("User", Context.MODE_PRIVATE);
                            SharedPreferences.Editor emailEditor = emailSharedPreferences.edit();

                            userEditor.putString("email", email);
                            userEditor.putString("guardian", guardian);
                            userEditor.putString("password", encPw.trim());
                            userEditor.commit();

                            emailEditor.putString("email", email);
                            emailEditor.commit();

                            editor.putBoolean("autologin", autoLoginCheck);
                            editor.commit();

                            Intent intent = new Intent(Activity_Login.this, Activity_Main.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();

                            runOnUiThread(() -> Toast.makeText(Activity_Login.this, getResources().getString(R.string.loginSuccess), Toast.LENGTH_SHORT).show());
                        }
                        else{
                            runOnUiThread(() -> {
                                AlertDialog.Builder builder = new AlertDialog.Builder(Activity_Login.this);
                                builder.setTitle(getResources().getString(R.string.loginFailed))
                                        .setMessage(getResources().getString(R.string.incorrectlyLogin))
                                        .setNegativeButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                // 취소 버튼 클릭 시 수행할 동작
                                                dialog.cancel(); // 팝업창 닫기
                                            }
                                        })
                                        .show();
                            });
                        }

                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                    }

                    @Override
                    public void onFailure(Exception e) {
                        runOnUiThread(() -> Toast.makeText(Activity_Login.this, getResources().getString(R.string.serverErr), Toast.LENGTH_SHORT).show());
                    }

                });

            }
        });
    }

    void setEmailEditTextEvent(){
        emailEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    // 포커스가 없어질 때
                    if (emailCheck) {
                        // 입력 값이 유효한 경우
                    }
                    else {
                        // 입력 값이 유효하지 않은 경우
                    }
                } else {
                    // 포커스를 얻었을 때
                    KeyboardUp(400);
                }
            }
        });
        emailEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override
            public void afterTextChanged(Editable s) {
                email = s.toString();

                if (s.toString().trim().matches(emailPattern)) {
                    // 매칭되는 경우
                    dataCheck.put("email", true);
                } else {
                    // 매칭되지 않는 경우
                    dataCheck.put("email", false);
                }
                // 유효성 체크
                emailCheck = dataCheck.get("email");
            }
        });
    }

    void setViewID(){
        // edit text
        emailEditText = findViewById(R.id.editEmail);
        passwordEditText = findViewById(R.id.editPassword);
        guardianEditText = findViewById(R.id.guardian_EditText);

        // auto Login Button
        autoLoginButton = findViewById(R.id.autoLogin);
        autoLoginImageButton = findViewById(R.id.autoLoginImage);

        loginButton = findViewById(R.id.loginButton);

        sv = findViewById(R.id.scrollView);
    }

    void setDataCheckClear(){
        dataCheck = new HashMap<>();
        dataCheck.put("email", false);
        dataCheck.put("password", false);
    }

    void setCheckClear(){
        autoLoginCheck = false;
        emailCheck = false;
        passwordCheck = false;
    }

    public void KeyboardUp(int size) {
        sv.postDelayed(new Runnable() {
            @Override
            public void run() {
                sv.smoothScrollTo(0, size);
            }
        }, 200);
    }

    // 자동 로그인 클릭 이벤트
    public void autoLoginClickEvent(View v) {
        autoLoginCheck = !autoLoginCheck;
        if (autoLoginCheck) {
            autoLoginImageButton.setImageResource(R.drawable.login_autologin_press);
        }else {
            autoLoginImageButton.setImageResource(R.drawable.login_autologin_normal);
        }
    }

    public void setHintText(){
        // EditText에 힌트 텍스트 스타일을 적용
        String emailHintText = getResources().getString(R.string.email_Hint);
        String passwordHintText = getResources().getString(R.string.password_Hint);
        String guardianHintText = getResources().getString(R.string.guardian_Hint);

        // 힌트 텍스트에 스타일을 적용
        SpannableString ssEmail = new SpannableString(emailHintText);
        SpannableString ssPassword = new SpannableString(passwordHintText);
        SpannableString ssGuardian = new SpannableString(guardianHintText);

        AbsoluteSizeSpan assEmail = new AbsoluteSizeSpan(12, true); // 힌트 텍스트 크기 설정
        AbsoluteSizeSpan assPassword = new AbsoluteSizeSpan(12, true);
        AbsoluteSizeSpan assGuardian = new AbsoluteSizeSpan(12, true);

        ssEmail.setSpan(assEmail, 0, ssEmail.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE); // 크기 적용
        ssPassword.setSpan(assPassword, 0, ssPassword.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssGuardian.setSpan(assGuardian, 0, ssGuardian.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        // 힌트 텍스트 굵기 설정
        ssEmail.setSpan(new StyleSpan(Typeface.NORMAL), 0, ssEmail.length(), 0); // 굵게
        ssPassword.setSpan(new StyleSpan(Typeface.NORMAL), 0, ssPassword.length(), 0);
        ssGuardian.setSpan(new StyleSpan(Typeface.NORMAL), 0, ssGuardian.length(), 0);

        // 스타일이 적용된 힌트 텍스트를 EditText에 설정
        EditText emailText = (EditText)findViewById(R.id.editEmail);
        EditText passwordText = (EditText)findViewById(R.id.editPassword);
        EditText guardianText = (EditText)findViewById(R.id.guardian_EditText);

        emailText.setHint(new SpannedString(ssEmail)); // 크기가 적용된 힌트 텍스트 설정
        passwordText.setHint(new SpannedString(ssPassword));
        guardianText.setHint(new SpannedString(ssGuardian));

        emailText.setHintTextColor(Color.parseColor("#555555")); // 색 변
        passwordText.setHintTextColor(Color.parseColor("#555555"));
        guardianText.setHintTextColor(Color.parseColor("#555555"));
    }

    public static String encryptECB(String key, String value) {
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);

            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(encrypted, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
