package com.loginactivity.ftdriver.ui.uploadPOD;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.HttpMethod;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.loginactivity.ftdriver.ApiService.ApiService;
import com.loginactivity.ftdriver.ApiService.RetrofitAPIServiceInstance;
import com.loginactivity.ftdriver.CacheMemory.ApiServiceCacheMemoryWrapper;
import com.loginactivity.ftdriver.R;
import com.loginactivity.ftdriver.model.BTVehicle;
import com.loginactivity.ftdriver.model.InsertTripModel;
import com.loginactivity.ftdriver.model.ShipmentDetailsModel;
import com.loginactivity.ftdriver.model.VehicleTripDetails;
import com.loginactivity.ftdriver.ui.FTDriver.VehicleDocumentsActivity;
import com.loginactivity.ftdriver.ui.UploadPODViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

interface UploadCallback {
    void onUploadSuccess(String url);
    void onUploadFailed(String error);
}

public class UploadPODFragment extends Fragment {

    private UploadPODViewModel mViewModel;
    private TextInputEditText VehicleNumberTextInputEditText;
    private boolean isLoadingVehicleNumbers = false;
    private  String accessKey,secretKey;
    private ApiServiceCacheMemoryWrapper apiServiceCacheMemoryWrapper;
    private static final int REQUEST_CODE_TAKE_PICTURE = 1, REQUEST_CODE_CAMERA_PERMISSION = 2, REQUEST_CODE_SELECT_IMAGES = 3;
    private List<BTVehicle> VehicleList = new ArrayList<>();
    private final Map<String, Integer> VehicleIDs = new HashMap<>();
    private List<String> vehicleNumbers = new ArrayList<>();
    private static final String TAG = "UploadPODFragment";
    private ProgressDialog progressDialog;
    private boolean isCameraLoopEnabled = false;

    TableLayout tableShipment;
    TextInputEditText selectVehicleNumber;
    MaterialButton btnSubmit,btnSubmitAll;
    private Uri cameraImageUri;

    private boolean isDialogShowing = false;
    private List<ShipmentDetailsModel> shipmentList = new ArrayList<>();
    private Map<Integer, List<Uri>> rowSelectedFiles = new HashMap<>();
    private Map<Integer, File> rowGeneratedPDF = new HashMap<>();
    private Map<Integer, String> rowUploadedFileName = new HashMap<>();
    private int currentRowIndex = -1;
    private TextView currentPdfLabel = null;
    private AmazonS3Client s3Client;
    HashMap<Integer, File> localPdfMap = new HashMap<>();
    HashMap<Integer, String> uploadedPdfUrlMap = new HashMap<>();

    private boolean isCameraLoopRunning = false;

    private Map<Integer, File> newFilesToUpload = new HashMap<>();

    public static UploadPODFragment newInstance() {
        return new UploadPODFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_upload_p_o_d,container,false);
        VehicleNumberTextInputEditText = view.findViewById(R.id.selectVehicleNumber);

