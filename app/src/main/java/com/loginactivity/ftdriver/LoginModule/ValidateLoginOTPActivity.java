package com.loginactivity.ftdriver.LoginModule;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;

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

public class ValidateLoginOTPActivity extends AppCompatActivity {

    private static final String TAG = "ValidateLoginOTPActivity";
    private String Username, UserID;
    private TextInputEditText OTPTextInputEditText;
    private Button validateOTPButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_validate_login_otp);

        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        Username = sharedPreferences.getString("Name", "");
        UserID = sharedPreferences.getString("UserId", "");
        Log.i( TAG,"Validate Login OTP : SharedPreferences UserID = " + UserID);
//        if (UserID.isEmpty()) {
//            Intent intent = new Intent(this, LoginWithPasswordActivity.class);
//            startActivity(intent);
//            return;
//        }

        if (UserID.isEmpty()) {
           showAlertDialog("Failed","Check the User Mobile Number again");
        }

        OTPTextInputEditText = findViewById(R.id.edEnteredOTP);

        validateOTPButton = findViewById(R.id.btnLogin);
        validateOTPButton.setOnClickListener(view -> {
            if (TextUtils.isEmpty(Objects.requireNonNull(OTPTextInputEditText.getText()).toString())) {
                showAlertDialog("Validation Error", "Please enter OTP");
            } else {
                validateOTPButton.setEnabled(false);
                login();
            }
        });
    }

    public void login() {

        String enteredOTP = Objects.requireNonNull(OTPTextInputEditText.getText()).toString().trim();

        String Parameters = "\n" + "OTP Value: " + enteredOTP + "\n" + "UserID Value: " + UserID ;
        Log.i(TAG, "Parameters : " + Parameters);

        ProgressDialog progressDialog = new ProgressDialog(ValidateLoginOTPActivity.this);
        progressDialog.setMessage("Logging in with OTP, please wait...");
        progressDialog.show();
        progressDialog.setCancelable(false);

        try {
            ApiService apiService = RetrofitAPIServiceInstance.getApiService();
            ValidateOTPRequest validateOTPRequest = new ValidateOTPRequest(UserID, enteredOTP);
            Call<ResponseBody> call = apiService.postValidateData(validateOTPRequest);
            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                    validateOTPButton.setEnabled(true);
                    progressDialog.dismiss();

                    if (response.isSuccessful() && response.body() != null) {
                        try (ResponseBody responseBody = response.body()) {
                            String successMessage = responseBody.string();
                            Log.v(TAG, "Response: " + successMessage);

                            JSONObject jsonResponse = new JSONObject(successMessage);
                            String status = jsonResponse.getString("Status");
                            String apiUserId = jsonResponse.getString("UserId");
                            String apiName = jsonResponse.getString("Name");
                            String apiPhone_No = jsonResponse.getString("PhoneNo");


                            Log.w(TAG, "User ID Value: " + apiUserId + ", User Name Value: " + apiName +  ", Phone Number Value: " + apiPhone_No);

                            if (status.equals("Success")) {
                                gotoSecondActivity();
                            } else {
                                String errorMessage = jsonResponse.getString("Message");
                                Log.e(TAG,"Error: " + errorMessage);
                                showAlertDialog("Failed", errorMessage);
                            }
                        } catch (IOException | JSONException e) {
                            Log.e(TAG,"IOException: " + e.getMessage(), e);
                            showAlertDialog("Failed","Invalid OTP");
                        }
                    } else {
                        String errorBodyMessage = "";
                        try {
                            if (response.errorBody() != null) {
                                errorBodyMessage = response.errorBody().string();
                            }
                        } catch (IOException e) {
                            errorBodyMessage = "Error parsing error body";
                        }
                        Log.e(TAG, "Error Response: " + errorBodyMessage);
                        showAlertDialog("Failed",errorBodyMessage);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                    validateOTPButton.setEnabled(true);
                    progressDialog.dismiss();

                    Log.e(TAG,"Failed to make a POST request", t);
                    showAlertDialog("Failed","Network Error");
//                    EmailErrorReporter.sendPostMethodErrorEmail(ValidateLoginOTPActivity.this, "Validate Login OTP Activity - API Error Notification", "Validate OTP ", Username, Parameters, null, t.getMessage());
                }
            });

        } catch (Exception e) {
            validateOTPButton.setEnabled(true);
            progressDialog.dismiss();

            Log.e(TAG,"Exception: " + e.getMessage());
            showAlertDialog("Failed","API Error");
//            EmailErrorReporter.sendPostMethodErrorEmail(ValidateLoginOTPActivity.this, "Validate Login OTP Activity - API Error Notification", "Validate OTP ", Username, Parameters, null, e.getMessage());
        }
    }

    public void gotoSecondActivity(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private void showAlertDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ValidateLoginOTPActivity.this);
        builder.setTitle(title)
                .setMessage(message)
                .setIcon(R.drawable.baseline_error_24)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }
}
