package com.mcuhq.simplebluetooth2.fragment;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.library.lookheartLibrary.controller.PeakController;
import com.mcuhq.simplebluetooth2.firebase.FirebaseMessagingService;
import com.mcuhq.simplebluetooth2.service.ForegroundService;
import com.mcuhq.simplebluetooth2.R;
import com.mcuhq.simplebluetooth2.server.RetrofitServerManager;
import com.mcuhq.simplebluetooth2.viewmodel.SharedViewModel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import com.library.lookheartLibrary.server.UserProfileManager;
import com.library.lookheartLibrary.server.UserProfile;

public class HomeFragment extends Fragment {

    /*ArrCount*/
    //region
    private int preArr = 0; // Yesterday ArrCnt
    private int currentArrCnt = 0; // 현재 시간 기준 발생한 비정상맥박 횟수
    private int previousArrCnt = 0; // 이전에 발생한 비정상맥박 횟수
    private int arrCnt;
    private int emergencyFlag = 0;
    private AlertDialog emergencyDialog;
    private View emergencyView;
    private int serverArrCnt = 0; // 화면 상단에 표시되는 arrCnt
    //endregion

    /*check*/
    //region
    private Boolean arrCheck = false; // 두번째 부터 알림 뜨게 하는 Flag
    private Boolean hourlyArrCheck = false; // 두번째 부터 알림 뜨게 하는 Flag
    private boolean HeartAttackCheck = false;
    //endregion

    /*Arr variables*/
    //region
    private ArrayList<String> arrList = new ArrayList<String>();
    private String arrIdx = "0";
    //endregion

    /*UI variables*/
    //region
    private static final int BPM_GRAPH_MAX = 250;
    private LinkedList<Double> bpmLastLines = new LinkedList<>(); // bpm graph
    private double realBPM;
    private double doubleTEMP;
    private int allstep;
    private double distance;
    private double dCal;
    private double dExeCal;
    //endregion

    /*SharedPreferences variables*/
    //region
    SharedPreferences userDetailsSharedPref;
    SharedPreferences.Editor userDetailsEditor;
    //endregion

    /*ServerTask variables*/
    //region
    RetrofitServerManager retrofitServerManager;
    private Boolean timeLoop = true;
    private Disposable disposable;
    //endregion

    /*Server 요청 시작 날짜 저장 변수*/
    //region
    String preDate;
    String bpmStartDate;
    String arrStartDate;
    String hourlyStartDate;
    //endregion

    /*Scheduler*/
    //region
    ScheduledExecutorService bpmScheduler = Executors.newScheduledThreadPool(1);
    ScheduledExecutorService bpmDataScheduler = Executors.newScheduledThreadPool(1);
    ScheduledExecutorService hourlyDataScheduler = Executors.newScheduledThreadPool(1);
    //endregion

    /*Notification variables*/
    //region
    private static final String PRIMARY_CHANNEL_ID = "LOOKHEART_GUARDIAN";
    private static final String PRIMARY_CHANNEL_NAME = "GUARDIAN";
    private int notificationId = 0;
    private boolean notificationsPermissionCheck;
    private NotificationManager notificationManager;
    //endregion

    /*Permission variables*/
    //region
    private ArrayList<String> permissions = new ArrayList<String>();
    private String[] PERMISSIONS;
    private ActivityResultContracts.RequestMultiplePermissions multiplePermissionsContract;
    private ActivityResultLauncher<String[]> multiplePermissionLauncher;
    //endregion

    /*Profile variables*/
    //region
    private String myEmail;
    private int sleep;
    private int wakeup;
    private int eCalBPM;
    //endregion

    /*currentTimeCheck() variables*/
    //region
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");
    private static final SimpleDateFormat YEAR_FORMAT = new SimpleDateFormat("yyyy");
    private static final SimpleDateFormat MONTH_FORMAT = new SimpleDateFormat("MM");
    private static final SimpleDateFormat DAY_FORMAT = new SimpleDateFormat("dd");
    private static final SimpleDateFormat HOUR_FORMAT = new SimpleDateFormat("HH");

    private final Handler currentDTHandler = new Handler(Looper.getMainLooper()); // DateTime Handler

    private Date currentDateTime;
    private String currentYear;
    private String currentMonth;
    private String currentDay;
    private String currentHour;
    private String currentDate;
    private String currentTime;
    private String targetDate; // currentDate 기준 다음 날을 저장하는 변수 (DB Select)
    //endregion

    /*TextView variables*/
    //region
    TextView arrStatus;
    TextView exerciseText;
    TextView preArrLabel;
    TextView restText;
    TextView sleepText;

    TextView arr_value;
    TextView bpm_value;
    TextView eCal_value;
    TextView preArr_value;
    TextView distance_value;
    TextView step_value;
    TextView temp_value;
    //endregion

    /*ImageView variables*/
    //region
    ImageView exerciseImg;
    ImageView filledHeart;
    ImageView restImg;
    ImageView sleepImg;
    //endregion

    /*LinearLayout variables*/
    //region
    LinearLayout exerciseBackground;
    LinearLayout restBackground;
    LinearLayout sleepBackground;
    //endregion

    /*Other layout variables*/
    //region
    private View view;
    private FrameLayout testButton;
    private LineChart chart;
    //endregion

    /*BroadcastReceiver variables*/
    //region
    private BroadcastReceiver messageReceiver;
    //endregion

    Intent serviceIntent; // Foreground Service var
    private SharedViewModel viewModel; // View Model var
    private AlertDialog onBackPressedDialog; // backPressed

    private PeakController peakCtrl = new PeakController();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // 뷰 초기화
        view = inflater.inflate(R.layout.fragment_home, container, false);

        // 사용자 정보 가져오기
        retrieveUserDetails();

        // UI 컴포넌트 초기화
        initializeUIComponents();

        // 데이터와 서비스 초기화
        initializeDataAndServices();

        // 이벤트 처리
        setEventListeners();

