package com.example.per6.hydrationapp;

public class WaterBottle {
    String bottleName;
    String jsonStoreString;

    public WaterBottle(String bottleName, String jsonStoreString) {
        this.bottleName = bottleName;
        this.jsonStoreString = jsonStoreString;
    }

    public WaterBottle() {
    }

    public String getBottleName() {
        return bottleName;
    }

    public void setBottleName(String bottleName) {
        this.bottleName = bottleName;
    }

    public String getJsonStoreString() {
        return jsonStoreString;
    }

    public void setJsonStoreString(String jsonStoreString) {
        this.jsonStoreString = jsonStoreString;
    }
}
