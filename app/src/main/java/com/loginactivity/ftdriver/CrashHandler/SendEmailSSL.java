package com.loginactivity.ftdriver.CrashHandler;

import android.os.StrictMode;
import android.util.Log;

import java.util.Properties;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public class SendEmailSSL {
    private static final String TAG = "SendEmailSSL";
    private final String username = "ppdbalaji@gmail.com";
    private final String password = "uxrsfstcesqhkjca";

    public void sendEmail(String subject, String body) {
        Properties prop = new Properties();
        prop.put("mail.smtp.host", "smtp.gmail.com");
        prop.put("mail.smtp.port", "465");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.socketFactory.port", "465");
        prop.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

        Session session = Session.getInstance(prop,
                new jakarta.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                });

        try {
            Log.v(TAG, "Trying to send email");
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(
                    Message.RecipientType.TO,
//                    InternetAddress.parse("mkichidi@gmail.com, rekharekha14270@gmail.com")
                    InternetAddress.parse("rekharekha14270@gmail.com")
            );

            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);

            Log.v(TAG, "Email sent successfully");

        } catch (MessagingException e) {
            Log.e(TAG, "Error sending email", e);
        }
    }
}
