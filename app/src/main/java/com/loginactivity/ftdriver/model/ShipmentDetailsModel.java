package com.loginactivity.ftdriver.model;

public class ShipmentDetailsModel {

    private int ShipmentId;
    private String Date;
    private String GcNo;
    private String InvoiceNo;
    private String PartyName;
    private String From;
    private String Unload;
    private String Tonnage;
    private int Vehicleid;
    private String Type;
    private String EWBNo;

    private String POD;

    public int getShipmentId() {
        return ShipmentId;
    }

    public String getDate() {
        return Date;
    }

    public String getGcNo() {
        return GcNo;
    }

    public String getInvoiceNo() {
        return InvoiceNo;
    }

    public String getPartyName() {
        return PartyName;
    }

    public String getFrom() {
        return From;
    }

    public String getUnload() {
        return Unload;
    }

    public String getTonnage() {
        return Tonnage;
    }

    public int getVehicleid() {
        return Vehicleid;
    }

    public String getType() {
        return Type;
    }

    public String getEWBNo() {
        return EWBNo;
    }

    public String getPOD(){
        return POD;
    }
}
