package com.mcuhq.simplebluetooth2.server;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

import com.library.lookheartLibrary.server.UserProfile;

public interface RetrofitService {

    // checkLogin
    @GET("msl/CheckLogin")
    Call<String> checkLogin(@Query("empid") String empid, @Query("pw") String pw, @Query("phone") String phone);

    @GET("msl/CheckLogin")
    Call<String> setToken(@Query("empid") String empid, @Query("pw") String pw, @Query("phone") String phone, @Query("token") String token);

    // Real-Time Bpm
    @GET("mslLast/Last")
    Call<String> getRealBPM(@Query("eq") String eq);

    // BpmData
    @GET("mslbpm/api_getdata")
    Call<String> getBpmData(@Query("eq") String eq, @Query("startDate") String startDate, @Query("endDate") String endDate);

    // Arr
    @GET("mslecgarr/test")
    Call<String> getArrData(@Query("idx") String idx, @Query("eq") String eq, @Query("startDate") String startDate, @Query("endDate") String endDate);

    // CalandDistance
    @GET("mslecgday/day")
    Call<String> getHourlyData(@Query("eq") String eq, @Query("startDate") String startDate, @Query("endDate") String endDate);

    // Profile
    @GET("msl/Profile")
    Call<List<UserProfile>> getProfileData(@Query("empid") String empid);

}