        tableShipment = view.findViewById(R.id.tableShipment);
        selectVehicleNumber = view.findViewById(R.id.selectVehicleNumber);
        btnSubmit = view.findViewById(R.id.btnSubmit);
        btnSubmitAll = view.findViewById(R.id.btnSubmitAll);
        btnSubmitAll.setOnClickListener(v -> {
            try {
                uploadAllPdfsAndSubmit();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        fetchVehicleNumber();

        fetchAWSAccessKey();
        fetchAWSSecretKey();



        btnSubmit.setOnClickListener(v -> {
            String selectedVehicleNumber = VehicleNumberTextInputEditText.getText().toString().trim();
            if (selectedVehicleNumber.isEmpty()) {
                showAlertDialog("Failed","Please Select Vehicle and Submit");
                return;
            }
            Integer vehicleId = VehicleIDs.get(selectedVehicleNumber);



            if (vehicleId != null) {
                try {
                    fetchShipmentDetails(vehicleId);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            } else {
                showAlertDialog("Failed","Please Select Vehicle");
            }
        });

        return view;

    }


    private void initS3Client() throws Exception {
        String ACCESS_KEY = decryptAES(accessKey,"NLKpQsoPaeoZ55ul","RbeqxtNXxucHI123");
        String SECRET_KEY = decryptAES(secretKey,"NLKpQsoPaeoZ55ul","RbeqxtNXxucHI123");

        BasicAWSCredentials credentials = new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY);
        s3Client = new AmazonS3Client(credentials, Region.getRegion(Regions.AP_SOUTH_1));

    }




    private void uploadAllPdfsAndSubmit() throws Exception {

        if (newFilesToUpload.isEmpty()) {
            Toast.makeText(requireContext(), "No new POD selected", Toast.LENGTH_SHORT).show();
            return;
        }

        if (s3Client == null) initS3Client();

        progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Saving the Data...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        int totalUploads = newFilesToUpload.size();
        AtomicInteger completedUploads = new AtomicInteger(0);

        uploadedPdfUrlMap.clear(); // reset before use

        for (Map.Entry<Integer, File> entry : newFilesToUpload.entrySet()) {

            int rowIndex = entry.getKey();
            File pdfFile = entry.getValue();

            String fileName = "POD_" + shipmentList.get(rowIndex).getShipmentId() + "_" + System.currentTimeMillis() + ".pdf";

            uploadToS3(pdfFile, fileName, new UploadCallback() {
                @Override
                public void onUploadSuccess(String url) {

                    uploadedPdfUrlMap.put(rowIndex, url);

                    if (completedUploads.incrementAndGet() == totalUploads) {
                        callInsertVehicleTripDetailsAPI(); // only for changed rows
                    }
                }

                @Override
                public void onUploadFailed(String error) {
                    progressDialog.dismiss();
                    Toast.makeText(requireContext(), "Upload Failed: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }



    private void uploadToS3(File file, String fileName, UploadCallback callback) {
        Log.v("UploadToAWS","UploadToAWS");
        new Thread(() -> {
            try {
                PutObjectRequest req = new PutObjectRequest("shipmentpod", fileName, file);
                s3Client.putObject(req);

                String fileUrl = "https://shipmentpod.s3.amazonaws.com/" + fileName;
                callback.onUploadSuccess(fileUrl);

            } catch (Exception e) {
                callback.onUploadFailed(e.getMessage());
                Log.v("AWSUploadError","AWSUploadError");
            }
        }).start();
    }


    private void callInsertVehicleTripDetailsAPI() {

        ApiService api;
        try { api = RetrofitAPIServiceInstance.getApiService(requireContext()); }
        catch (Exception e) { throw new RuntimeException(e); }

        Gson gson = new Gson();

        int totalCalls = uploadedPdfUrlMap.size();
        AtomicInteger completedCalls = new AtomicInteger(0);

        for (Map.Entry<Integer, String> entry : uploadedPdfUrlMap.entrySet()) {

            int rowIndex = entry.getKey();
            String newPodUrl = entry.getValue();

            ShipmentDetailsModel m = shipmentList.get(rowIndex);

            InsertTripModel body = new InsertTripModel();
            body.shipmentId = String.valueOf(m.getShipmentId());
            body.date = convertDate(m.getDate());
            body.gcNo = m.getGcNo();
            body.invoiceNo = m.getInvoiceNo();
            body.type = m.getType();
            body.pod = newPodUrl;

            Call<ResponseBody> call = api.InsertVehicleTripDetails(body);

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                    // delete generated PDF
                    File pdfFile = newFilesToUpload.get(rowIndex);
                    if (pdfFile != null && pdfFile.exists()) pdfFile.delete();

                    // cleanup
                    newFilesToUpload.remove(rowIndex);
                    rowGeneratedPDF.remove(rowIndex);
                    uploadedPdfUrlMap.remove(rowIndex);

                    if (completedCalls.incrementAndGet() == totalCalls) {

                        if (progressDialog != null && progressDialog.isShowing())
                            progressDialog.dismiss();

                        btnSubmitAll.setVisibility(View.GONE);

                        showAlertDialog("Success", "All Data Saved Successfully");
                        tableShipment.removeAllViews();
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    if (completedCalls.incrementAndGet() == totalCalls) {
                        if (progressDialog.isShowing()) progressDialog.dismiss();
                    }
                    showAlertDialog("Failed", "Network Failure");
                }
            });
        }
    }




    private String convertDate(String inputDate) {
        try {
            SimpleDateFormat fromFormat = new SimpleDateFormat("dd-MMM-yy", Locale.ENGLISH);
            SimpleDateFormat toFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

            Date date = fromFormat.parse(inputDate);
            return toFormat.format(date);

        } catch (Exception e) {
            Log.e("DateFormat", "Error: " + e.getMessage());
            return inputDate;
        }
    }


    private void fetchVehicleNumber() {
        if (isLoadingVehicleNumbers) return;

        try {
            ApiService apiService = RetrofitAPIServiceInstance.getApiService(requireContext());
            apiServiceCacheMemoryWrapper = new ApiServiceCacheMemoryWrapper(requireContext(), apiService);

            ProgressDialog progressDialog = new ProgressDialog(requireContext());
            progressDialog.setMessage("Loading Vehicle Numbers...");
            progressDialog.setCancelable(false);
            progressDialog.show();

            isLoadingVehicleNumbers = true;

            apiServiceCacheMemoryWrapper.getBTVehicleNumbersDetails(new Callback<List<BTVehicle>>() {
                @Override
                public void onResponse(@NonNull Call<List<BTVehicle>> call, @NonNull Response<List<BTVehicle>> response) {
                    progressDialog.dismiss();
                    isLoadingVehicleNumbers = false;

                    if (response.isSuccessful()) {
                        VehicleList = response.body();
                        if (VehicleList == null) return;

                        VehicleIDs.clear();
                        vehicleNumbers.clear();

                        for (BTVehicle vehicle : VehicleList) {
                            String formattedVehicleNumber = vehicle.getVehicle();
                            vehicleNumbers.add(formattedVehicleNumber);
                            VehicleIDs.put(formattedVehicleNumber, vehicle.getVehicleID());
                        }

                        // ✅ Now set up click listener to show dialog when user clicks the field
                        VehicleNumberTextInputEditText.setOnClickListener(v -> showVehicleDialog());

                    } else {
                        Log.e(TAG, "Failed to get a successful response");
                        showAlertDialog("Alert", "Failed to get Vehicle Numbers");
                    }
                }

                @Override
                public void onFailure(@NonNull Call<List<BTVehicle>> call, @NonNull Throwable t) {
                    progressDialog.dismiss();
                    isLoadingVehicleNumbers = false;
                    Log.e(TAG, "API call failed: " + t.getMessage());
                    showAlertDialog("Alert", "Failed to get Vehicle Numbers");
                }
            });

        } catch (Exception e) {
            isLoadingVehicleNumbers = false;
            Log.e(TAG, "Exception occurred", e);
            showAlertDialog("Alert", "Failed to get Vehicle Numbers");
        }
    }

    private void fetchShipmentDetails(int vehicleId) throws Exception {

        // Initialize
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage("Loading...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        ApiService apiService = RetrofitAPIServiceInstance.getApiService(requireContext());
        Call<List<ShipmentDetailsModel>> call = apiService.getShipmentDetails(vehicleId);

        call.enqueue(new Callback<List<ShipmentDetailsModel>>() {
            @Override
            public void onResponse(Call<List<ShipmentDetailsModel>> call, Response<List<ShipmentDetailsModel>> response) {
                btnSubmitAll.setVisibility(View.VISIBLE);
                // Close dialog
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                if (!response.isSuccessful()) {
                    Toast.makeText(getContext(), "Error: " + response.code(), Toast.LENGTH_SHORT).show();
                    return;
                }

                shipmentList = response.body();
                loadTable(shipmentList);
            }

            @Override
            public void onFailure(Call<List<ShipmentDetailsModel>> call, Throwable t) {

                // Close dialog
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                Toast.makeText(getContext(), "Failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }



    private void showVehicleDialog() {
        if (isDialogShowing || vehicleNumbers == null || vehicleNumbers.isEmpty()) return;

        isDialogShowing = true;

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_searchable_spinner);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Resize
        DisplayMetrics displayMetrics = new DisplayMetrics();
        requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        dialog.getWindow().setLayout((int) (screenWidth * 0.8), (int) (screenHeight * 0.7));

        TextView LocationName = dialog.findViewById(R.id.dropdownname);
        LocationName.setText(getString(R.string.Select_Vehicle_Number));
        LocationName.setTextColor(Color.BLACK);

        EditText editText = dialog.findViewById(R.id.edit_text);
        ListView listView = dialog.findViewById(R.id.list_view);

        List<String> filteredList = new ArrayList<>(vehicleNumbers);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.list_item, R.id.text_view, filteredList);
        listView.setAdapter(adapter);

        listView.setDivider(new ColorDrawable(getResources().getColor(android.R.color.black)));
        listView.setDividerHeight(2);

        // Search filter
        editText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String filter = s.toString().toLowerCase().trim();
                filteredList.clear();
                if (filter.isEmpty()) {
                    filteredList.addAll(vehicleNumbers);
                } else {
                    for (String item : vehicleNumbers) {
                        if (item.toLowerCase().contains(filter)) {
                            filteredList.add(item);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedVehicleNumber = adapter.getItem(position);
            VehicleNumberTextInputEditText.setText(selectedVehicleNumber);

            if (selectedVehicleNumber != null && !selectedVehicleNumber.isEmpty()) {
                Integer VehicleID = VehicleIDs.get(selectedVehicleNumber);
                if (VehicleID != null) {
                    Log.v(TAG, "Vehicle ID = " + VehicleID + ", Vehicle Number = " + selectedVehicleNumber);
                    // 👉 You can call fetchVehicleDocuments(VehicleID) here
                } else {
                    showAlertDialog("Alert", "Vehicle ID not Found");
                }
            }

            dialog.dismiss();
            isDialogShowing = false;
        });

        dialog.setOnDismissListener(dialogInterface -> isDialogShowing = false);
        dialog.show();
    }


    private void loadTable(List<ShipmentDetailsModel> list) {

        tableShipment.removeAllViews();

        // Header row
        TableRow headerRow = new TableRow(getContext());
        addHeadingCell(headerRow, "ShipmentId", Color.YELLOW);
        addHeadingCell(headerRow, "Date", Color.YELLOW);
        addHeadingCell(headerRow, "GC No", Color.YELLOW);
        addHeadingCell(headerRow, "Invoice", Color.YELLOW);
        addHeadingCell(headerRow, "Party", Color.YELLOW);
        addHeadingCell(headerRow, "Type", Color.YELLOW);
        addHeadingCell(headerRow, "Tonnage", Color.YELLOW);
        addHeadingCell(headerRow, "EWBNo", Color.YELLOW);
        addHeadingCell(headerRow, "Upload POD", Color.YELLOW);
        addHeadingCell(headerRow, "POD File", Color.YELLOW);

        tableShipment.addView(headerRow);

        for (int i = 0; i < list.size(); i++) {

            ShipmentDetailsModel model = list.get(i);
            int rowIndex = i;

            TableRow row = new TableRow(getContext());

            addTableCell(row, String.valueOf(model.getShipmentId()));
            addTableCell(row, model.getDate());
            addTableCell(row, model.getGcNo());
            addTableCell(row, model.getInvoiceNo());
            addTableCell(row, model.getPartyName());
            addTableCell(row, model.getType());
            addTableCell(row, model.getTonnage());
            addTableCell(row, model.getEWBNo());

            // Upload Button
            Button uploadBtn = new Button(requireContext());
            uploadBtn.setText("Upload");
            row.addView(uploadBtn);

            // POD Label
            TextView pdfNameLabel = new TextView(requireContext());
            pdfNameLabel.setPadding(20, 20, 20, 20);
            pdfNameLabel.setTextColor(Color.BLUE);
            pdfNameLabel.setPaintFlags(pdfNameLabel.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

            row.addView(pdfNameLabel);

            String podUrl = model.getPOD(); // Comes from API

            if (podUrl != null && !podUrl.isEmpty()) {

                // Extract file name from S3 URL
                String s3FileName = podUrl.substring(podUrl.lastIndexOf("/") + 1);

                pdfNameLabel.setText("Downloading...");

                // Auto-download existing POD
                downloadPODFromS3(s3FileName, rowIndex, pdfNameLabel);

            } else {
                pdfNameLabel.setText("No File");

                // Open already-generated local PDF if exists
                pdfNameLabel.setOnClickListener(v -> {
                    if (rowGeneratedPDF.containsKey(rowIndex)) {
                        openPDF(rowGeneratedPDF.get(rowIndex));
                    }
                });
            }

            uploadBtn.setOnClickListener(v -> showImageSourceDialog(rowIndex, pdfNameLabel));

            tableShipment.addView(row);
        }
    }



    private void downloadPODFromS3(String fileName, int rowIndex, TextView label) {

        new Thread(() -> {
            try {
                // Generate signed URL (valid for 5 mins)
                Date expiration = new Date();
                expiration.setTime(expiration.getTime() + 1000 * 60 * 5);

                GeneratePresignedUrlRequest request =
                        new GeneratePresignedUrlRequest("shipmentpod", fileName)
                                .withMethod(HttpMethod.GET)
                                .withExpiration(expiration);

                URL signedUrl = s3Client.generatePresignedUrl(request);

                // Download file
                HttpURLConnection conn = (HttpURLConnection) signedUrl.openConnection();
                conn.connect();

                InputStream input = conn.getInputStream();

                File outFile = new File(requireContext().getCacheDir(), fileName);
                FileOutputStream output = new FileOutputStream(outFile);

                byte[] buffer = new byte[4096];
                int len;

                while ((len = input.read(buffer)) != -1) {
                    output.write(buffer, 0, len);
                }

                output.close();
                input.close();

                // Save local file reference
                rowGeneratedPDF.put(rowIndex, outFile);

                // Update UI
                requireActivity().runOnUiThread(() -> {
                    label.setText(outFile.getName());
                    label.setOnClickListener(v -> openPDF(outFile));
                });

            } catch (Exception e) {
                e.printStackTrace();

                requireActivity().runOnUiThread(() -> {
                    label.setText("Failed");
                    Toast.makeText(requireContext(), "Failed to download POD", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }


    private void showImageSourceDialog(int rowIndex, TextView pdfLabel) {
        currentRowIndex = rowIndex;
        currentPdfLabel = pdfLabel;

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Select Source")
                .setItems(new CharSequence[]{"Take Picture", "Choose File"}, (d, w) -> {
                    if (w == 0) checkAndRequestPhotoPermissions();
                    else openImagePicker();
                });
        builder.show();
    }

    private String getTimestamp() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
    }

@Override
public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (currentRowIndex == -1) return;

    // User pressed BACK → stop camera loop and create PDF
    if (resultCode != Activity.RESULT_OK) {
        isCameraLoopRunning = false;
        generateFinalPDF(); // create PDF if any images were captured
        return;
    }

    // Get previous list for this row
    List<Uri> selectedUris = rowSelectedFiles.getOrDefault(currentRowIndex, new ArrayList<>());

    // -------------------- HANDLE GALLERY ----------------------
    if (requestCode == REQUEST_CODE_SELECT_IMAGES && data != null) {
        if (data.getClipData() != null) {
            int count = data.getClipData().getItemCount();
            for (int i = 0; i < count; i++) {
                Uri uri = data.getClipData().getItemAt(i).getUri();
                handleSelectedFile(uri, selectedUris);
            }
        } else if (data.getData() != null) {
            Uri uri = data.getData();
            handleSelectedFile(uri, selectedUris);
        }
    }

    // -------------------- HANDLE CAMERA ----------------------
    if (requestCode == REQUEST_CODE_TAKE_PICTURE) {
        selectedUris.add(cameraImageUri);

        if (isCameraLoopRunning) {
            rowSelectedFiles.put(currentRowIndex, selectedUris);
            openCamera(); // reopen camera automatically
            return; // skip PDF creation until user finishes
        }
    }

    // Save selected URIs
    rowSelectedFiles.put(currentRowIndex, selectedUris);

    // Generate PDF only if there are images (skip if already a PDF)
    if (!newFilesToUpload.containsKey(currentRowIndex)) {
        generateFinalPDF();
    }
}

    // -------------------- Helper for gallery selection --------------------
    private void handleSelectedFile(Uri uri, List<Uri> selectedUris) {
        String fileName = getFileName(uri);

        if (fileName != null && fileName.toLowerCase().endsWith(".pdf")) {
            File pdfFile = copyPdfToCache(uri, fileName);
            if (pdfFile != null) {

                rowGeneratedPDF.remove(currentRowIndex);
                localPdfMap.remove(currentRowIndex);
                newFilesToUpload.remove(currentRowIndex);
                uploadedPdfUrlMap.remove(currentRowIndex);   // VERY IMPORTANT

// Replace with new PDF
                rowGeneratedPDF.put(currentRowIndex, pdfFile);
                localPdfMap.put(currentRowIndex, pdfFile);
                newFilesToUpload.put(currentRowIndex, pdfFile);

// Update UI
                currentPdfLabel.setText(pdfFile.getName());
                currentPdfLabel.setOnClickListener(v -> openPDF(pdfFile));
            }
        } else {
            selectedUris.add(uri); // image files will be converted to PDF later
        }
    }

    // -------------------- Get sanitized filename from Uri --------------------
    private String getFileName(Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = requireContext().getContentResolver()
                    .query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) result = cursor.getString(index);
                }
            }
        }

        if (result == null) result = uri.getLastPathSegment();

        // Remove query parameters if any (e.g., ?id=123)
        if (result != null && result.contains("?")) {
            result = result.substring(0, result.indexOf("?"));
        }

        return result;
    }

    // -------------------- Copy PDF to cache without renaming --------------------
    private File copyPdfToCache(Uri uri, String fileName) {
        try {
            File cacheFile = new File(requireContext().getCacheDir(), fileName);

            try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(cacheFile)) {

                byte[] buffer = new byte[1024];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                out.flush();
            }

            return cacheFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // -------------------- Generate PDF from images --------------------
    private void generateFinalPDF() {
//        if (newFilesToUpload.containsKey(currentRowIndex)) return;

        List<Uri> selectedUris = rowSelectedFiles.get(currentRowIndex);
        if (selectedUris == null || selectedUris.isEmpty()) return;

        File pdfFile = createPDFfromImages(
                selectedUris,
                "POD_" + currentRowIndex + getTimestamp() + ".pdf"
        );

        // Remove old existing POD
        rowGeneratedPDF.remove(currentRowIndex);
        localPdfMap.remove(currentRowIndex);
        newFilesToUpload.remove(currentRowIndex);
        uploadedPdfUrlMap.remove(currentRowIndex);

// Save new one
        rowGeneratedPDF.put(currentRowIndex, pdfFile);
        localPdfMap.put(currentRowIndex, pdfFile);
        newFilesToUpload.put(currentRowIndex, pdfFile);

// Update UI
        currentPdfLabel.setText(pdfFile.getName());
        currentPdfLabel.setOnClickListener(v -> openPDF(pdfFile));


        Log.d("PDF_CREATED", "Final PDF created with " + selectedUris.size() + " images");
    }


    private File createPDFfromImages(List<Uri> images, String fileName) {
        File pdfFile = new File(requireContext().getExternalFilesDir(null), fileName);

        try {
            PdfDocument pdfDocument = new PdfDocument();

            for (int i = 0; i < images.size(); i++) {
                Uri uri = images.get(i);
                Bitmap bmp = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);

                int pageNumber = i + 1;

                PdfDocument.PageInfo pageInfo =
                        new PdfDocument.PageInfo.Builder(bmp.getWidth(), bmp.getHeight(), pageNumber).create();

                PdfDocument.Page page = pdfDocument.startPage(pageInfo);
                Canvas canvas = page.getCanvas();
                canvas.drawBitmap(bmp, 0, 0, null);
                pdfDocument.finishPage(page);
            }

            FileOutputStream fos = new FileOutputStream(pdfFile);
            pdfDocument.writeTo(fos);
            pdfDocument.close();
            fos.close();

        } catch (Exception e) {
            Log.e("PDF", "Error creating PDF: " + e.getMessage());
        }

        return pdfFile;
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGES);
    }
    private void checkAndRequestPhotoPermissions() {
        if (ContextCompat.checkSelfPermission( requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA_PERMISSION);
        } else {
            isCameraLoopRunning = true;
            openCamera();
        }
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
            File photoFile = createImageFile();
            cameraImageUri = FileProvider.getUriForFile(requireContext(), "com.loginactivity.ftdriver.fileprovider", photoFile);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
            startActivityForResult(intent, REQUEST_CODE_TAKE_PICTURE);
        }
    }

    private File createImageFile() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "IMG_" + timeStamp + ".jpg";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = new File(storageDir, imageFileName);
        try {
            if (!image.exists()) {
                image.createNewFile();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error creating image file: " + e.getMessage());
        }
        return image;
    }


    private void addHeadingCell(TableRow tableRow, String text, int backgroundColor) {
        TextView headingTextView = new TextView(requireContext());
        headingTextView.setText(text);
        headingTextView.setTextColor(Color.BLACK);
        headingTextView.setPadding(16, 16, 16, 16);
        headingTextView.setTextSize(14);
        headingTextView.setTypeface(null, Typeface.BOLD);
        headingTextView.setBackgroundColor(backgroundColor);
        tableRow.addView(headingTextView);
    }

    private void addTableCell(TableRow tableRow, String text) {
        TextView textView = new TextView(requireContext());
        textView.setText(text);
        textView.setTextColor(Color.BLACK);
        textView.setPadding(16, 16, 16, 16);
        tableRow.addView(textView);
    }

    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();

                    List<Uri> selectedUris = new ArrayList<>();

                    if (data.getClipData() != null) {
                        // MULTIPLE FILES SELECTED
                        int count = data.getClipData().getItemCount();
                        for (int i = 0; i < count; i++) {
                            selectedUris.add(data.getClipData().getItemAt(i).getUri());
                        }
                    } else if (data.getData() != null) {
                        // SINGLE FILE SELECTED
                        selectedUris.add(data.getData());
                    } else if (cameraImageUri != null) {
                        // FILE FROM CAMERA
                        selectedUris.add(cameraImageUri);
                    }

                    Toast.makeText(requireContext(), "Selected: " + selectedUris.size(), Toast.LENGTH_SHORT).show();
                }
            });
    private TextView createTextView(String text) {
        TextView tv = new TextView(getContext());
        tv.setText(text);
        tv.setPadding(8, 8, 8, 8);
        tv.setTextSize(14);
        return tv;
    }

    private void openPDF(File pdfFile) {

        if (pdfFile == null || !pdfFile.exists()) {
            Toast.makeText(getContext(), "File not found!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Authority must match manifest provider
        Uri uri = FileProvider.getUriForFile(
                requireContext(),
                "com.loginactivity.ftdriver.fileprovider", // fixed
                pdfFile
        );

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "No PDF Viewer installed", Toast.LENGTH_SHORT).show();
        }
    }
    private void fetchAWSAccessKey() {
        try {
            ApiService apiService = RetrofitAPIServiceInstance.getApiService(requireContext());
            apiServiceCacheMemoryWrapper = new ApiServiceCacheMemoryWrapper(requireContext(), apiService);

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
                        Toast.makeText(requireContext(), "Failed to get key", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Log.e("AWSKey", "API call failed: " + t.getMessage(), t);
                    Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            Log.e("AWSKey", "Exception occurred: " + e.getMessage(), e);
        }
    }

    private void fetchAWSSecretKey() {
        try {
            ApiService apiService = RetrofitAPIServiceInstance.getApiService(requireContext());
            apiServiceCacheMemoryWrapper = new ApiServiceCacheMemoryWrapper(requireContext(), apiService);

            apiServiceCacheMemoryWrapper.getAmazonSecretKey(new Callback<String>() {
                @Override
                public void onResponse(Call<String> call, Response<String> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        secretKey = response.body();
                        Log.d("AWSKey", "Fetched key: " + secretKey);




                        if (accessKey != null && !accessKey.isEmpty()
                                && secretKey != null && !secretKey.isEmpty()) {
                            try {
                                initS3Client();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            Log.e("Failed", "AWS Credentials are Missing");
                        }
                    } else {
                        Log.e("AWSKey", "Failed to get key. Response code: " + response.code());
                        Toast.makeText(requireContext(), "Failed to get key", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<String> call, Throwable t) {
                    Log.e("AWSKey", "API call failed: " + t.getMessage(), t);
                    Toast.makeText(requireContext(), "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            Log.e("AWSKey", "Exception occurred: " + e.getMessage(), e);
        }
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

    private void showAlertDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(title)
                .setMessage(message)
                .setIcon(R.drawable.baseline_error_24)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                })
                .show();
    }



}