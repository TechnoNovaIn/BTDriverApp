package com.loginactivity.ftdriver.LoginModule;

public class ValidateOTPRequest {
    private String user_id;
    private String generatedOtp;

    public ValidateOTPRequest(String user_id, String generatedOtp) {
        this.user_id = user_id;
        this.generatedOtp = generatedOtp;
    }
}
