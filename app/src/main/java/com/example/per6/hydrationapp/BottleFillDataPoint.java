package com.example.per6.hydrationapp;

import android.os.Parcel;
import android.os.Parcelable;

public class BottleFillDataPoint implements Parcelable {
    double bottleFillDataPoint;

    public BottleFillDataPoint() {
    }

    public double getBottleFillDataPoint() {
        return bottleFillDataPoint;
    }

    public void setBottleFillDataPoint(double bottleFillDataPoint) {
        this.bottleFillDataPoint = bottleFillDataPoint;
    }

    public BottleFillDataPoint(double bottleFillDataPoint) {
        this.bottleFillDataPoint = bottleFillDataPoint;
    }

    protected BottleFillDataPoint(Parcel in) {
        bottleFillDataPoint = in.readDouble();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(bottleFillDataPoint);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<BottleFillDataPoint> CREATOR = new Parcelable.Creator<BottleFillDataPoint>() {
        @Override
        public BottleFillDataPoint createFromParcel(Parcel in) {
            return new BottleFillDataPoint(in);
        }

        @Override
        public BottleFillDataPoint[] newArray(int size) {
            return new BottleFillDataPoint[size];
        }
    };
}