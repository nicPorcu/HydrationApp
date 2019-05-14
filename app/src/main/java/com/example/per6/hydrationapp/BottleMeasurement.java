package com.example.per6.hydrationapp;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class BottleMeasurement {
    private final Date d;
    private long timeStamp;
    private int measurement;
    private WaterBottle bottle;
    private int maxIndex, minIndex;
    private boolean minSet, maxSet;

    public BottleMeasurement(long timeStamp, int measurement, WaterBottle bottle) {
        this.timeStamp = timeStamp;
        this.measurement = measurement;
        this.bottle = bottle;

        //sets up time stamp
        d = new Date (timeStamp);

        //sets up searching
        minSet = false;
        maxSet = false;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public double getOz() {
        return 3;
        //convert from list of BottleFillDataPoints to array of doubles
//        bottle = new WaterBottle();
//        for(int i = 9; i >=0; i++){
//            BottleFillDataPoint b = new BottleFillDataPoint(i);
//        }
//
//        List<BottleFillDataPoint> bottleDataPoints = bottle.getBottleFillDataPoints();
//        double[] ozValues = new double[bottleDataPoints.size()];
//        for(int i = 0; i < bottleDataPoints.size(); i++){
//            ozValues[i] = bottleDataPoints.get(i).getBottleFillDataPoint();
//        }
//        if(!minSet || !maxSet){
//            //prevents searching extra times
//            //binary search that shit
//            minIndex = ozValues.length-1; //last measurement is lowest bc bottle full means smallest distance
//            maxIndex = 0;
//            binarySearch(ozValues);
//        }
//        double ounces = ozValues.length - maxIndex; //40 oz - 24 oz
//        double decimal =  measurement/ (ozValues[maxIndex] + ozValues[minIndex]); //% between min and max measurement actually is
//        ounces = ounces + decimal;
//
//        return ounces;
    }

    private void binarySearch(double[] ozValues) {
        //bigger index = smaller measurement
        //min starts at full bottle = last pos.
        //max starts at empty bottle = 0
        int mid = (int) (minIndex - maxIndex)/2 + maxIndex;
        if(ozValues[mid] >= measurement){ //value at midpoint is larger, ie longer distance ie less water so bring max up to here
            //mid position is at a lower water level than what we lok for, now it is lowest level
            if(ozValues[mid + 1] < measurement){
                //the next measurement is higher, we have the midpoint!
                maxIndex = mid;
                minIndex = mid + 1;
                maxSet = true;
                minSet = true;
            } else {
                maxIndex = mid;
                binarySearch(ozValues);
            }
        } else if(ozValues[mid] <= measurement){
            //mid position is higher water level, becomes new minimum pos., maximum water level
            if(ozValues[mid + 1] > measurement){
                //the next measurement is lower, we have the midpoint!
                maxIndex = mid - 1;
                minIndex = mid;
                maxSet = true;
                minSet = true;
            } else {
                minIndex = mid;
                binarySearch(ozValues);
            }
        }
    }

    public void setMeasurement(int measurement) {
        this.measurement = measurement;
    }

    public WaterBottle getBottle() {
        return bottle;
    }

    public void setBottle(WaterBottle bottle) {
        this.bottle = bottle;
    }

    public String getTime(){
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.US);
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(d);
    }

    public long getDateTime(){
        return d.getTime();
    }

    public String getDate(){
        SimpleDateFormat sdf = new SimpleDateFormat("LL dd,yyyy", Locale.US);
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(d);
    }
}
