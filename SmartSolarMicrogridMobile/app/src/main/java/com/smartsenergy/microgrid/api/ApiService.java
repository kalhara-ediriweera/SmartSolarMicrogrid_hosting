package com.smartsenergy.microgrid.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {
    @POST("auth/login") Call<JsonObject> login(@Body JsonObject body);
    @POST("prosumers/register") Call<JsonObject> register(@Body JsonObject body);
    @GET("prosumers/{nic}") Call<JsonObject> getProsumer(@Path("nic") String nic);
    @PUT("prosumers/{nic}") Call<JsonObject> updateProsumer(@Path("nic") String nic, @Body JsonObject body);
    @POST("prosumers/{nic}/deactivation-request") Call<JsonObject> requestDeactivation(@Path("nic") String nic);

    @GET("dashboard/prosumer") Call<JsonObject> prosumerDashboard(@Query("nic") String nic);
    @GET("dashboard/operator") Call<JsonObject> operatorDashboard();

    @GET("microgrid-nodes") Call<JsonElement> nodes(@Query("activeOnly") boolean activeOnly);
    @GET("microgrid-nodes/{id}") Call<JsonObject> node(@Path("id") String id);
    @GET("energy-slots") Call<JsonElement> slots(@Query("nodeId") String nodeId, @Query("availableOnly") boolean availableOnly);

    @POST("reservations") Call<JsonObject> createReservation(@Query("nic") String nic, @Body JsonObject body);
    @GET("reservations/mine") Call<JsonElement> myReservations(@Query("nic") String nic, @Query("search") String search, @Query("status") String status);
    @GET("reservations/{id}") Call<JsonObject> reservation(@Path("id") String id);
    @PUT("reservations/{id}") Call<JsonObject> updateReservation(@Path("id") String id, @Query("nic") String nic, @Body JsonObject body);
    @DELETE("reservations/{id}") Call<JsonObject> cancelReservation(@Path("id") String id, @Query("nic") String nic);
    @PUT("reservations/{id}/approve") Call<JsonObject> approveReservation(@Path("id") String id);

    @POST("qr/generate/{reservationId}") Call<JsonObject> generateQr(@Path("reservationId") String reservationId);
    @POST("qr/verify") Call<JsonObject> verifyQr(@Body JsonObject body);
    @POST("qr/complete") Call<JsonObject> completeQr(@Body JsonObject body);

    @GET("energy-slots") Call<JsonElement> operatorSlots(@Query("nodeId") String nodeId, @Query("availableOnly") boolean availableOnly);
    @PUT("energy-slots/{id}/availability") Call<JsonObject> updateAvailability(@Path("id") String id, @Query("availableCapacityKwh") double capacity);
    @GET("reservations/status/{status}") Call<JsonElement> reservationsByStatus(@Path("status") String status);
    @GET("reservations/all") Call<JsonElement> allReservations();
}
