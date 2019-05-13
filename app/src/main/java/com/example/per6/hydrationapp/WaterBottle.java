package com.example.per6.hydrationapp;


import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class WaterBottle implements Parcelable {
    private String bottleName;
    private List<BottleFillDataPoint> bottleFillDataPoints;
    private int capacity;
    private String objectId;


    public WaterBottle(String bottleName, List<BottleFillDataPoint> bottleFillValues) {
        this.bottleName = bottleName;
        this.bottleFillDataPoints = bottleFillValues;
    }

    public WaterBottle() {
        bottleFillDataPoints=new ArrayList<>();
    }

    public String getBottleName() {
        return bottleName;
    }

    public void setBottleName(String bottleName) {
        this.bottleName = bottleName;
    }

    public List<BottleFillDataPoint> getBottleFillDataPoints() {
        return bottleFillDataPoints;
    }

    public void setBottleFillDataPoints(List<BottleFillDataPoint> bottleFillValues) {
        this.bottleFillDataPoints = bottleFillValues;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public String getObjectId() {
        return objectId;
    }
    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public void addBottleFillDataPoint(BottleFillDataPoint bottleFillDataPoint){
        bottleFillDataPoints.add(bottleFillDataPoint);
    }


    public void clearBottleFillDataPoint(){
        bottleFillDataPoints.clear();
    }

    protected WaterBottle(Parcel in) {
        bottleName = in.readString();
        if (in.readByte() == 0x01) {
            bottleFillDataPoints = new ArrayList<BottleFillDataPoint>();
            in.readList(bottleFillDataPoints, BottleFillDataPoint.class.getClassLoader());
        } else {
            bottleFillDataPoints = null;
        }
        capacity = in.readInt();
        objectId = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(bottleName);
        if (bottleFillDataPoints == null) {
            dest.writeByte((byte) (0x00));
        } else {
            dest.writeByte((byte) (0x01));
            dest.writeList(bottleFillDataPoints);
        }
        dest.writeInt(capacity);
        dest.writeString(objectId);
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