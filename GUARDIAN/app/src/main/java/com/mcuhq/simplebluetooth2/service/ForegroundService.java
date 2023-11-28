package com.mcuhq.simplebluetooth2.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.mcuhq.simplebluetooth2.R;
import com.mcuhq.simplebluetooth2.activity.Activity_Main;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ForegroundService extends Service {

    private Executor executor = Executors.newSingleThreadExecutor(); // 백그라운드에서 코드를 실행할 Executor
    private volatile boolean isRunning = true; // 서비스가 실행 중인지 확인하는 플래그

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startBackgroundTask();

        initializeNotification(); // 포그라운드 생성
        return START_NOT_STICKY;
    }

    private void startBackgroundTask() {
        executor.execute(() -> {
            while (isRunning) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    // Thread interrupted
                }
            }
        });
    }

    public void initializeNotification(){
        String channelId = "LookHeartGuardianFS"; // 채널 ID 정의
        String channelName = "Foreground Service"; // 채널 이름 정의
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);
        manager.createNotificationChannel(channel);

        // Notification 생성
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId);
        builder.setSmallIcon(R.mipmap.ic_launcher_round);
        builder.setContentTitle("LOOKHEART GUARDIAN");
        builder.setContentText(getResources().getString(R.string.serviceRunning));
        builder.setOngoing(true);
        builder.setWhen(0);
        builder.setShowWhen(false);

        // 클릭 시 이동할 액티비티 설정
        Intent notificationIntent = new Intent(this, Activity_Main.class);
        // 현재 작업 스택에 있는 액티비티 인스턴스를 가져오거나, 없으면 새로 만듭니다.
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        builder.setContentIntent(pendingIntent);

        // 포그라운드 서비스 시작
        Notification notification = builder.build();
        startForeground(777, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false; // 백그라운드 작업 중지
        stopSelf();
    }
}
