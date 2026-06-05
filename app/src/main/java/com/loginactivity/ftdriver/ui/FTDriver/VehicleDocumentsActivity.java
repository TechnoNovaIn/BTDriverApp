package com.loginactivity.ftdriver.ui.FTDriver;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.S3Object;
import com.loginactivity.ftdriver.ApiService.ApiService;
import com.loginactivity.ftdriver.ApiService.RetrofitAPIServiceInstance;
import com.loginactivity.ftdriver.CacheMemory.ApiServiceCacheMemoryWrapper;
import com.loginactivity.ftdriver.R;
import com.loginactivity.ftdriver.model.SearchVehicleDocumentsResponseModel;
import com.loginactivity.ftdriver.model.VehicleDocumentType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VehicleDocumentsActivity extends AppCompatActivity {
    private static final String TAG = "VehicleDocumentsFragment";


    private final List<Boolean> selectedVehicleDocumentNames = new ArrayList<>();
    private final List<Boolean> selectedVehicleNumber = new ArrayList<>();
    private final List<Integer> selectedVehicleDocumentIDs = new ArrayList<>();
    private final List<Integer> selectedVehicleId = new ArrayList<>();
    private final List<String> selectedItemNames = new ArrayList<>();
    private final List<String> selectedVehicle = new ArrayList<>();
    private final Map<String, Integer> BTVehicleDocumentIDs = new HashMap<>();
    private boolean isLoadingBTVehicleNumbers = false;
    private boolean isLoadingBTVehicleDocumentType = false;
    private AmazonS3Client amazonS3Client;
    private ApiServiceCacheMemoryWrapper apiServiceCacheMemoryWrapper;
    private Button submitButton, downloadAllButton, shareButton;
//    private List<BTVehicle> BTVehicleList = new ArrayList<>();
    private  String accessKey,secretKey;
    private List<VehicleDocumentType> BTVehicleDocumentList = new ArrayList<>();
    private String UserName;
    private TableLayout ResponseTable;
    private TextView BTVehicleNumberTextView, BTVehicleDocumentTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_vehicle_documents);

        ResponseTable = findViewById(R.id.ResponseTab);

        fetchAWSAccessKey();
        fetchAWSSecretKey();

        GetTableData();

        shareButton = findViewById(R.id.shareButton);
    }


    private void GetTableData() {
//        downloadAllButton.setVisibility(View.GONE);
        ResponseTable.removeAllViews();

        ProgressDialog progressDialog = new ProgressDialog(VehicleDocumentsActivity.this);
        progressDialog.setMessage("Loading....");
        progressDialog.setCancelable(false);
        progressDialog.show();

//        submitButton.setEnabled(true);

        String VehicleID = TextUtils.join(",", selectedVehicleId);
//        String DocumentID = TextUtils.join(",", selectedVehicleDocumentIDs);

        int vehicleId = getIntent().getIntExtra("vehicleId", -1);
        Log.v("vehicleID","vehicleID"+String.valueOf(vehicleId));

        String Parameters = "\n" + "Vehicle ID Value : " + VehicleID +
                "\n" + "Document ID Value : ";

        Log.i(TAG, "GetDocuments - Parameters : " + Parameters);

        try {
            ApiService apiService = RetrofitAPIServiceInstance.getApiService();
            Call<List<SearchVehicleDocumentsResponseModel>> call =
                    apiService.postVehicleDocumentData(String.valueOf(vehicleId), "");

            call.enqueue(new Callback<List<SearchVehicleDocumentsResponseModel>>() {
                @Override
                public void onResponse(@NonNull Call<List<SearchVehicleDocumentsResponseModel>> call,
                                       @NonNull Response<List<SearchVehicleDocumentsResponseModel>> response) {
//                    submitButton.setEnabled(true);
                    progressDialog.dismiss();

                    if (response.isSuccessful()) {
                        List<SearchVehicleDocumentsResponseModel> apiResponseList = response.body();
                        if (apiResponseList != null && !apiResponseList.isEmpty()) {
                            displayResponseInTable(apiResponseList);
                            // updateDownloadButtonVisibility();
                        } else {
                            showFailureDialog("Alert", "Data not available");
                        }
                    } else {
                        try {
                            assert response.errorBody() != null;
                            Log.e(TAG, "API Error: " + response.errorBody().string());
                        } catch (Exception e) {
                            Log.e(TAG, "API Error: Error parsing error body - " + e.getMessage());
                        }
                        showFailureDialog("Alert", "Failed to fetch data");
                    }
                }

                @Override
                public void onFailure(@NonNull Call<List<SearchVehicleDocumentsResponseModel>> call,
                                      @NonNull Throwable t) {
                    progressDialog.dismiss();
//                    submitButton.setEnabled(true);
                    Log.e(TAG, "API Failure: " + t.getMessage());
                    showFailureDialog("Alert", "Failed to fetch data");
                }
            });

        } catch (Exception e) {
//            submitButton.setEnabled(true);
            progressDialog.dismiss();

            Log.e(TAG, "API Exception: " + e.getMessage());
            showFailureDialog("Alert", "Failed to fetch data");
        }
    }


    private void displayResponseInTable(List<SearchVehicleDocumentsResponseModel> apiResponseList) {
        TableLayout responseTable = findViewById(R.id.ResponseTab);
        responseTable.removeAllViews();

        List<SearchVehicleDocumentsResponseModel> selectedDocuments = new ArrayList<>();

        shareButton.setOnClickListener(v -> {
            try {
                shareDocuments(selectedDocuments);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        shareButton.setVisibility(View.GONE);

        TableRow headerRow = new TableRow(VehicleDocumentsActivity.this);
        addHeadingCell(headerRow, "Select", Color.parseColor("#FFD700"));
        addHeadingCell(headerRow, "Vehicle Number", Color.parseColor("#FFD700"));
        addHeadingCell(headerRow, "Document Type", Color.parseColor("#FFD700"));
        addHeadingCell(headerRow, "Document Name", Color.parseColor("#FFD700"));
        addHeadingCell(headerRow, "Year", Color.parseColor("#FFD700"));
        addHeadingCell(headerRow, "View", Color.parseColor("#FFD700"));
        addHeadingCell(headerRow, "Download", Color.parseColor("#FFD700"));
        responseTable.addView(headerRow);

        for (SearchVehicleDocumentsResponseModel response : apiResponseList) {
            TableRow dataRow = new TableRow(VehicleDocumentsActivity.this);

            CheckBox checkBox = new CheckBox(VehicleDocumentsActivity.this);
            String trimmedPath = response.getPath().replace("\\\\BJ01\\VehicleData\\", "");

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedDocuments.add(response);
                } else {
                    selectedDocuments.remove(response);
                }
                updateShareButtonVisibility(selectedDocuments);
            });

            dataRow.addView(checkBox);

            addTableCell(dataRow, response.getVehicleNumber());
            addTableCell(dataRow, response.getVehicleDocumentType());
            addTableCell(dataRow, response.getDocumentName());
            addTableCell(dataRow, response.getYear());

            Button viewButton = new Button(VehicleDocumentsActivity.this);
            viewButton.setText(getString(R.string.VIEW));
            viewButton.setOnClickListener(v -> {
                try {
                    viewDocument(trimmedPath);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            dataRow.addView(viewButton);

            Button downloadButton = new Button(VehicleDocumentsActivity.this);
            downloadButton.setText(getString(R.string.Download));
            downloadButton.setOnClickListener(v -> {
                try {
                    downloadDocument(trimmedPath);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            dataRow.addView(downloadButton);

            responseTable.addView(dataRow);
        }
    }

    private void downloadDocument(String trimmedPath) throws Exception {
        Log.d(TAG, "Trimmed-Path :" + trimmedPath);

        initializeS3Client();

        new VehicleDocumentsActivity.DownloadFileTask().execute(trimmedPath);
    }

    private class DownloadFileTask extends AsyncTask<String, Void, String> implements VehicleDocumentsActivity.ProgressUpdateCallback {
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = new ProgressDialog(VehicleDocumentsActivity.this);
            progressDialog.setMessage("Downloading file...");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            String filePath = params[0];
            return DownloadS3File(filePath);
        }

        @Override
        protected void onPostExecute(String downloadedFilePath) {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            if (downloadedFilePath != null) {
                Log.v(TAG, "Download completed successfully. File path: " + downloadedFilePath);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse("content://download"), "*/*");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "No application found to open Downloads directory.", e);
                }
            } else {
                Log.e(TAG, "Download failed.");
            }
        }

        @Override
        public void onProgressUpdate(int progress) {}
    }
    private void initializeS3Client() throws Exception {
//        String accessKey = decodeCredential(ENCODED_ACCESS_KEY);
//        String secretKey = decodeCredential(ENCODED_SECRET_KEY);

        String AccessKeyID = decryptAES(accessKey,"NLKpQsoPaeoZ55ul","RbeqxtNXxucHI123");
        String SecretKeyID = decryptAES(secretKey,"NLKpQsoPaeoZ55ul","RbeqxtNXxucHI123");


        BasicAWSCredentials credentialsProvider = new BasicAWSCredentials(AccessKeyID, SecretKeyID);
        amazonS3Client = new AmazonS3Client(credentialsProvider);
    }


    public static String decryptAES(String encryptedBase64, String secretKey, String initVector) throws Exception {
        // Ensure key and IV are 16 bytes for AES-128
        if (secretKey.length() != 16 || initVector.length() != 16) {
            throw new IllegalArgumentException("Key and IV must be 16 characters long.");
        }

        byte[] keyBytes = secretKey.getBytes("UTF-8");
        byte[] ivBytes = initVector.getBytes("UTF-8");

        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivSpec);

        byte[] encryptedBytes = Base64.decode(encryptedBase64, Base64.DEFAULT);
        byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

        return new String(decryptedBytes, "UTF-8");
    }
    private void shareDocuments(List<SearchVehicleDocumentsResponseModel> documents) throws Exception {
        List<String> trimmedPaths = new ArrayList<>();

        for (SearchVehicleDocumentsResponseModel document : documents) {
            String trimmedPath = document.getPath().replace("\\\\BJ01\\VehicleData\\", "");
            trimmedPaths.add(trimmedPath);
        }

        ProgressDialog progressDialog = new ProgressDialog(VehicleDocumentsActivity.this);
        progressDialog.setCancelable(false);
        progressDialog.show();

        downloadSelectedDocuments(trimmedPaths, progressDialog);
    }


    private void downloadSelectedDocuments(List<String> trimmedPaths, ProgressDialog progressDialog) throws Exception {

        progressDialog.setMessage("Downloading All files...");
        progressDialog.setMax(trimmedPaths.size());
        progressDialog.setProgress(0);
        progressDialog.setIndeterminate(false);

        List<String> downloadedFilePaths = new ArrayList<>();

        for (int i = 0; i < trimmedPaths.size(); i++) {
            String trimmedPath = trimmedPaths.get(i);
            downloadSelectedDocument(trimmedPath, progressDialog, i, trimmedPaths.size(), downloadedFilePaths);
        }
    }

    private void downloadSelectedDocument(String trimmedPath, ProgressDialog progressDialog, int currentIndex, int totalFiles, List<String> downloadedFilePaths) throws Exception {
        Log.d(TAG, "Trimmed-Path: " + trimmedPath);

        initializeS3Client();

        new DownloadSelectedFileTask(progressDialog, currentIndex, totalFiles, downloadedFilePaths).execute(trimmedPath);
    }

    private class DownloadSelectedFileTask extends AsyncTask<String, Integer, String> {
        private final ProgressDialog progressDialog;
        private final int currentIndex;
        private final int totalFiles;
        private final List<String> downloadedFilePaths;

        public DownloadSelectedFileTask(ProgressDialog progressDialog, int currentIndex, int totalFiles, List<String> downloadedFilePaths) {
            this.progressDialog = progressDialog;
            this.currentIndex = currentIndex;
            this.totalFiles = totalFiles;
            this.downloadedFilePaths = downloadedFilePaths;
        }

        @Override
        protected String doInBackground(String... paths) {
            String downloadedFilePath = DownloadSelectedS3File(paths[0]);

            publishProgress(currentIndex + 1);

            if (downloadedFilePath != null) {
                downloadedFilePaths.add(downloadedFilePath);
            }

            return downloadedFilePath;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progressDialog.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(String downloadedFilePath) {
            if (currentIndex == totalFiles - 1) {
                progressDialog.dismiss();
                shareDownloadedFiles(downloadedFilePaths);
            }
        }
    }

    private String DownloadSelectedS3File(String filePath) {
        String bucketName = "balajivehicledata";
        String downloadedFilePath = null;

        try {
            S3Object s3object = amazonS3Client.getObject(bucketName, filePath);

            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloadsDir, filePath.substring(filePath.lastIndexOf("/") + 1));

            try (InputStream objectData = s3object.getObjectContent();
                 OutputStream outputStream = new FileOutputStream(file)) {

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = objectData.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }

            downloadedFilePath = file.getAbsolutePath();
            Log.v(TAG, "File downloaded successfully to -> " + downloadedFilePath);
        } catch (AmazonS3Exception e) {
            Log.e(TAG, "S3 Error: " + e.getMessage() + " for key: " + filePath);
        } catch (IOException e) {
            Log.e(TAG, "Failed to download file: " + filePath);
        }

        return downloadedFilePath;
    }

    private void shareDownloadedFiles(List<String> downloadedFilePaths) {
        ArrayList<Uri> fileUris = new ArrayList<>();

        for (String filePath : downloadedFilePaths) {
            Log.d(TAG, "File Path : " + filePath);
            File file = new File(filePath);

            if (file.exists()) {
                Uri fileUri = FileProvider.getUriForFile(this, "com.loginactivity.ftdriver.fileprovider", file);
                fileUris.add(fileUri);
                Log.e(TAG, "Generated URI: " + fileUri.toString());
            } else {
                Log.e(TAG, "File does not exist: " + filePath);
            }
        }

        if (!fileUris.isEmpty()) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);

            String mimeType = getMimeTypes(downloadedFilePaths.get(0));
            shareIntent.setType(mimeType);

            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris);

            try {
                startActivity(Intent.createChooser(shareIntent, "Share via"));
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "WhatsApp or WhatsApp Business is not installed", e);
                Toast.makeText(this, "WhatsApp or WhatsApp Business is not installed", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "No valid files to share");
        }
    }

    private String getMimeTypes(String filePath) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(filePath);
        if (extension == null) {
            return "*/*";
        }

        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
        return mimeType != null ? mimeType : "*/*";
    }
    private void viewDocument(String trimmedPath) throws Exception {
        Log.d(TAG, "Attempting to view file at path: " + trimmedPath);

        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(downloadsDir, trimmedPath.substring(trimmedPath.lastIndexOf("/") + 1));

        if (file.exists()) {
            Log.w(TAG, "File already exists, opening directly.");
            openFile(file.getAbsolutePath());
        } else {
            Log.e(TAG, "File does not exist, downloading from S3.");
            initializeS3Client();
            new VehicleDocumentsActivity.DownloadViewFileTask().execute(trimmedPath);
        }
    }

    private void openFile(String filePath) {
        File file = new File(filePath);
        Uri fileUri = FileProvider.getUriForFile(this, "com.loginactivity.ftdriver.fileprovider", file);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(fileUri, getMimeType(filePath));
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            this.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "No application found to open this file type.");
        }
    }

    private class DownloadViewFileTask extends AsyncTask<String, Void, String> implements VehicleDocumentsActivity.ProgressUpdateCallback {
        private ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = new ProgressDialog(VehicleDocumentsActivity.this);
            progressDialog.setMessage("Downloading file...");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            String filePath = params[0];
            return DownloadS3File(filePath);
        }

        @Override
        protected void onPostExecute(String downloadedFilePath) {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            if (downloadedFilePath != null) {
                Log.v(TAG, "Download completed successfully. File path: " + downloadedFilePath);

                File file = new File(downloadedFilePath);
                Uri fileUri = FileProvider.getUriForFile(VehicleDocumentsActivity.this, "com.loginactivity.ftdriver.fileprovider",file);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(fileUri, getMimeType(downloadedFilePath));
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                try {
                    VehicleDocumentsActivity.this.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Log.e(TAG, "No application found to open this file type.");
                }
            } else {
                Log.e(TAG, "Download failed.");
            }
        }

        @Override
        public void onProgressUpdate(int progress) {}
    }

    private String DownloadS3File(String filePath) {
        String bucketName = "balajivehicledata";
        String downloadedFilePath = null;

        try {
            S3Object s3object = amazonS3Client.getObject(bucketName, filePath);

            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File file = new File(downloadsDir, filePath.substring(filePath.lastIndexOf("/") + 1));

            try (InputStream objectData = s3object.getObjectContent();
                 OutputStream outputStream = new FileOutputStream(file)) {

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = objectData.read(buffer)) > 0) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            downloadedFilePath = file.getAbsolutePath();

            Log.v(TAG, "File downloaded successfully to -> " + downloadedFilePath);
        } catch (AmazonS3Exception e) {
            if ("NoSuchKey".equals(e.getErrorCode())) {
                Log.e(TAG, "The specified key does not exist: " + filePath);
                showAlertDialog();
            } else {
                Log.e(TAG, "AmazonS3Exception: " + e.getMessage());
                showAlertDialog();
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException: " + e.getMessage());
            showAlertDialog();
        }

        return downloadedFilePath;
    }

    public interface ProgressUpdateCallback {
        void onProgressUpdate(int progress);
    }

    private void showAlertDialog() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            new AlertDialog.Builder(this)
                    .setTitle("Alert")
                    .setMessage("File not found")
                    .setIcon(R.drawable.baseline_error_24)
                    .setPositiveButton("OK", null)
                    .show();
        } else {
            new Handler(Looper.getMainLooper()).post(() -> new AlertDialog.Builder(this)
                    .setTitle("Error")
                    .setMessage("File not found")
                    .setIcon(R.drawable.baseline_error_24)
                    .setPositiveButton("OK", null)
                    .show());
        }
    }
    private String getMimeType(String path) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(path);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }


    private void updateShareButtonVisibility(List<SearchVehicleDocumentsResponseModel> selectedDocuments) {
        if (selectedDocuments.isEmpty()) {
            shareButton.setVisibility(View.GONE);
        } else {
            shareButton.setVisibility(View.VISIBLE);
        }
    }
    private void addHeadingCell(TableRow tableRow, String text, int backgroundColor) {
        TextView headingTextView = new TextView(this);
        headingTextView.setText(text);
        headingTextView.setTextColor(Color.BLACK);
        headingTextView.setPadding(16, 16, 16, 16);
        headingTextView.setTextSize(14);
        headingTextView.setTypeface(null, Typeface.BOLD);
        headingTextView.setBackgroundColor(backgroundColor);
        tableRow.addView(headingTextView);
    }

    private void addTableCell(TableRow tableRow, String text) {
        TextView textView = new TextView(VehicleDocumentsActivity.this);
        textView.setText(text);
        textView.setTextColor(Color.BLACK);
        textView.setPadding(16, 16, 16, 16);
        tableRow.addView(textView);
    }
    private void showFailureDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(VehicleDocumentsActivity.this);
        builder.setTitle(title)
                .setMessage(message)
                .setIcon(R.drawable.baseline_error_24)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    private void fetchAWSAccessKey() {
        try {
            ApiService apiService = RetrofitAPIServiceInstance.getApiService();
            apiServiceCacheMemoryWrapper = new ApiServiceCacheMemoryWrapper(this, apiService);

            apiServiceCacheMemoryWrapper.getAmazonAccessKey(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        accessKey = response.body();

//                        try {
//                            decryptedAccessKey = decryptAES(accessKey,"NLKpQsoPaeoZ55ul","RbeqxtNXxucHI123");
//                            Log.v("decryptedAccessKey","decryptedAccessKey"+decryptedAccessKey);
//                        } catch (Exception e) {
//                            throw new RuntimeException(e);
//                        }



                        Log.d("AWSKey", "Fetched key: " + accessKey);

                        // Use accessKey as needed
                    } else {
                        Log.e("AWSKey", "Failed to get key. Response code: " + response.code());
                        Toast.makeText(VehicleDocumentsActivity.this, "Failed to get key", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Log.e("AWSKey", "API call failed: " + t.getMessage(), t);
                    Toast.makeText(VehicleDocumentsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            Log.e("AWSKey", "Exception occurred: " + e.getMessage(), e);
        }
    }

    private void fetchAWSSecretKey() {
        try {
            ApiService apiService = RetrofitAPIServiceInstance.getApiService();
            apiServiceCacheMemoryWrapper = new ApiServiceCacheMemoryWrapper(this, apiService);

            apiServiceCacheMemoryWrapper.getAmazonSecretKey(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        secretKey = response.body();
                        Log.d("AWSKey", "Fetched key: " + secretKey);




                        if (accessKey != null && !accessKey.isEmpty()
                                && secretKey != null && !secretKey.isEmpty()) {
                            try {
                                initializeS3Client();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            Log.e("Failed", "AWS Credentials are Missing");
                        }
                    } else {
                        Log.e("AWSKey", "Failed to get key. Response code: " + response.code());
                        Toast.makeText(VehicleDocumentsActivity.this, "Failed to get key", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Log.e("AWSKey", "API call failed: " + t.getMessage(), t);
                    Toast.makeText(VehicleDocumentsActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            Log.e("AWSKey", "Exception occurred: " + e.getMessage(), e);
        }
    }
}