package com.loginactivity.ftdriver.ApiService;

import com.loginactivity.ftdriver.LoginModule.ValidateOTPRequest;
import com.loginactivity.ftdriver.model.BTVehicle;
import com.loginactivity.ftdriver.model.Driver;
import com.loginactivity.ftdriver.model.EwayBillExpireDate;
import com.loginactivity.ftdriver.model.InsertTripModel;
import com.loginactivity.ftdriver.model.SearchVehicleDocumentsResponseModel;
import com.loginactivity.ftdriver.model.ShipmentDetailsModel;
import com.loginactivity.ftdriver.model.Unload;
import com.loginactivity.ftdriver.model.UnloadDetails;
import com.loginactivity.ftdriver.model.VehicleDocument;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    @GET("BTVehicles")
    Call<List<BTVehicle>> getBTVehicles();

    @GET("OTPAuthDriver")
    Call<ResponseBody> postUserInfoData(@Query("username_mobileno") String username_mobileno);

    @POST("OTPAuthDriver/VerifyOtp")
    Call<ResponseBody> postValidateData(@Body ValidateOTPRequest validateOTPRequest);

    @GET("Driver")
    Call<List<Driver>> getDrivers();

    @GET("Unload")
    Call<List<Unload>> getUnload();

    @POST("VehicleStatus")
    Call<ResponseBody> searchVehicleStatusDetails(@Query("driver") String driverDetails);

    @POST("VehicleStatus/GetUnloadForShipmentType")
    Call<List<UnloadDetails>> getUnloadForShipment(@Query("PartyName") String PartyName, @Query("ShipmentType") String ShipmentType);
    @POST("GetVehicleDocuments/GetVehicleDocumentsExpireDate")
    Call<List<VehicleDocument>> getVehicleDocuments(@Query("VehicleID") int vehicleID);

    @POST("GetOwnVehicleWeb/GetVehicleDocumentsForAPP")
    Call<List<SearchVehicleDocumentsResponseModel>> postVehicleDocumentData(
            @Query("PropertyID") String PropertyID,
            @Query("DocumentType") String DocumentType
    );

    @GET("DBConnectionString/GetAmazons3CredentialsS3AccessKey")
    Call<ResponseBody> getAmazonS3AccessKey();

    @GET("DBConnectionString/GetAmazons3CredentialsS3SecretKey")
    Call<ResponseBody> getAmazonS3SecretKey();


    @POST("VehicleStatus/GetEwayBillExpireDateForVehicle")
    Call<List<EwayBillExpireDate>> getEwayBillExpireDate(@Query("VehicleNo") String VehicleNo);

    @POST("VehicleDetails/GetLast10ShipmentDetailsOnVehicle")
    Call<List<ShipmentDetailsModel>> getShipmentDetails(
            @Query("VehicleID") int vehicleId
    );


    @POST("DriverPayment/InsertVehicleTripDetailsforPODUpload")
    Call<ResponseBody> InsertVehicleTripDetails(@Body InsertTripModel req);

}
