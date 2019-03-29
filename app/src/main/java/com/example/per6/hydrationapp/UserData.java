package com.example.per6.hydrationapp;

public class UserData {


    private int daysLogged;
    private double currentPercent;
    private String name;


    public UserData(){
        daysLogged=0;
        currentPercent=0;
    }
    public int getDaysLogged() {
        return daysLogged;
    }

    public void setDaysLogged(int daysLogged) {
        this.daysLogged = daysLogged;
    }

    public double getCurrentPercent() {
        return currentPercent;
    }

    public void setCurrentPercent(double currentPercent) {
        this.currentPercent = currentPercent;
    }
}
