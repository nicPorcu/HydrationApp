package com.example.per6.hydrationapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class CalibrationActivity extends AppCompatActivity {
    private FloatingActionButton nextButton;
    private Button finish;
    private int fullBottle;
    private TextView instructions;
    private int[] measurements;
    private int mesurementNumber;
    private WaterBottle waterBottle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);
        Intent i= getIntent();
        waterBottle= i.getParcelableExtra("waterBottle");
        wireWidgets();

    }

    private void wireWidgets() {
        fullBottle=24; //todo update
        mesurementNumber=0;
        measurements = new int[fullBottle];
        nextButton = findViewById(R.id.nextButton);
        nextButton.setOnClickListener(view -> {
            if(mesurementNumber < fullBottle){
                instructions.setText("Pour 1oz of water into the bottle, screw the lid back on and press next, press done when the water bottle is full");
                //todo sent uart 0
                measurements[mesurementNumber] = 120; //placeholder
                mesurementNumber ++;
            } else {
                instructions.setText("Thank you! Now your water bottle is ready for use");
            }

        });

        finish=findViewById(R.id.doneButton);
        finish.setText("Finish");
        finish.setOnClickListener(view -> {
            //todo save in backendless
        });

        //todo make sure you are connected to kiwi companion
        instructions=findViewById(R.id.instruction);
        instructions.setText("Ready to Calibrate?\n Empty your water bottle and place the Kiwi Companion lid on and turn it on.\n Press next when you have done this");
    }
}
