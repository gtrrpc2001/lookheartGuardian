package com.mcuhq.simplebluetooth2.firebase;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.RemoteMessage;
import com.mcuhq.simplebluetooth2.R;

import com.mcuhq.simplebluetooth2.server.RetrofitServerManager;

public class FirebaseMessagingService extends com.google.firebase.messaging.FirebaseMessagingService {

    private static final String CHANNEL_ID = "high_importance_channel";
    private static final CharSequence CHANNEL_NAME = "FIREBASE";

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);

        if (isFirstToken(getApplicationContext()))
            setTokenAsReceived(getApplicationContext());
        else {
            Context context = getApplicationContext();
            fetchFCMToken(token, context);
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        Intent intent = new Intent("arr-event");
        intent.putExtra("message", remoteMessage.getNotification().getTitle());

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        //수신한 메시지를 처리
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());

        NotificationCompat.Builder builder = null;
        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            //채널 생성
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH // 중요도
            );
            channel.setDescription("This channel is used for important notifications."); // 채널 설명
            channel.setSound(Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.arrsound), new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            notificationManager.createNotificationChannel(channel);
        }

        builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID);

        String title = remoteMessage.getNotification().getTitle();
        String body = remoteMessage.getNotification().getBody();

        builder.setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_MAX) // 최대 우선순위
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE) // 소리와 진동 설정
                .setSmallIcon(R.mipmap.ic_launcher_round);

        Notification notification = builder.build();
        notificationManager.notify(1610, notification);
    }

    public void sendToken(Context context) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w("FCM", "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        String token = task.getResult();

                        fetchFCMToken(token, context);
                    }
                });
    }

    private void fetchFCMToken(String token, Context context) {
        RetrofitServerManager retrofitServerManager = new RetrofitServerManager();

        SharedPreferences emailSharedPreferences = context.getSharedPreferences("User", Context.MODE_PRIVATE);
        String email = emailSharedPreferences.getString("email", null);

        SharedPreferences userSp = context.getSharedPreferences(email, Context.MODE_PRIVATE);
        String pw = userSp.getString("password", null);
        String guardian = userSp.getString("guardian", null);

        retrofitServerManager.tokenTask(email, pw, guardian, token, new RetrofitServerManager.ServerTaskCallback() {

            @Override
            public void onSuccess(String result) {
                if (result.toLowerCase().contains("true"))
                    Log.i("fetchFCMToken", "토큰 전송 성공");
                else
                    Log.i("fetchFCMToken", "토큰 전송 실패");
            }

            @Override
            public void onFailure(Exception e) {
                e.printStackTrace();
                Log.e("fetchFCMToken", "서버 응답 없음");
            }

        });
    }

    private boolean isFirstToken(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("FCM_TOKEN", MODE_PRIVATE);
        return sharedPreferences.getBoolean("isFirstToken", true);
    }

    private void setTokenAsReceived(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("FCM_TOKEN", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isFirstToken", false);
        editor.apply();
    }

}