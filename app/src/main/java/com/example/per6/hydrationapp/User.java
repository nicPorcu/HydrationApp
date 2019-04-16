package com.example.per6.hydrationapp;

import java.util.List;

public class User {



    private int currentStreak;
    private double currentPercent;
    private List<String> waterBottles;
    private String name;


    public User(){
        currentStreak =0;
        currentPercent=0;
    }
    public int getCurrentStreak() {
        return currentStreak;
    }

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }

    public double getCurrentPercent() {
        return currentPercent;
    }

    public void setCurrentPercent(double currentPercent) {
        this.currentPercent = currentPercent;
    }

    public List<String> getWaterBottles() {
        return waterBottles;
    }

    public void setWaterBottles(List<String> waterBottles) {
        this.waterBottles = waterBottles;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
