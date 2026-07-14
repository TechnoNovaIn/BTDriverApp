package com.loginactivity.ftdriver.LoginModule;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.loginactivity.ftdriver.ApiService.ApiService;
import com.loginactivity.ftdriver.ApiService.RetrofitAPIServiceInstance;
import com.loginactivity.ftdriver.MainActivity;
import com.loginactivity.ftdriver.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Objects;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GetLoginOTPActivity extends AppCompatActivity {
    private static final String TAG = "GetLoginOTPActivity";
    private TextInputEditText UsernameTextInputEditText;
    private Button getOtpButton;
    private SharedPreferences sharedPreferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_get_login_otpactivity);
        UsernameTextInputEditText = findViewById(R.id.enterUsernameOrPhoneNumber);

        getOtpButton = findViewById(R.id.btnGetOTP);
        getOtpButton.setOnClickListener(v -> {
            if (TextUtils.isEmpty(Objects.requireNonNull(UsernameTextInputEditText.getText()).toString())) {
                showAlertDialog("Validation Error","Username / Mobile Number Required");
            } else {
                getOtpButton.setEnabled(false);
                GetOTP();
            }
        });

        sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String token = sharedPreferences.getString("Token", "");

        if (!token.isEmpty()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

    }

    public void GetOTP() {

        String enteredUsername  = Objects.requireNonNull(UsernameTextInputEditText.getText()).toString();
        String Parameters = "Username Value : " + enteredUsername ;
        Log.i(TAG, "GetOTP - Parameters : " + Parameters);

        ProgressDialog progressDialog = new ProgressDialog(GetLoginOTPActivity.this);
        progressDialog.setMessage("Sending Login OTP, please wait...");
        progressDialog.show();
        progressDialog.setCancelable(false);

        try {
            ApiService apiService = RetrofitAPIServiceInstance.getApiService(this);
            Call<ResponseBody> call = apiService.postUserInfoData(enteredUsername);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                    getOtpButton.setEnabled(true);
                    progressDialog.dismiss();

                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            String successMessage = response.body().string();
                            JSONObject jsonObject = new JSONObject(successMessage);
                            Log.v(TAG, "Response : " + jsonObject);

                            if (jsonObject.has("Status") && jsonObject.getString("Status").equals("Success")) {

                                String message = jsonObject.getString("Message");
                                String phone = jsonObject.getString("PhoneNo");
                                String otp = jsonObject.getString("OTP");
                                String Username = jsonObject.getString("Name");
                                String userId = jsonObject.getString("UserId");
                                Log.d(TAG, "==========================");
                                Log.d(TAG, "User ID : " + userId);
                                Log.d(TAG, "User Name : " + Username);
                                Log.d(TAG, "Phone No : " + phone);
                                Log.d(TAG, "OTP Received : " + otp);
                                Log.d(TAG, "==========================");
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("UserId", userId);
                                editor.putString("Name", Username);
                                editor.putString("Phone_No", phone);
                                editor.putBoolean("isLoggedIn", true);
                                editor.apply();

                                Toast.makeText(GetLoginOTPActivity.this, message, Toast.LENGTH_LONG).show();
                                Log.v(TAG, "OTP: " + otp);
                                gotoSecondActivity();
                            } else {
                                String errorMessage = " ";
                                if (jsonObject.has("Message")) {
                                    errorMessage += jsonObject.getString("Message");
                                }
                                Log.e(TAG, "Error : " + errorMessage);
                                showAlertDialog("Failed",errorMessage);
                            }
                        } catch (IOException | JSONException e) {
                            Log.e(TAG, "Error : " + e.getMessage());
                            showAlertDialog("Failed","User Not Found");
                        }
                    } else {
                        Log.e(TAG,"Failed to get a successful response" + response.message());
                        showAlertDialog("Failed","User Not Registered");
                    }
                }
                @Override
                public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                    getOtpButton.setEnabled(true);
                    progressDialog.dismiss();

                    Log.e(TAG, "Failed to make a POST request", t);
                    showAlertDialog("Failed", "Network Error");
//                    EmailErrorReporter.sendPostMethodErrorEmail(GetLoginOTPActivity.this, "Get Login OTP Activity - API Error Notification", "Get OTP ", enteredUsername, Parameters, null, t.getMessage());
                }
            });
        } catch (Exception e) {
            getOtpButton.setEnabled(true);
            progressDialog.dismiss();

            Log.e(TAG, "API Error : " + e.getMessage());
            showAlertDialog("Failed","API Error");
//            EmailErrorReporter.sendPostMethodErrorEmail(GetLoginOTPActivity.this, "Get Login OTP Activity - API Error Notification", "Get OTP ", enteredUsername, Parameters, null, e.getMessage());
        }
    }

    public void gotoSecondActivity(){
        Intent intent = new Intent(this, ValidateLoginOTPActivity.class);
        startActivity(intent);
    }

    private void showAlertDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(GetLoginOTPActivity.this);
        builder.setTitle(title)
                .setMessage(message)
                .setIcon(R.drawable.baseline_error_24)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }
}
