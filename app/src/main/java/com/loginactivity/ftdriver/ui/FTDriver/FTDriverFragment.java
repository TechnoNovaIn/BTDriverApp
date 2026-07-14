package com.loginactivity.ftdriver.ui.FTDriver;

import static android.content.Context.MODE_PRIVATE;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.JsonReader;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.loginactivity.ftdriver.ApiService.ApiService;
import com.loginactivity.ftdriver.ApiService.RetrofitAPIServiceInstance;
import com.loginactivity.ftdriver.CacheMemory.ApiServiceCacheMemoryWrapper;
import com.loginactivity.ftdriver.R;
import com.loginactivity.ftdriver.model.BTVehicle;
import com.loginactivity.ftdriver.model.Driver;
import com.loginactivity.ftdriver.model.EwayBillExpireDate;
import com.loginactivity.ftdriver.model.Unload;
import com.loginactivity.ftdriver.model.UnloadDetails;
import com.loginactivity.ftdriver.model.VehicleDocument;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FTDriverFragment extends Fragment {

    private FTDriverViewModel mViewModel;

    private TextInputEditText VehicleNumberTextInputEditText, DriverNameTextInputEditText, UnloadLocationEditText,PartyNameEditText,InvoiceNumberEditText,UnloadLocationUrlText,EwayBillExpireDateText;
    private boolean isLoadingVehicleNumbers = false;
    private boolean isLoadingDriverNames = false;
    private static final String TAG = "FTDriverFragment";
    private final Map<String, Integer> VehicleIDs = new HashMap<>();
    private final Map<String, Integer> UnloadIDs = new HashMap<>();
    private final Map<String, Integer> DriverIDs = new HashMap<>();
    private String SelectedVehicleID, SelectedUnloadID,LocationUrl;
    private List<BTVehicle> VehicleList = new ArrayList<>();
    private List<Unload> UnloadList = new ArrayList<>();
    private List<Driver> DriverList = new ArrayList<>();
    private List<String> vehicleNumbers = new ArrayList<>();
    private ApiServiceCacheMemoryWrapper apiServiceCacheMemoryWrapper;
    private boolean isDialogShowing = false;
    private Button submitButton;

    private SharedPreferences sharedPreferences;

    private boolean isLoadingUnload = false;
    private String LoginUserID, LoginUserName;
    private TextView rcExpire,fcExpire,pollutionExpire,insuranceExpire,nationalPermit,kaHomeTax,kaHomeStatePermit,authorization;

    private boolean isDriverListLoaded = false;



    public static FTDriverFragment newInstance() {
        return new FTDriverFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_f_t_driver,container,false);

        DriverNameTextInputEditText = view.findViewById(R.id.selectDriverName);



        VehicleNumberTextInputEditText = view.findViewById(R.id.selectVehicleNumber);
        UnloadLocationEditText = view.findViewById(R.id.unloadLocation);
        PartyNameEditText = view.findViewById(R.id.partyName);
        InvoiceNumberEditText = view.findViewById(R.id.invoiceNumber);
        UnloadLocationUrlText = view.findViewById(R.id.unloadLocationUrl);
        EwayBillExpireDateText = view.findViewById(R.id.ewayBillExpire);

        fetchVehicleNumber();


        ImageButton googleMapsSearchButton = view.findViewById(R.id.googleMapSearchButton);
        googleMapsSearchButton.setOnClickListener(v -> openGoogleMap());


        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("LoginPrefs", MODE_PRIVATE);


        LoginUserID = sharedPreferences.getString("UserId", "");
        LoginUserName = sharedPreferences.getString("Name", "");

        fetchDriverNames();

//        DriverNameTextInputEditText.setText(LoginUserID);




        MaterialButton btnVehicleDocuments = view.findViewById(R.id.btnVehicleDocuments);
        btnVehicleDocuments.setOnClickListener(v -> {
            String selectedVehicleNumber = VehicleNumberTextInputEditText.getText().toString().trim();
            if (selectedVehicleNumber.isEmpty()) {
               showAlertDialog("Failed","Please Select Driver and Submit");
                return;
            }
            Integer vehicleId = VehicleIDs.get(selectedVehicleNumber);



            if (vehicleId != null) {
                fetchVehicleDocuments(vehicleId);
            } else {
                showAlertDialog("Failed","Please Select Vehicle");
            }
        });

        MaterialButton vehicleDocuments = view.findViewById(R.id.VehicleDocuments);
        vehicleDocuments.setOnClickListener(v -> {
            String selectedVehicleNumber = VehicleNumberTextInputEditText.getText().toString().trim();
            if (selectedVehicleNumber.isEmpty()) {
                showAlertDialog("Failed","Please Select Driver and Submit");
                return;
            }
            Integer vehicleId = VehicleIDs.get(selectedVehicleNumber);
            Intent intent = new Intent(requireContext(), VehicleDocumentsActivity.class);
            intent.putExtra("vehicleId", vehicleId);
            startActivity(intent);
        });

        submitButton = view.findViewById(R.id.btnSubmit);
        submitButton.setOnClickListener(v -> fetchShipmentDetails());

        return view;
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


    private void showDriverDialog() {
        if (isDialogShowing || DriverList == null || DriverList.isEmpty()) return;

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

        TextView title = dialog.findViewById(R.id.dropdownname);
        title.setText(getString(R.string.Select_Driver_Name));
        title.setTextColor(Color.BLACK);

        EditText searchBox = dialog.findViewById(R.id.edit_text);
        ListView listView = dialog.findViewById(R.id.list_view);

        // Prepare Driver Names list
        List<String> driverNames = new ArrayList<>();
        for (Driver d : DriverList) {
            driverNames.add(d.getDriver());
            DriverIDs.put(d.getDriver(), d.getDriverID());
        }

        // The list used for filtering
        List<String> filteredList = new ArrayList<>(driverNames);

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(requireContext(), R.layout.list_item, R.id.text_view, filteredList);

        listView.setAdapter(adapter);

        listView.setDivider(new ColorDrawable(getResources().getColor(android.R.color.black)));
        listView.setDividerHeight(2);

        // Search logic
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String filter = s.toString().toLowerCase().trim();
                filteredList.clear();

                if (filter.isEmpty()) {
                    filteredList.addAll(driverNames);
                } else {
                    for (String name : driverNames) {
                        if (name.toLowerCase().contains(filter)) {
                            filteredList.add(name);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Item select
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedDriverName = adapter.getItem(position);
            DriverNameTextInputEditText.setText(selectedDriverName);

            // Patch SelectedDriverID
            if (selectedDriverName != null) {
                Integer SelectedDriverID = DriverIDs.get(selectedDriverName);
                Log.v(TAG, "DriverID = " + SelectedDriverID + ", Name = " + selectedDriverName);
            }

            dialog.dismiss();
            isDialogShowing = false;
        });

        dialog.setOnDismissListener(dialogInterface -> isDialogShowing = false);
        dialog.show();
    }

    private void fetchDriverNames() {
        if (isLoadingDriverNames || isDialogShowing) {
            return;
        }
        try {
            ApiService apiService = RetrofitAPIServiceInstance.getApiService(requireContext());
            apiServiceCacheMemoryWrapper = new ApiServiceCacheMemoryWrapper(requireContext(), apiService);

//            DriverNameTextInputEditText.setOnClickListener(v -> {
                if (isLoadingDriverNames || isDialogShowing) {
                    return;
                }

                ProgressDialog progressDialog = new ProgressDialog(requireContext());
                progressDialog.setMessage("Loading Driver Names...");
                progressDialog.setCancelable(false);
                progressDialog.show();

                isLoadingDriverNames = true;
                apiServiceCacheMemoryWrapper.getDriverNamesDetails(new Callback<List<Driver>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Driver>> call, @NonNull Response<List<Driver>> response) {
                        progressDialog.dismiss();
                        isLoadingDriverNames = false;

                        if (response.isSuccessful()) {
                            DriverList = response.body();

                            for (Driver driver : DriverList) {
                                if (String.valueOf(driver.getDriverID()).equals(LoginUserID)) {
                                    DriverNameTextInputEditText.setText(driver.getDriver());
                                    break;
                                }
                            }

                            DriverNameTextInputEditText.setOnClickListener(v -> showDriverDialog());
                        } else {
                            Log.e(TAG, "Failed to get Driver details");
                            showAlertDialog("Error", "Failed to get Driver Names");
                            //EmailErrorReporter.sendGetMethodErrorEmail(requireContext(), "AdBlue Issued to Vehicle Fragment - API Error Notification", "Fetching Driver Names", Username, response.code(), String.valueOf(response.errorBody()));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<Driver>> call, @NonNull Throwable t) {
                        requireActivity().runOnUiThread(() -> {
                            if (!isAdded() || requireActivity().isFinishing() || requireActivity().isDestroyed()) {
                                return;
                            }
                            progressDialog.dismiss();
                            isLoadingDriverNames = false;

                            Log.e(TAG, "Fetch Driver Names API call failed: " + t.getMessage());
                            showAlertDialog("Error", "Failed to get Driver Names");
                         //   EmailErrorReporter.sendGetMethodErrorEmail(requireContext(), "AdBlue Issued to Vehicle Fragment - API Error Notification", "Fetching Driver Names", Username, null, t.getMessage());
                        });
                    }
                });
//            });
        } catch (Exception e) {
            Log.e(TAG, "Error : " + e.getMessage());
            showAlertDialog("Error", "Failed to get Driver Names");
          //  EmailErrorReporter.sendGetMethodErrorEmail(requireContext(), "AdBlue Issued to Vehicle Fragment - API Error Notification", "Fetching Driver Names", Username, null, e.getMessage());
        }
    }


    private void fetchShipmentDetails() {
        clearFields();

        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Fetching Shipment Details...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        String selectedValue = DriverNameTextInputEditText.getText().toString();
        Log.d(TAG, "selectedValue: " + selectedValue);

        String driverID = DriverIDs.containsKey(selectedValue) ? String.valueOf(DriverIDs.get(selectedValue)) : "";
        Log.i(TAG, "VehicleStatus - Driver ID: " + driverID);

        try {
            ApiService apiService = RetrofitAPIServiceInstance.getApiService(requireContext());
            Call<ResponseBody> call = apiService.searchVehicleStatusDetails(LoginUserID);

            call.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                    submitButton.setEnabled(true);
                    progressDialog.dismiss();

                    if (response.isSuccessful() && response.body() != null) {
                        try {
                            String successMessage = response.body().string();
                            Log.v(TAG, "Response: " + successMessage);

                            // Split on ][ but preserve JSON structure
                            String[] responseParts = successMessage.split("(?<=\\])\\s*\\[(?=\\{)");

                            for (String part : responseParts) {
                                part = part.replaceAll("^\\[|\\]$", ""); // remove outer brackets
                                JSONArray jsonArray = new JSONArray("[" + part + "]");

                                if (jsonArray.length() == 0) continue;

                                JSONObject firstObject = jsonArray.getJSONObject(0);

                                if (firstObject.has("shipmentid")) {
                                    // 🚚 Shipment Info
                                    for (int i = 0; i < jsonArray.length(); i++) {
                                        JSONObject shipment = jsonArray.getJSONObject(i);

                                        String shipmentId = shipment.optString("shipmentid", "");
                                        String date = shipment.optString("Date", "");
                                        String gcNo = shipment.optString("GcNo", "");
                                        String invoiceNo = shipment.optString("InvoiceNo", "");
                                        String partyName = shipment.optString("PartyName", "");
                                        String from = shipment.optString("From", "");
                                        String unload = shipment.optString("Unload", "");
                                        String tonnage = shipment.optString("Tonnage", "");
                                        String vehicleNo = shipment.optString("VehicleNo", "");
                                        String vehicleType = shipment.optString("VehicleType", "");
                                        String shipmentType = shipment.optString("ShipmentType", "");
                                        String vehicleID = shipment.optString("VehicleID","");

                                        vehicleNo = vehicleNo.replace(" ", "");

                                        Log.v("EwayBillVehicleNo","EwayBillVehicleNo"+vehicleNo);
                                        fetchEwayBillExpireDate(vehicleNo);

                                        Log.d(TAG, "vehicleID: " + vehicleID);

                                        String vehicleNumber = getVehicleNumberById(vehicleID);
                                        Log.v("VehicleNumberText","VehicleNummberText"+vehicleNumber);
                                        if (!vehicleNumber.isEmpty()) {
                                            VehicleNumberTextInputEditText.setText(vehicleNumber);
                                        } else {
                                            VehicleNumberTextInputEditText.setText(""); // fallback
                                        }

//                                        VehicleNumberTextInputEditText.setText(vehicleNo);
                                        InvoiceNumberEditText.setText(invoiceNo);
                                        PartyNameEditText.setText(partyName);

                                        if (partyName != null && !partyName.isEmpty() &&
                                                shipmentType != null && !shipmentType.isEmpty()) {
                                            Log.i(TAG, "Calling fetchUnloadLocation: " + partyName + " | " + shipmentType);
                                            fetchUnloadLocation(partyName, shipmentType);
                                        } else {
                                            Log.w(TAG, "Skipped fetchUnloadLocation: empty PartyName or ShipmentType");
                                        }
                                    }

                                } else if (firstObject.has("HSDDate")) {
                                    // ⛽ Diesel Info
                                    JSONObject diesel = jsonArray.getJSONObject(0);
                                    String hsdDate = diesel.optString("HSDDate", "");
                                    double hsd = diesel.optDouble("HSD", 0.0);
                                    String bunk = diesel.optString("Bunk", "");
                                    Log.d(TAG, "Diesel Info: " + hsd + " from " + bunk);

                                } else if (firstObject.has("driverid")) {
                                    // 👨‍✈️ Driver Info
                                    JSONObject driver = jsonArray.getJSONObject(0);
                                    String vehicleNo = driver.optString("VehicleNo", "");
                                    if (!vehicleNo.isEmpty()) {
                                        String trimVeh = vehicleNo.replaceAll("[- BT]+$", "");
                                        Log.d(TAG, "Trimmed Vehicle No: " + trimVeh);
                                    }
                                }
                            }

                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing JSON response", e);
                            progressDialog.dismiss();
                            showAlertDialog("Failed","Failed To Get Response, Try Again");
                        }
                    } else {
                        Log.e(TAG, "Failed to get a successful response");
                        showAlertDialog("Failed", "Failed to get Data");
                        progressDialog.dismiss();
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                    submitButton.setEnabled(true);
                    progressDialog.dismiss();
                    Log.e(TAG, "Failed to make a POST request", t);
                    showAlertDialog("Failed","Network Failure, Try Again");
                }
            });
        } catch (Exception e) {
            submitButton.setEnabled(true);
            Log.e(TAG, "Exception occurred", e);
            showAlertDialog("Failed","Network Failure, Try Again");
        }
    }

    private String getVehicleNumberById(String vehicleId) {
        if (vehicleId == null || vehicleId.isEmpty()) return "";
        for (Map.Entry<String, Integer> entry : VehicleIDs.entrySet()) {
            if (String.valueOf(entry.getValue()).equals(vehicleId)) {
                return entry.getKey(); // return VehicleNumber
            }
        }
        return "";
    }


    private void fetchVehicleDocuments(int vehicleId) {
        ApiService apiService;
        try {
            apiService = RetrofitAPIServiceInstance.getApiService(requireContext());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage("Fetching vehicle documents...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        Call<List<VehicleDocument>> call = apiService.getVehicleDocuments(vehicleId);
        call.enqueue(new Callback<List<VehicleDocument>>() {
            @Override
            public void onResponse(Call<List<VehicleDocument>> call, Response<List<VehicleDocument>> response) {
                progressDialog.dismiss();

                if (response.isSuccessful() && response.body() != null) {
                    List<VehicleDocument> docs = response.body();

                    // Create scrollable layout
                    ScrollView scrollView = new ScrollView(requireContext());
                    LinearLayout layout = new LinearLayout(requireContext());
                    layout.setOrientation(LinearLayout.VERTICAL);
                    layout.setPadding(50, 40, 50, 40);
                    scrollView.addView(layout);

                    // Map to hold TextViews for each doc type
                    Map<String, TextView> docMap = new HashMap<>();
                    docMap.put("RC", addDocRow(layout, "RC Expire Date"));
                    docMap.put("FC", addDocRow(layout, "FC Expire Date"));
                    docMap.put("Pollution", addDocRow(layout, "Pollution Expire Date"));
                    docMap.put("Insurance", addDocRow(layout, "Insurance Expire Date"));
                    docMap.put("VehicleNationalPermit", addDocRow(layout, "National Permit"));
                    docMap.put("KAHomeTax", addDocRow(layout, "KA Home Tax"));
                    docMap.put("VehicleStatePermit", addDocRow(layout, "KA Home State Permit"));
                    docMap.put("Authorization", addDocRow(layout, "Authorization"));

                    // Fill data from API
                    for (VehicleDocument doc : docs) {
                        String key = doc.getVehicleDoc();
                        TextView field = docMap.get(key);
                        if (field != null) {
                            String formattedDate = formatDate(doc.getDocExpireDate());
                            field.setText(formattedDate);
                        }
                    }

                    // Show dialog
                    new AlertDialog.Builder(requireContext())
                            .setTitle("🚛 Vehicle Documents")
                            .setView(scrollView)
                            .setPositiveButton("OK", null)
                            .show();

                } else {
                    Toast.makeText(requireContext(), "Failed to fetch document data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<VehicleDocument>> call, Throwable t) {
                progressDialog.dismiss();
                Toast.makeText(requireContext(), "API failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }


    public void clearFields()
    {
        VehicleNumberTextInputEditText.setText("");
        PartyNameEditText.setText("");
        UnloadLocationEditText.setText("");
        UnloadLocationUrlText.setText("");
        EwayBillExpireDateText.setText("");
        InvoiceNumberEditText.setText("");
    }
    public void fetchEwayBillExpireDate(String VehicleNo) {
        ApiService apiService;
        try {
            apiService = RetrofitAPIServiceInstance.getApiService(requireContext());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Call<List<EwayBillExpireDate>> call = apiService.getEwayBillExpireDate(VehicleNo);
        call.enqueue(new Callback<List<EwayBillExpireDate>>() {
            @Override
            public void onResponse(Call<List<EwayBillExpireDate>> call, Response<List<EwayBillExpireDate>> response) {

                if (response.isSuccessful() && response.body() != null) {

                    List<EwayBillExpireDate> list = response.body();

                    if (!list.isEmpty()) {
                        EwayBillExpireDate model = list.get(0);   // Get first item

                        String validUpto = model.getValidUpto();  // ← Get ValidUpto
                        String formattedDate = formatValidUpto(validUpto);
                        EwayBillExpireDateText.setText(formattedDate);



//                        Toast.makeText(requireContext(),
//                                "Valid Upto: " + validUpto,
//                                Toast.LENGTH_LONG).show();

                    } else {
                        Toast.makeText(requireContext(), "No data found", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    Toast.makeText(requireContext(), "Failed to fetch document data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<EwayBillExpireDate>> call, Throwable t) {
                Toast.makeText(requireContext(), "API failed: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }


    private String formatValidUpto(String dateStr) {
        try {
            // Input format from API
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());

            // Output format you want
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());

            Date date = inputFormat.parse(dateStr);
            return outputFormat.format(date);

        } catch (Exception e) {
            return dateStr;
        }
    }


    private TextView addDocRow(LinearLayout layout, String labelText) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 15, 0, 15);
        row.setWeightSum(2);

        TextView label = new TextView(requireContext());
        label.setText(labelText);
        label.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        label.setTextSize(16);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black));

        TextView value = new TextView(requireContext());
        value.setText("—");
        value.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        value.setGravity(Gravity.END);
        value.setTextSize(16);
        value.setTextColor(ContextCompat.getColor(requireContext(), R.color.purple_500));

        row.addView(label);
        row.addView(value);

        View divider = new View(requireContext());
        divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.light_green));

        layout.addView(row);
        layout.addView(divider);

        return value;
    }

    private String formatDate(String isoDate) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault());
            Date date = inputFormat.parse(isoDate);
            return outputFormat.format(date);
        } catch (Exception e) {
            return isoDate;
        }
    }

    public void fetchUnloadLocation(String PartyName, String ShipmentType) throws Exception {
        Log.v("FetchUnload", "PartyName: " + PartyName);
        Log.v("FetchUnload", "ShipmentType: " + ShipmentType);

        ApiService apiService = RetrofitAPIServiceInstance.getApiService(requireContext());
        Call<List<UnloadDetails>> call = apiService.getUnloadForShipment(PartyName, ShipmentType);

        call.enqueue(new Callback<List<UnloadDetails>>() {
            @Override
            public void onResponse(Call<List<UnloadDetails>> call, Response<List<UnloadDetails>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<UnloadDetails> unloadList = response.body();

                    if (unloadList != null && !unloadList.isEmpty()) {
                        for (UnloadDetails unload : unloadList) {
                            Log.d("UnloadDetails",
                                    "TripID: " + unload.getTripid() +
                                            ", Party: " + unload.getPartyName() +
                                            ", UnloadName: " + unload.getUnloadName() +
                                            ", From: " + unload.getFromLocation() +
                                            ", To: " + unload.getToPlace() +
                                            ", LocationUrl: " + unload.getLocationUrl());
                            String UnloadName = unload.getUnloadName();
                            LocationUrl = unload.getLocationUrl();
                            UnloadLocationEditText.setText(UnloadName);
                            UnloadLocationUrlText.setText(LocationUrl);

                        }
                    } else {
                        Log.w("FetchUnload", "No unload data found for this Party and Shipment Type.");
                    }

                } else {
                    Log.e("FetchUnload", "Response unsuccessful. Code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<List<UnloadDetails>> call, Throwable throwable) {
                Log.e("FetchUnload", "API call failed: " + throwable.getMessage(), throwable);
            }
        });
    }

    private void openGoogleMap() {
        if (LocationUrl == null || LocationUrl.isEmpty()) {
            showAlertDialog("Failed", "Location Not Found");
            return; // ✅ Stop execution if URL is missing
        }
        String url =  LocationUrl;
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }


    private void showAlertDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(title)
                .setMessage(message)
                .setIcon(R.drawable.baseline_error_24)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }
}