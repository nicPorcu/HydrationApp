package com.example.per6.hydrationapp;

import android.os.Parcel;
import android.os.Parcelable;

public class WaterBottle implements Parcelable {
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

    protected WaterBottle(Parcel in) {
        bottleName = in.readString();
        jsonStoreString = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(bottleName);
        dest.writeString(jsonStoreString);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<WaterBottle> CREATOR = new Parcelable.Creator<WaterBottle>() {
        @Override
        public WaterBottle createFromParcel(Parcel in) {
            return new WaterBottle(in);
        }

        @Override
        public WaterBottle[] newArray(int size) {
            return new WaterBottle[size];
        }
    };
}