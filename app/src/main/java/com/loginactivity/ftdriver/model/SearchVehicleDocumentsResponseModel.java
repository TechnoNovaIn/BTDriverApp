package com.loginactivity.ftdriver.model;

public class SearchVehicleDocumentsResponseModel {
    private String DocumentId;
    private String DocumentName;
    private String Path;
    private String Remarks;
    private String VehicleDocumentType;
    private String Year;
    private String VehicleDocumentTypeID;
    private String VehicleId;
    private String VehicleNo;

    public String getDocumentName() {
        return DocumentName;
    }
    public String getPath() {
        return Path;
    }
    public String getRemarks() {
        return Remarks;
    }
    public String getVehicleDocumentType() {
        return VehicleDocumentType;
    }
    public String getYear() {
        return Year;
    }
    public String getVehicleNumber(){return VehicleNo; }
}