        return view;
    }

    private void retrieveUserDetails() {

        SharedPreferences emailSharedPreferences = safeGetActivity().getSharedPreferences("User", Context.MODE_PRIVATE);
        myEmail = emailSharedPreferences.getString("email", "null");

        viewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        viewModel.setEmail(myEmail);

        userDetailsSharedPref = safeGetActivity().getSharedPreferences(myEmail, Context.MODE_PRIVATE);
        userDetailsEditor = userDetailsSharedPref.edit();

        // previous data select start date
        bpmStartDate = userDetailsSharedPref.getString("bpmStartDate", "");
        arrStartDate = userDetailsSharedPref.getString("arrStartDate", "");
        hourlyStartDate = userDetailsSharedPref.getString("hourlyStartDate", "");

        preDate = userDetailsSharedPref.getString("preDate", "2023-01-01");
    }

    private void initializeUIComponents() {
        bpm_value = view.findViewById(R.id.bpm_Value);
        arr_value = view.findViewById(R.id.arr_value);
        eCal_value = view.findViewById(R.id.eCal_Value);
        step_value = view.findViewById(R.id.step_Value);
        temp_value = view.findViewById(R.id.temp_Value);
        distance_value = view.findViewById(R.id.distance_Value);

        preArr_value = view.findViewById(R.id.homeArrValue);
        preArrLabel = view.findViewById(R.id.preArrLabel);
        arrStatus = view.findViewById(R.id.homeArrStatus);

        filledHeart = view.findViewById(R.id.filledHeart);

        exerciseImg = view.findViewById(R.id.exerciseImg);
        exerciseText = view.findViewById(R.id.exerciseText);
        exerciseBackground = view.findViewById(R.id.exercise);

        restImg = view.findViewById(R.id.restImg);
        restText = view.findViewById(R.id.restText);
        restBackground = view.findViewById(R.id.rest);

        sleepImg = view.findViewById(R.id.sleepImg);
        sleepText = view.findViewById(R.id.sleepText);
        sleepBackground = view.findViewById(R.id.sleep);

        chart = view.findViewById(R.id.myChart);

        testButton = view.findViewById(R.id.testButton);
    }

    private void initializeDataAndServices() {
        retrofitServerManager = new RetrofitServerManager();

        currentTimeCheck();
        dateCalculate(1, true);

        getProfile();

        loadPreviousUserData();

        startTimeCheck();

        initVar();

        permissionsCheck();

//        startService();

        setFCM();
    }

    private void setEventListeners() {

        setOnBackPressed();

        testButton.setOnClickListener(v -> {
//            setFCM();
            getProfile();
        });
    }


    public String dateCalculate(int myDay, String startDate) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate date;

        date = LocalDate.parse(startDate, formatter);
        date = date.plusDays(myDay);

        startDate = date.format(formatter);

        date = LocalDate.parse(startDate, formatter);

        return String.valueOf(date);

    }

    public void loadPreviousUserData(){

        safeGetActivity().runOnUiThread(() -> Toast.makeText(getActivity(), getResources().getString(R.string.loadUserData), Toast.LENGTH_SHORT).show());

        Observable<Boolean> observable = Observable.create(emitter -> {
            try {
                loadPreviousBpmData(bpmStartDate);
                loadPreviousHourlyData(hourlyStartDate);
                loadPreviousArrData(arrStartDate);

                emitter.onNext(true);
                emitter.onComplete();

            } catch (Exception e) {
                emitter.onError(e);
            }
        });

        observable
                .subscribeOn(Schedulers.io()) // 작업을 IO 스레드에서 실행
                .observeOn(AndroidSchedulers.mainThread()) // 결과를 메인 스레드에서 받음
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable = d;
                    }

                    @Override
                    public void onNext(Boolean aBoolean) {
                        Log.i("onNext", "onNext");
                    }

                    @Override
                    public void onError(Throwable e) {
                        // 데이터 로딩 실패, 에러 메시지 표시
                        Toast.makeText(getActivity(), getResources().getString(R.string.failLoadUserData), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        Log.i("onComplete", "onComplete");

                        realTimeBPMLoop();
                        bpmLoop();
                        hourlyDataLoop();
                        refreshArrData();
                    }
                });
    }

    public void loadPreviousBpmData(String startDate){

        retrofitServerManager.getBpmData("BpmData", myEmail, startDate, targetDate, new RetrofitServerManager.DataCallback() {
            @Override
            public void getData(List<Map<String, String>> bpmData) {

                ArrayList<String> dataList = new ArrayList<>();

                for(int i = 1; i < bpmData.size() - 1; i++) {

                    String[] startDate = bpmData.get(i).get("time").split(" ");
                    String[] nextDate = bpmData.get(i + 1).get("time").split(" ");

                    if (startDate[0].equals(nextDate[0])) {
                        dataList.add(bpmData.get(i).toString());
                    } else {
                        writeBpmDataToFile(startDate[0], dataList);
                        dataList.clear();
                    }

                    if (i == bpmData.size() - 2){
                        dataList.clear();
                    }

                }
                safeGetActivity().runOnUiThread(() -> Toast.makeText(getActivity(), getResources().getString(R.string.sucBpmData), Toast.LENGTH_SHORT).show());
                userDetailsEditor.putString("bpmStartDate", currentDate);
                userDetailsEditor.apply();
            }

            @Override
            public void onFailure(Exception e) {
                safeGetActivity().runOnUiThread(() -> Toast.makeText(getActivity(), getResources().getString(R.string.failBpmData), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void writeBpmDataToFile(String startDate, ArrayList<String> dataList) {
        String[] spStartDate = startDate.split("-");
        String directoryName = "LOOKHEART/" + myEmail + "/" + spStartDate[0] + "/" + spStartDate[1] + "/" + spStartDate[2];
        File directory = new File(safeGetActivity().getFilesDir(), directoryName);

        if (!directory.exists()) {
            directory.mkdirs();
        }

        File file = new File(directory, "BpmData.csv");

        try (FileOutputStream fos = new FileOutputStream(file, false)) {
            StringBuilder csvBuilder = new StringBuilder();

            for (String dataStr : dataList) {
                dataStr = dataStr.substring(1, dataStr.length() - 1).trim(); // 중괄호, 공백 제거

                String[] data = dataStr.split(","); // 쉼표 기준 분리

                // 키, 값 저장
                Map<String, String> map = new HashMap<>();
                for (String key : data) {
                    String[] entry = key.split("=");

                    if ( entry.length < 2 )
                        map.put(entry[0].trim(), "null");
                    else
                        map.put(entry[0].trim(), entry[1].trim());

                }
                String time[] = map.get("time").split(" ");
                String csvData = time[1] + "," + map.get("utcOffset") + "," + map.get("bpm") + "," + map.get("temp") + "," + map.get("hrv") + "\n";
                csvBuilder.append(csvData);
            }

            fos.write(csvBuilder.toString().getBytes());
            fos.close();
        } catch (IOException e) {
            // handle exception
        }
    }

    public void loadPreviousHourlyData(String startDate){

        retrofitServerManager.getHourlyData("calandDistanceData", myEmail, startDate, targetDate, new RetrofitServerManager.HourlyDataCallback() {

            @Override
            public void hourlyData(List data) {

                ArrayList<String> dataList = new ArrayList<>();

                for(int i = 1; data.size() - 1 > i; i++) {
                    String stringData = (String) data.get(i);
                    String stringNextData = (String) data.get(i + 1);

                    String[] spStringData = stringData.split(",");
                    String[] nextSpStringData = stringNextData.split(",");

                    String[] startDate = spStringData[0].split(" ");
                    String[] nextDate = nextSpStringData[0].split(" ");

                    if (startDate[0].equals(nextDate[0])) {
                        String[] realData = stringData.split(",", 2);
                        dataList.add(realData[1]);
                    } else {
                        writeHourlyDataToFile(startDate[0], dataList);
                        dataList.clear();
                    }

                    // 마지막 값
                    if ( i == data.size() - 2) {
                        dataList.clear();
                    }
                }

                safeGetActivity().runOnUiThread(() -> Toast.makeText(getActivity(), getResources().getString(R.string.sucDailyData), Toast.LENGTH_SHORT).show());
                searchYesterdayArrCnt(currentDate);
                userDetailsEditor.putString("hourlyStartDate", currentDate);
                userDetailsEditor.apply();
            }

            @Override
            public void onFailure(Exception e) {
                safeGetActivity().runOnUiThread(() -> Toast.makeText(getActivity(), getResources().getString(R.string.failDailyData), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void writeHourlyDataToFile(String startDate, ArrayList<String> dataList) {
        String[] spStartDate = startDate.split("-");
        String directoryName = "LOOKHEART/" + myEmail + "/" + spStartDate[0] + "/" + spStartDate[1] + "/" + spStartDate[2];
        File directory = new File(safeGetActivity().getFilesDir(), directoryName);

        if (!directory.exists()) {
            directory.mkdirs();
        }

        File file = new File(directory, "CalAndDistanceData.csv");

        try (FileOutputStream fos = new FileOutputStream(file, false)) {
            StringBuilder csvBuilder = new StringBuilder();

            for (String dataStr : dataList) {

                String[] data = dataStr.split(","); // 쉼표 기준 분리

//                Log.e("data", Arrays.toString(data));

                // 시간, utcOffset, 걸음, 거리, 칼로리, 활동 칼로리, 비정상맥박
                String csvData = data[1] + "," + data[0] + "," + data[2] + "," + data[3] + "," + data[4] + "," + data[5] + "," + data[6] + "\n";
                csvBuilder.append(csvData);
            }

            fos.write(csvBuilder.toString().getBytes());
            fos.close();
        } catch (IOException e) {
            // handle exception
        }
    }

    public void loadPreviousArrData(String startDate){

        retrofitServerManager.getArrData("arrEcgData", arrIdx, myEmail, startDate, targetDate, new RetrofitServerManager.ArrDataCallback() {
            @Override
            public void getData(List result) {
                ArrayList<String> dataList = new ArrayList<>();

                for(int i = 1 ; result.size() - 1 > i ; i++){
//                    Log.e("result.get", "i : " + i + " data : " + result.get(i).toString());
                    String data = (String) result.get(i);
                    String nextData = (String) result.get(i + 1);

                    String[] spData = data.split(",");
                    String[] nextSpData = nextData.split(",");

                    String[] time = spData[0].split("\\|");
                    String[] nextTime = nextSpData[0].split("\\|");

                    if (time.length != nextTime.length) {
                        continue;
                    }

                    String[] date = time[1].split(" ");
                    String[] nextDate = nextTime[1].split(" ");

                    if (date[0].equals(nextDate[0])) {
                        String[] realData = data.split("\\|");
                        dataList.add(realData[2]);
                    } else {
                        writeArrDataToFile(date[0], dataList);
                        dataList.clear();
                    }

                    // 마지막 값
                    if ( i == result.size() - 2) {
                        dataList.clear();
                    }
                }

                if (isAdded())
                    safeGetActivity().runOnUiThread(() -> Toast.makeText(getActivity(), getResources().getString(R.string.sucArrData), Toast.LENGTH_SHORT).show());

                userDetailsEditor.putString("arrStartDate", currentDate);
                userDetailsEditor.apply();
            }

            @Override
            public void onFailure(Exception e) {
                safeGetActivity().runOnUiThread(() -> Toast.makeText(getActivity(), getResources().getString(R.string.failArrData), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void writeArrDataToFile(String startDate, ArrayList<String> dataList) {

        int arrCnt = 0;
        String arrType = null;

        String[] spStartDate = startDate.split("-");
        Log.e("spStartDate", Arrays.toString(spStartDate));
        String directoryName = "LOOKHEART/" + myEmail + "/" + spStartDate[0] + "/" + spStartDate[1] + "/" + spStartDate[2] + "/" + "arrEcgData";
        File directory = new File(safeGetActivity().getFilesDir(), directoryName);

        if (!directory.exists()) {
            directory.mkdirs();
        }

        for (int i = 0 ; dataList.size() > i ; i++ ){
            String data = dataList.get(i);
            String[] spData = data.split(",");

            if(!(spData.length > 500))
                continue;

//            Log.e("data", data);
            arrType = spData[3];

            String date;
            String ecgData = "";
            String filename = "";

            int startEcgIndex = 0;

            // start Ecg Index
            switch (arrType){
                case "arr":
                    startEcgIndex = data.indexOf("arr,") + 4;
                    break;
                case "fast":
                    startEcgIndex = data.indexOf("fast,") + 5;
                    break;
                case "slow":
                    startEcgIndex = data.indexOf("slow,") + 5;
                    break;
                case "irregular":
                    startEcgIndex = data.indexOf("irregular,") + 10;
                    break;
            }

            ecgData = data.substring(startEcgIndex);
            date = startDate + "_" + spData[0] + "_";

            try {

                arrCnt++;

                File file = new File(directory, "arrEcgData_" + date + arrCnt + ".csv");

                FileOutputStream fos = new FileOutputStream(file, false); // 'true' to append
                String csvData = spData[0] + "," + spData[1] + "," + spData[2] + "," + spData[3] + "," + ecgData;
                fos.write(csvData.getBytes());
                fos.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }


    public void bpmChart(String myBpm) {

        List<Entry> entries = new ArrayList<>();

        bpmLastLines.add(Double.valueOf(myBpm));
        if (bpmLastLines.size() > BPM_GRAPH_MAX) {  // 250개 라인만 저장
            bpmLastLines.removeFirst();
        }

        // 그래프에 들어갈 데이터 저장
        for (int i = 0; bpmLastLines.size() > i; i++) {
            entries.add(new Entry((float) i, bpmLastLines.get(i).floatValue()));
        }

        // 그래프 Set
        LineDataSet dataSet = new LineDataSet(entries, "BPM");
        dataSet.setDrawCircles(false);
        dataSet.setColor(Color.BLUE);
        dataSet.setLineWidth(1.0f);
        dataSet.setDrawValues(false);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        LineData bpmChartData = new LineData(dataSet);
        chart.setData(bpmChartData);  // 차트에 표시되는 데이터 설정
        chart.setNoDataText(""); // 데이터가 없는 경우 차트에 표시되는 텍스트 설정
        chart.getXAxis().setEnabled(false);   // x축 활성화(true)
        chart.getLegend().setTextSize(15f);  // 범례 텍스트 크기 설정("BPM" size)
        chart.getLegend().setTypeface(Typeface.DEFAULT_BOLD);
        chart.setVisibleXRangeMaximum(500);  // 한 번에 보여지는 x축 최대 값
        chart.getXAxis().setGranularity(1f); // 축의 최소 간격
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM); // x축 위치
        chart.getXAxis().setDrawGridLines(false);    // 축의 그리드 선
        chart.getDescription().setEnabled(false);    // 차트 설명

        chart.getAxisLeft().setAxisMaximum(200f); // y 축 최대값
        chart.getAxisLeft().setAxisMinimum(40f); // y 축 최소값
        chart.getAxisRight().setEnabled(false);  // 참조 반환
        chart.setDrawMarkers(false); // 값 마커
        chart.setDragEnabled(false);  // 드래그 기능
        chart.setPinchZoom(false);   // 줌 기능
        chart.setDoubleTapToZoomEnabled(false);  // 더블 탭 줌 기능
        chart.setHighlightPerDragEnabled(false); // 드래그 시 하이라이트
        chart.getLegend().setEnabled(false); // 라벨 제거
        chart.setTouchEnabled(false); // 터치 불가
        chart.getData().notifyDataChanged(); // 차트에게 데이터가 변경되었음을 알림
        chart.notifyDataSetChanged();    // 차트에게 데이터가 변경되었음을 알림
        chart.moveViewToX(0);    // 주어진 x값의 위치로 뷰 이동

        chart.invalidate(); // 차트 다시 그림

    }

    public void startTimeCheck() {
        currentDTHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                currentTimeCheck();
                currentDTHandler.postDelayed(this, 1000);
            }
        }, 1000);
    }


    public void dateCalculate(int myDay, boolean check) {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate date;

        if (check) {
            // tomorrow
            date = LocalDate.parse(currentDate, formatter);
            date = date.plusDays(myDay);

            targetDate = date.format(formatter);
            System.out.println(targetDate);

        } else {
            // yesterday
            date = LocalDate.parse(currentDate, formatter);
            date = date.minusDays(myDay);

            targetDate = date.format(formatter);
            System.out.println(targetDate);
        }
    }

    public void setUI() {
        eCal_value.setText((int) dExeCal + " " + getResources().getString(R.string.kcalValue));
        step_value.setText(allstep + " " + getResources().getString(R.string.stepValue2));
        distance_value.setText((String.format("%.3f", distance / 1000)) + " " + getResources().getString(R.string.distanceValue2));
        temp_value.setText(String.format("%.1f", doubleTEMP) + " " + getResources().getString(R.string.temperatureValue2));
    }

    public void setOnBackPressed() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                onBackPressedDialog = new AlertDialog.Builder(safeGetActivity())
                        .setTitle(getResources().getString(R.string.noti))
                        .setMessage(getResources().getString(R.string.exit))
                        .setNegativeButton(getResources().getString(R.string.rejectLogout), null)
                        .setPositiveButton(getResources().getString(R.string.exit2), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface arg0, int arg1) {
                                safeGetActivity().finish();
                            }
                        })
                        .create();

                onBackPressedDialog.show();

            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);
    }


    public void startService() {
        serviceIntent = new Intent(safeGetActivity(), ForegroundService.class);
        safeGetActivity().startService(serviceIntent);
    }

    public void stopService() {
        safeGetActivity().stopService(serviceIntent);
    }

    private void permissionsCheck() {

        boolean notiCheck = NotificationManagerCompat.from(getContext()).areNotificationsEnabled();
        Log.e("NotiCheck", String.valueOf(notiCheck));

        // 알림 채널 생성
        createNotificationChannel();

        // 배터리 최적화 기능 무시
        requestBatteryOptimizationsPermission();

        // 권한 요청
        requestPermissions();

    }

    private void requestBatteryOptimizationsPermission() {
        PowerManager pm = (PowerManager) safeGetActivity().getApplicationContext().getSystemService(safeGetActivity().POWER_SERVICE);
        boolean isWhiteListing = pm.isIgnoringBatteryOptimizations(safeGetActivity().getApplicationContext().getPackageName());
        if (!isWhiteListing) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + safeGetActivity().getApplicationContext().getPackageName()));
            startActivity(intent);
        }
    }

    private void requestPermissions() {
        // 권한 리스트 설정
        if (Build.VERSION.SDK_INT >= 33) {
            PERMISSIONS = new String[] {
                    Manifest.permission.POST_NOTIFICATIONS,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };
        } else {
            PERMISSIONS = new String[] {
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };
        }

        // 권한 요청
        multiplePermissionsContract = new ActivityResultContracts.RequestMultiplePermissions();
        multiplePermissionLauncher = registerForActivityResult(multiplePermissionsContract, isGranted -> {
            handlePermissionResult(isGranted);
        });

        // 필요 권한 확인 후 권한 요청
        askPermissions(multiplePermissionLauncher);
    }

    private void handlePermissionResult(Map<String, Boolean> isGranted) {
        if (isGranted.containsValue(false)) {
            handlePermissionDenied();
        } else {
            handlePermissionGranted();
        }
    }

    private void handlePermissionDenied() {
        Toast.makeText(safeGetActivity(), getResources().getString(R.string.permissionToast), Toast.LENGTH_SHORT).show();
        askPermissions(multiplePermissionLauncher);
    }

    private void handlePermissionGranted() {
        createNotificationChannel();
        notificationsPermissionCheck = true;
        userDetailsEditor.putBoolean("noti", notificationsPermissionCheck);
        startService();
    }

    private void askPermissions(ActivityResultLauncher<String[]> multiplePermissionLauncher) {
        if (!hasPermissions(PERMISSIONS)) {
            Log.d("PERMISSIONS", "Launching multiple contract permission launcher for ALL required permissions");
            multiplePermissionLauncher.launch(PERMISSIONS);
        } else {
            Log.d("PERMISSIONS", "All permissions are already granted");
            startService();
        }
    }

    private boolean hasPermissions(String[] permissions) {
        if (permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(safeGetActivity(), permission) != PackageManager.PERMISSION_GRANTED) {
                    Log.d("PERMISSIONS", "Permission is not granted: " + permission);
                    return false;
                }
                Log.d("PERMISSIONS", "Permission already granted: " + permission);
            }
            return true;
        }
        return false;
    }

    public void initVar() {
        distance = 0;
        dCal = 0;
        dExeCal = 0;
        allstep = 0;

        safeGetActivity().runOnUiThread(() -> arrStatus());

        bpm_value.setText(String.valueOf((int) realBPM));

        eCal_value.setText((int) dExeCal + " kcal");
        step_value.setText(allstep + " step");
        distance_value.setText((String.format("%.3f", distance / 1000)) + " km");
        arr_value.setText(Integer.toString(serverArrCnt));
        temp_value.setText(String.format("%.1f", doubleTEMP) + " °C");

    }

    public void currentTimeCheck() {

        long currentMillis = System.currentTimeMillis();
        currentDateTime = new Date(currentMillis);

        currentDate = DATE_FORMAT.format(currentDateTime);
        currentTime = TIME_FORMAT.format(currentDateTime);

        currentYear = YEAR_FORMAT.format(currentDateTime);
        currentMonth = MONTH_FORMAT.format(currentDateTime);
        currentDay = DAY_FORMAT.format(currentDateTime);

        currentHour = HOUR_FORMAT.format(currentDateTime);

        if (!currentDate.equals(preDate)){

            preDate = currentDate;
            serverArrCnt = 0;

            userDetailsEditor.putString("preDate", preDate);
            userDetailsEditor.putInt("serverArrCnt", 0);
            userDetailsEditor.apply();

        }

        // 날이 바뀌는 경우 targetDate 갱신
        if (currentDate.equals(targetDate))
            dateCalculate(1, true);

    }

    public void getProfile() {

        retrofitServerManager.getProfile(myEmail, new RetrofitServerManager.UserDataCallback() {
            @Override
            public void userData(UserProfile user) {

                eCalBPM = Integer.parseInt(user.getActivityBPM());
                sleep = Integer.parseInt(user.getSleepStart());
                wakeup = Integer.parseInt(user.getSleepEnd());

                peakCtrl.setEcgToPeakDataFlag(user.getEcgFlag().equals("0"));  // peak(0) : true, ecg(1) : false

            }

            @Override
            public void onFailure(Exception e) {
                Log.e("getProfile", "onFailure");
                e.printStackTrace();
            }
        });

    }

    public void createNotificationChannel() {

        // notification manager 생성
        notificationManager = (NotificationManager) safeGetActivity().getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel notificationChannel = new NotificationChannel(PRIMARY_CHANNEL_ID, PRIMARY_CHANNEL_NAME, notificationManager.IMPORTANCE_HIGH);

        notificationChannel.enableLights(true);
        notificationChannel.setLightColor(Color.RED);
        notificationChannel.enableVibration(true);
        notificationChannel.setDescription("Notification");
        notificationChannel.setSound(Uri.parse("android.resource://" + safeGetActivity().getPackageName() + "/" + R.raw.arrsound), new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build());
        notificationManager.createNotificationChannel(notificationChannel);
    }

    public void sendNotification(String noti) {
        // Builder 생성
        NotificationCompat.Builder notifyBuilder = getNotificationBuilder(noti);
        notificationManager.notify(notificationId, notifyBuilder.build());
        // alert
        safeGetActivity().runOnUiThread(() -> arrEvent(Integer.parseInt(noti)));
    }

    private NotificationCompat.Builder getNotificationBuilder(String noti) {
        NotificationCompat.Builder notifyBuilder = new NotificationCompat.Builder(safeGetActivity(), PRIMARY_CHANNEL_ID);

        switch (noti) {
            case "50":
                notifyBuilder.setContentTitle(getResources().getString(R.string.arrCnt50));
                notifyBuilder.setContentText(getResources().getString(R.string.arrCnt50Text));
                notifyBuilder.setSmallIcon(R.mipmap.ic_msl_guardian_round);
                return notifyBuilder;
            case "100":
                notifyBuilder.setContentTitle(getResources().getString(R.string.arrCnt100));
                notifyBuilder.setContentText(getResources().getString(R.string.arrCnt100Text));
                notifyBuilder.setSmallIcon(R.mipmap.ic_msl_guardian_round);
                return notifyBuilder;
            case "200":
                notifyBuilder.setContentTitle(getResources().getString(R.string.arrCnt200));
                notifyBuilder.setContentText(getResources().getString(R.string.arrCnt200Text));
                notifyBuilder.setSmallIcon(R.mipmap.ic_msl_guardian_round);
                return notifyBuilder;
            case "300":
                notifyBuilder.setContentTitle(getResources().getString(R.string.arrCnt300));
                notifyBuilder.setContentText(getResources().getString(R.string.arrCnt300Text));
                notifyBuilder.setSmallIcon(R.mipmap.ic_msl_guardian_round);
                return notifyBuilder;
            default:
                return notifyBuilder;
        }
    }

    private void heartAttackEvent(String data) {
        String[] time = data.split("\\|");
        String messageText = getResources().getString(R.string.occurrenceTime) + time[1] + "\n" + getResources().getString(R.string.occurrenceLocation) + "\n" + time[3];

        if (emergencyDialog != null) {
            setEmergencyAlertText(messageText);
            return;
        }

        // 시스템 사운드 재생
        MediaPlayer mediaPlayer = MediaPlayer.create(safeGetActivity(), R.raw.heartattacksound); // res/raw 폴더에 사운드 파일을 넣어주세요
        mediaPlayer.setLooping(true); // 반복
        mediaPlayer.start();

        // 알림 대화상자 표시
        AlertDialog.Builder builder = new AlertDialog.Builder(safeGetActivity(), R.style.AlertDialogTheme);

        emergencyView = LayoutInflater.from(safeGetActivity()).inflate(R.layout.heartattack_dialog, (LinearLayout) view.findViewById(R.id.layoutDialog));

        builder.setView(emergencyView);

        ((TextView) emergencyView.findViewById(R.id.heartattack_title)).setText(getResources().getString(R.string.emergency));
        ((TextView) emergencyView.findViewById(R.id.textMessage)).setText(getResources().getString(R.string.occurrenceTime) + time[1] + "\n" + getResources().getString(R.string.occurrenceLocation) + "\n" + time[3]);
        ((TextView) emergencyView.findViewById(R.id.btnOk)).setText(getResources().getString(R.string.ok));

        emergencyDialog = builder.create();

        emergencyView.findViewById(R.id.btnOk).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mediaPlayer.release();
                HeartAttackCheck = false;
                emergencyDialog.dismiss();
            }
        });

        // 다이얼로그 형태 지우기
        if (emergencyDialog.getWindow() != null) {
            emergencyDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        emergencyDialog.show();
    }

    private void setEmergencyAlertText(String message) {
        ((TextView) emergencyView.findViewById(R.id.heartattack_title)).setText(getResources().getString(R.string.emergency));
        ((TextView) emergencyView.findViewById(R.id.textMessage)).setText(message);
        ((TextView) emergencyView.findViewById(R.id.btnOk)).setText(getResources().getString(R.string.ok));
    }

    private void arrEvent(int arrCnt) {

        String title = null;
        String message = null;

        switch (arrCnt) {
            case 50:
                title = getResources().getString(R.string.arrCnt50);
                message = getResources().getString(R.string.arrCnt50Text);
                break;
            case 100:
                title = getResources().getString(R.string.arrCnt100);
                message = getResources().getString(R.string.arrCnt100Text);
                break;
            case 200:
                title = getResources().getString(R.string.arrCnt200);
                message = getResources().getString(R.string.arrCnt200Text);
                break;
            case 300:
                title = getResources().getString(R.string.arrCnt300);
                message = getResources().getString(R.string.arrCnt300Text);
                break;
        }

        // 알림 대화상자 표시
        AlertDialog.Builder builder = new AlertDialog.Builder(safeGetActivity(), R.style.AlertDialogTheme);

        View v = LayoutInflater.from(safeGetActivity()).inflate(R.layout.heartattack_dialog, (LinearLayout) view.findViewById(R.id.layoutDialog));

        builder.setView(v);

        ((TextView) v.findViewById(R.id.heartattack_title)).setText(title);
        ((TextView) v.findViewById(R.id.textMessage)).setText(message);
        ((TextView) v.findViewById(R.id.btnOk)).setText(getResources().getString(R.string.ok));

        AlertDialog alertDialog = builder.create();

        v.findViewById(R.id.btnOk).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        // 다이얼로그 형태 지우기
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        alertDialog.show();

    }

    private void hourlyArrEvent(int arrCnt) {

        String title = null;
        String message = null;

        int hourlyArrFlag = 0;
        int[] thresholds = { 10, 20, 30, 50 };

        for (int i = 0; i < thresholds.length; i++) {
            if (arrCnt >= thresholds[i])
                hourlyArrFlag = i + 1;
            else
                break; // 현재 임계값 보다 arrCnt 값이 작으면 반복문 탈출
        }

        switch (hourlyArrFlag) {
            case 1:
                title = getResources().getString(R.string.notiHourlyArr10);
                message = getResources().getString(R.string.notiHourlyArr10Text);
                break;
            case 2:
                title = getResources().getString(R.string.notiHourlyArr20);
                message = getResources().getString(R.string.notiHourlyArr20Text);
                break;
            case 3:
                title = getResources().getString(R.string.notiHourlyArr30);
                message = getResources().getString(R.string.notiHourlyArr30Text);
                break;
            case 4:
                title = getResources().getString(R.string.notiHourlyArr50);
                message = getResources().getString(R.string.notiHourlyArr50Text);
                break;
        }

        // 알림 대화 상자 표시
        AlertDialog.Builder builder = new AlertDialog.Builder(safeGetActivity(), R.style.AlertDialogTheme);

        View v = LayoutInflater.from(safeGetActivity()).inflate(R.layout.arr_dialog, (LinearLayout) view.findViewById(R.id.layoutDialog));

        builder.setView(v);

        ((TextView) v.findViewById(R.id.heartattack_title)).setText(title);
        ((TextView) v.findViewById(R.id.textMessage)).setText(message);
        ((TextView) v.findViewById(R.id.btnOk)).setText(getResources().getString(R.string.ok));

        AlertDialog alertDialog = builder.create();

        v.findViewById(R.id.btnOk).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });

        // 다이얼로그 형태 지우기
        if (alertDialog.getWindow() != null) {
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
        }

        alertDialog.show();

    }


    public void playSound(String message) {
        AudioManager audioManager = (AudioManager) safeGetActivity().getSystemService(Context.AUDIO_SERVICE);

        MediaPlayer mediaPlayer = null;
        String[] type = message.split(" ");

        switch (type[0]) {
            case "비정상맥박":
            case "I.H.R.":
            case "느린맥박":
            case "Slow":
            case "빠른맥박":
            case "Fast":
            case "연속적인":
            case "Heavy":
                mediaPlayer = MediaPlayer.create(safeGetActivity(), R.raw.arrsound);
                break;
        }


        if (audioManager != null && (audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT || audioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE)) {
            // 무음 또는 진동 모드일 때
            // mediaPlayer를 시작하지 않음
        } else {
            // 소리 모드일 때
            if (mediaPlayer != null) {
                mediaPlayer.start();
            }
        }

        MediaPlayer finalMediaPlayer = mediaPlayer;

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                if (finalMediaPlayer != null) {
                    finalMediaPlayer.release();
                }
            }
        }, 3000);  // 3초 (3000ms)
    }

    public void statusCheck(int myBpm) {
        int intCurrentHour = Integer.parseInt(currentHour);

        if (eCalBPM <= myBpm) {
            // 활동중
            exerciseBackground.setBackground(ContextCompat.getDrawable(safeGetActivity(), R.drawable.rest_round_press));
            exerciseText.setTextColor(Color.WHITE);
            exerciseImg.setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);

            restBackground.setBackgroundColor(Color.TRANSPARENT);
            restText.setTextColor(ContextCompat.getColor(safeGetActivity(), R.color.lightGray));
            restImg.setColorFilter(ContextCompat.getColor(safeGetActivity(), R.color.lightGray));

            sleepBackground.setBackgroundColor(Color.TRANSPARENT);
            sleepText.setTextColor(ContextCompat.getColor(safeGetActivity(), R.color.lightGray));
            sleepImg.setColorFilter(ContextCompat.getColor(safeGetActivity(), R.color.lightGray));

        } else if ((sleep < intCurrentHour || wakeup > intCurrentHour)) {
            // 수면중

            exerciseBackground.setBackgroundColor(Color.TRANSPARENT);
            exerciseText.setTextColor(ContextCompat.getColor(safeGetActivity(), R.color.lightGray));
            exerciseImg.setColorFilter(ContextCompat.getColor(safeGetActivity(), R.color.lightGray));

            restBackground.setBackgroundColor(Color.TRANSPARENT);
            restText.setTextColor(ContextCompat.getColor(safeGetActivity(), R.color.lightGray));
            restImg.setColorFilter(ContextCompat.getColor(safeGetActivity(), R.color.lightGray));

            sleepBackground.setBackground(ContextCompat.getDrawable(safeGetActivity(), R.drawable.rest_round_press));
            sleepText.setTextColor(Color.WHITE);
            sleepImg.setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);
        } else {
            // 휴식중

            exerciseBackground.setBackgroundColor(Color.TRANSPARENT);
            exerciseText.setTextColor(ContextCompat.getColor(safeGetActivity(), R.color.lightGray));
            exerciseImg.setColorFilter(ContextCompat.getColor(safeGetActivity(), R.color.lightGray));

            restBackground.setBackground(ContextCompat.getDrawable(safeGetActivity(), R.drawable.rest_round_press));
            restText.setTextColor(Color.WHITE);
            restImg.setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY);

            sleepBackground.setBackgroundColor(Color.TRANSPARENT);
            sleepText.setTextColor(ContextCompat.getColor(safeGetActivity(), R.color.lightGray));
            sleepImg.setColorFilter(ContextCompat.getColor(safeGetActivity(), R.color.lightGray));

        }
    }

    private FragmentActivity safeGetActivity() {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            Log.e("HomeFragment", "Fragment is not attached to an activity.");
            return null;
        }
        return activity;
    }

    public void arrStatus(){

        float progressPercentage = 0.0f;
        int intCurrentHour = Integer.parseInt(currentHour);

        if (preArr < serverArrCnt){
            // 어제보다 많음
            preArrLabel.setText(getResources().getString(R.string.moreArr));
            preArrLabel.setTextColor(ContextCompat.getColor(safeGetActivity(), R.color.myRed));
            preArr_value.setText(String.valueOf(serverArrCnt - preArr));
        }
        else {
            // 어제보다 적음
            preArrLabel.setText(getResources().getString(R.string.lessArr));
            preArrLabel.setTextColor(ContextCompat.getColor(safeGetActivity(), R.color.myBlue));
            preArr_value.setText(String.valueOf( preArr - serverArrCnt));
        }


        if (serverArrCnt < 50 ){
            arrStatus.setText(getResources().getString(R.string.arrStatusGood));

            float fill = (float) 100 / (serverArrCnt * 2) * 100;
            float percentageRelativeTo70 = 100 / fill * 100;
            progressPercentage = percentageRelativeTo70 / 100;

            filledHeart.setColorFilter(ContextCompat.getColor(safeGetActivity(), R.color.myLightGreen));
        }
        else if (serverArrCnt > 50 && serverArrCnt < 100){
            arrStatus.setText(getResources().getString(R.string.arrStatusCaution));

            float fill = (float) 100 / ((serverArrCnt - 50) * 2) * 100;
            float percentageRelativeTo70 = 100 / fill * 100;
            progressPercentage = percentageRelativeTo70 / 100;

            filledHeart.setColorFilter(ContextCompat.getColor(safeGetActivity(), R.color.myBlue));
        }
        else if (arrCnt >= 100){

            arrStatus.setText(getResources().getString(R.string.arrStatusWarning));
            arrStatus.setTextColor(ContextCompat.getColor(safeGetActivity(), R.color.white));

            float fill = (float) 100 / ((serverArrCnt - 100) * 2) * 100;
            float percentageRelativeTo70 = 100 / fill * 100;
            progressPercentage = percentageRelativeTo70 / 100;

            filledHeart.setColorFilter(ContextCompat.getColor(safeGetActivity(), R.color.myRed));
        }

        float finalProgressPercentage = progressPercentage;

        filledHeart.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                filledHeart.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                int originalHeight = filledHeart.getHeight();

                int clippedHeight = (int) (finalProgressPercentage * originalHeight);

                Rect clipBounds = new Rect(0, originalHeight - clippedHeight, filledHeart.getWidth(), originalHeight);
                filledHeart.setClipBounds(clipBounds);
            }
        });
    }

    public void searchYesterdayArrCnt(String currentDate){

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate date;

        date = LocalDate.parse(currentDate, formatter);
        date = date.minusDays(1);

        String yesterday = date.format(formatter);

        String[] spYesterday = yesterday.split("-");

        // 경로
        String directoryName = "LOOKHEART/" + myEmail + "/" + spYesterday[0] + "/" + spYesterday[1] + "/" + spYesterday[2];

        File directory = new File(getActivity().getFilesDir(), directoryName);

        // 파일 경로와 이름
        File file = new File(directory, "CalAndDistanceData.csv");

        if (file.exists()) {
            // 파일이 있는 경우

            try {
                // file read
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;

                while ((line = br.readLine()) != null) {
                    String[] columns = line.split(","); // 데이터 구분
                    preArr += Integer.parseInt(columns[6]);
                }

                br.close();

            }catch (IOException e) {
                e.printStackTrace();
            }

            Log.i("yesterday Arr file", "preArr : " + preArr);
        }

        else {
            // 파일이 없는 경우
            Log.i("yesterday Arr file", "not find");
            preArr = 0;
        }
    }

    private void realTimeBPMLoop(){
        bpmScheduler.scheduleAtFixedRate(() -> responseRealBPM(), 0, 1, TimeUnit.SECONDS);
    }

    private void bpmLoop(){
        bpmDataScheduler.scheduleAtFixedRate(() ->
                responseBpmData(currentDate, targetDate, currentYear, currentMonth, currentDay), 0, 10, TimeUnit.SECONDS);
    }

    private void hourlyDataLoop(){
        hourlyDataScheduler.scheduleAtFixedRate(() -> {
            responseHourlyData(currentDate, targetDate, currentYear, currentMonth, currentDay);
        }, 0, 10, TimeUnit.SECONDS);
    }

    public void responseRealBPM(){

        retrofitServerManager.getRealBPM(myEmail, new RetrofitServerManager.RealBpmCallback() {
            @Override
            public void getBpm(String bpm) {
                String[] myBpm = bpm.split("\n");

                Log.e("myBpm", Arrays.toString(myBpm));
                if (myBpm.length > 1 && isAdded()){
                    if (!myBpm[1].isEmpty() && safeGetActivity() != null){
                        // UI Update
                        safeGetActivity().runOnUiThread(() -> {
                            bpmChart(myBpm[1]);
                            bpm_value.setText(String.valueOf((myBpm[1])));
                            statusCheck(Integer.parseInt(myBpm[1]));
                        });
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("responseRealBPM", "responseRealBPM sendErr");
                e.printStackTrace();
            }
        });
    }

    public void responseBpmData(String currentDate, String targetDate, String year, String month, String day) {

        retrofitServerManager.getBpmData("BpmData", myEmail, currentDate, targetDate, new RetrofitServerManager.DataCallback() {
            @Override
            public void getData(List<Map<String, String>> bpmData) {

                if ( bpmData.size() > 0 && isAdded()){
                    try {
                        String directoryName = "LOOKHEART/" + myEmail + "/" + year + "/" + month + "/" + day;
                        File directory = new File(safeGetActivity().getFilesDir(), directoryName);

                        if (!directory.exists()) {
                            directory.mkdirs();
                        }

                        File file = new File(directory, "BpmData.csv");

                        // 시간, bpm, temp, hrv
                        FileOutputStream fos = new FileOutputStream(file, false); // 'true' to append

                        for (Map<String, String> data : bpmData) {
                            if (!data.get("time").equals("writetime")) {

                                String[] time = data.get("time").split(" ");
                                String csvData = time[1] + "," + data.get("utcOffset") + "," + data.get("bpm") + "," + data.get("temp") + "," + data.get("hrv") + "\n";
//                                Log.e("csvData", csvData);
                                doubleTEMP = Double.parseDouble(data.get("temp"));

                                fos.write(csvData.getBytes());
                            }
                        }

                        fos.close();

                    } catch (IOException e) {
                        Log.e("responseBpmData", "responseBpmData writeFileErr");
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("responseBpmData", "responseBpmData sendErr" );
                e.printStackTrace();
            }
        });
    }

    public void responseHourlyData(String currentDate, String targetDate, String currentYear, String currentMonth, String currentDay) {

        allstep = 0;
        distance = 0;
        dCal = 0;
        dExeCal = 0;

        retrofitServerManager.getHourlyData("calandDistanceData", myEmail, currentDate, targetDate, new RetrofitServerManager.HourlyDataCallback() {

            @Override
            public void hourlyData(List data) {
                if ( safeGetActivity() != null ){
                    try {
                        // 경로
                        String directoryName = "LOOKHEART/" + myEmail + "/" + currentYear + "/" + currentMonth + "/" + currentDay;
                        File directory = new File(safeGetActivity().getFilesDir(), directoryName);

                        // 디렉토리가 없는 경우 생성
                        if (!directory.exists()) {
                            directory.mkdirs();
                        }

                        // 파일 경로와 이름
                        File file = new File(directory, "CalAndDistanceData.csv");

                        FileOutputStream fos = new FileOutputStream(file, false); // 'true' to append

                        for (int i = 1; data.size() > i; i++) {
                            String spData = (String) data.get(i); // data
                            String[] result = spData.split(",");

                            allstep += Integer.parseInt(result[3]);
                            distance += Integer.parseInt(result[4]);
                            dCal += Integer.parseInt(result[5]);
                            dExeCal += Integer.parseInt(result[6]);

                            String csvData = result[2] + "," + result[1] + "," + result[3] + "," + result[4] + "," + result[5] + "," + result[6] + "," + result[7];
                            fos.write(csvData.getBytes());

                            // last hourly data
                            if(data.size() - 1 == i && hourlyArrCheck) {
                                currentArrCnt = Integer.parseInt(result[7]);

                                if(     shouldNotify(previousArrCnt, currentArrCnt, 10) ||
                                        shouldNotify(previousArrCnt, currentArrCnt, 20) ||
                                        shouldNotify(previousArrCnt, currentArrCnt, 30) ||
                                        shouldNotify(previousArrCnt, currentArrCnt, 50) ){

                                    safeGetActivity().runOnUiThread(() -> hourlyArrEvent(currentArrCnt));

                                }

                                previousArrCnt = currentArrCnt; // 현재 값을 이전 값으로 업데이트
                            }
                        }

                        safeGetActivity().runOnUiThread(() -> setUI());
                        fos.close();

                    } catch (IOException e) {
                        Log.e("responseHourlyData", "responseHourlyData writeErr" );
                        e.printStackTrace();
                    }
                    hourlyArrCheck = true;
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("responseHourlyData", "responseHourlyData sendErr" );
                e.printStackTrace();
            }
        });
    }

    public void refreshArrData() {

        retrofitServerManager.getArrData("arrEcgData", arrIdx, myEmail, currentDate, targetDate, new RetrofitServerManager.ArrDataCallback() {
            @Override
            public void getData(List<String> dataList) {

                String arrType = null;

                for(int i = 1 ; dataList.size() > i; i ++) {
                    String data = (String) dataList.get(i);
                    String[] spData = data.split(",");
                    String[] time = spData[0].split("\\|");
                    String[] spTime = time[1].split(" ");

                    if(!(spData.length > 500)) {
                        Log.e("HeartAttackCheck", data);
                        // 응급상황 발생
                        if (emergencyFlag == 1) // 두번째 호출부터 동작
                            heartAttackEvent(data);
                        arrIdx = time[0]; // 다음 Select idx 값 저장
                        continue;
                    }

                    Log.e("data", data);
                    String writeTime = time[2];
                    arrType = spData[3];

                    if (!spTime[0].equals(currentDate)) {
                        Log.e("return", spTime[0]);
                        return;
                    } else if (arrIdx.equals(time[0])) {
                        Log.e("return", time[0]);
                        return;
                    }

                    String date;
                    String ecgData = "";
                    int startEcgIndex = 0;

                    // start Ecg Index
                    switch (arrType){
                        case "arr":
                            startEcgIndex = data.indexOf("arr,") + 4;
                            break;
                        case "fast":
                            startEcgIndex = data.indexOf("fast,") + 5;
                            break;
                        case "slow":
                            startEcgIndex = data.indexOf("slow,") + 5;
                            break;
                        case "irregular":
                            startEcgIndex = data.indexOf("irregular,") + 10;
                            break;
                    }

                    ecgData = data.substring(startEcgIndex);

                    // last Data
                    if (i == dataList.size() - 1) {
                        String[] lastArrDate = spData[0].split("\\|");
                        arrIdx = lastArrDate[0];

                        Log.e("lastArrDate", Arrays.toString(lastArrDate));
                    }

                    // time
                    if (time.length > 2) {
                        spData[0] = writeTime;
                        date = currentDate + "_" +  writeTime + "_";
                    }
                    else {
                        date = currentDate + "_" + time[0] + "_";
                    }

                    try {

                        String directoryName = "LOOKHEART/" + myEmail + "/" + currentYear + "/" + currentMonth + "/" + currentDay + "/" + "arrEcgData";
                        File directory = new File(safeGetActivity().getFilesDir(), directoryName);

                        if (!directory.exists()) {
                            directory.mkdirs();
                        }

                        int copyServerArrCnt = serverArrCnt + 1;
                        File file = new File(directory, "arrEcgData_" + date + copyServerArrCnt + ".csv");

                        FileOutputStream fos = new FileOutputStream(file, false); // 'true' to append
                        String csvData = writeTime + "," + spData[1] + "," + spData[2] + "," + spData[3] + "," + ecgData;

                        // arrFragment Update
                        viewModel.addArrList(spData[0]);
                        arrList.add(csvData);
                        serverArrCnt++;

                        // UI Update
                        safeGetActivity().runOnUiThread(() -> {
                            safeGetActivity().runOnUiThread(() -> arrStatus());
                            safeGetActivity().runOnUiThread(() -> arr_value.setText(Integer.toString(serverArrCnt)));
                        });

                        fos.write(csvData.getBytes());
                        fos.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } // for

                // arr Noti And AlertDialog
                if (arrCheck){
                    switch (serverArrCnt){
                        case 50:
                        case 100:
                        case 200:
                        case 300:
                            notificationId = serverArrCnt;
                            sendNotification(String.valueOf(serverArrCnt));
                            notificationId = 0;

                            break;
                    }
                }

                arrCheck = true; // 두번째 반복부터 알림 뜨게 하는 Flag
                emergencyFlag = 1;
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("refreshArrData", "refreshArrData sendErr" );
                e.printStackTrace();
            }
        });
    }

    private boolean shouldNotify(int previous, int current, int threshold) {
        return previous < threshold && current >= threshold;
    }

    private void setFCM() {
        Boolean guardianCheck = userDetailsSharedPref.getBoolean("FCMCheck", false);

        if (!guardianCheck) {
            FirebaseMessagingService firebaseMessagingService = new FirebaseMessagingService();
            firebaseMessagingService.sendToken(getContext());
            userDetailsEditor.putBoolean("FCMCheck", true);
            userDetailsEditor.apply();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        timeLoop = false;
        currentDTHandler.removeCallbacksAndMessages(null);
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(messageReceiver);

        if (serviceIntent != null)
            stopService();

        if (disposable != null && !disposable.isDisposed())
            disposable.dispose();

        if (bpmScheduler != null && !bpmScheduler.isShutdown())
            bpmScheduler.shutdown();

        if (hourlyDataScheduler != null && !hourlyDataScheduler.isShutdown())
            hourlyDataScheduler.shutdown();

        if (bpmDataScheduler != null && !bpmDataScheduler.isShutdown())
            bpmDataScheduler.shutdown();

    }

    @Override
    public void onStart() {
        super.onStart();
        // 인텐트 필터를 사용하여 액션 리시버 등록
        IntentFilter filter = new IntentFilter("arr-event");
        messageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String message = intent.getStringExtra("message");
                Log.e("message", message);
                refreshArrData();
            }
        };
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(messageReceiver, filter);
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(messageReceiver);
    }

    @Override
    public void onResume(){
        super.onResume();
        Log.i("onResume", "onResume");

        if (arrCheck)
            refreshArrData(); // Arr Update
    }

    @Override
    public void onPause() {
        super.onPause();
        if (onBackPressedDialog != null && onBackPressedDialog.isShowing()) {
            onBackPressedDialog.dismiss();
        }
    }
}
