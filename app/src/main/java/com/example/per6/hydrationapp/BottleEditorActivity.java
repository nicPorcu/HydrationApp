package com.example.per6.hydrationapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;

import java.util.ArrayList;
import java.util.List;

public class BottleEditorActivity extends AppCompatActivity {

    private static final String TAG = "BottleEditorActivity";
    private EditText waterBottleName, waterBottleCapacity;
    private Button editButton,calibrateButton, submitButton;
    private Boolean editMode, hasBeenCalibrated;
    private WaterBottle waterBottle;
    private String singlePeripheralIdentifier;
    private Context context;
    private static final int calibrationActivityRequestCode=23;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bottle_editor);
        Intent i = getIntent();
        editMode = i.getBooleanExtra("editMode", false);

        waterBottle =  i.getParcelableExtra("waterBottle");
        singlePeripheralIdentifier = i.getStringExtra("singlePeripheralIdentifier");
        wireWidgets();
        setOnClickListeners();
        setEditMode();

    }




    private void wireWidgets() {
        context = this;
        waterBottleName = findViewById(R.id.water_bottle_name);
        waterBottleCapacity = findViewById(R.id.capacity_edittext);
        editButton = findViewById(R.id.edit);
        calibrateButton = findViewById(R.id.calibrate);
        submitButton = findViewById(R.id.button_submit);

        if(!(waterBottle.getBottleName()==null)) {
            waterBottleName.setText(waterBottle.getBottleName());
            waterBottleCapacity.setText(waterBottle.getCapacity() + "");
        }



    }


    private void setOnClickListeners() {
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editMode=true;
                setEditMode();

            }
        });
        calibrateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                waterBottle.setBottleName(waterBottleName.getText().toString());
                waterBottle.setCapacity(Integer.parseInt(waterBottleCapacity.getText().toString()));
                Intent i = new Intent(context, CalibrationActivity.class);
                i.putExtra(getString(R.string.water_bottle), waterBottle);
                i.putExtra("singlePeripheralIdentifier", singlePeripheralIdentifier);
                startActivityForResult(i, calibrationActivityRequestCode);
            }
        });
        submitButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                waterBottle.setBottleName(waterBottleName.getText().toString());
                waterBottle.setCapacity(Integer.parseInt(waterBottleCapacity.getText().toString()));
                checkCalibration();
                if(hasBeenCalibrated) {
                    backendlessUpdateBottle();
                    Intent resultIntent = new Intent();
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                }else {
                    Toast.makeText(context, "Please Calibrate Water Bottle", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void checkCalibration() {
        if(waterBottle.getCapacity()!=waterBottle.getBottleFillDataPoints().size()){
            Log.d(TAG, "checkCalibration: "+waterBottle.getBottleFillDataPoints().size());
            hasBeenCalibrated=false;
        }
        else{
            hasBeenCalibrated=true;
        }
    }



    private void backendlessUpdateBottle() {
        waterBottle.setBottleName(waterBottleName.getText().toString());
        waterBottle.setCapacity(Integer.parseInt(waterBottleCapacity.getText().toString()));


        saveBottle();



    }

    private void saveDataPoints() {
        List<BottleFillDataPoint> bottleFillValues=waterBottle.getBottleFillDataPoints();

        List<BottleFillDataPoint> responseList= new ArrayList<>();

        final int bottleFillValuesSize= bottleFillValues.size();

        for(BottleFillDataPoint t: bottleFillValues) {

            Backendless.Persistence.save(t, new AsyncCallback<BottleFillDataPoint>() {
                @Override
                public void handleResponse(BottleFillDataPoint response) {
                    Log.d(TAG, "handleResponse: dataPointsSaved");
                    responseList.add(response);
                    if (responseList.size() == bottleFillValuesSize)
                        setChildren(responseList);
                     }

                   @Override
                     public void handleFault(BackendlessFault fault) {
                        Log.d(TAG, "handleFault: " + fault.getMessage());
                    }

            });
        }
    }

    private void saveBottle() {
        Backendless.Persistence.save(waterBottle, new AsyncCallback<WaterBottle>() {
            public void handleResponse(WaterBottle response) {
                Toast.makeText(BottleEditorActivity.this, "yay", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "handleResponse: bottle saved"+ response.getBottleName());
                saveDataPoints();
            }

            public void handleFault(BackendlessFault fault) {
                Log.d(TAG, "handleFault: error " + fault.getMessage());
                // an error has occurred, the error code can be retrieved with fault.getCode()
            }

        });
    }

    private void setChildren(List<BottleFillDataPoint> bottleFillValues) {
        Backendless.Data.of( WaterBottle.class ).setRelation( waterBottle, "bottleFillDataPoints", waterBottle.getBottleFillDataPoints(),
                new AsyncCallback<Integer>()
                {
                    @Override
                    public void handleResponse( Integer response )
                    {

                        Log.i( TAG, "handleResponse: yay");
                    }

                    @Override
                    public void handleFault( BackendlessFault fault )
                    {
                        Log.d(TAG, "handleFault: "+fault.getMessage());
                        Log.d(TAG, "handleFault: "+fault.getDetail());
                    }
                } );
    }


    private void setEditMode() {
        if(editMode){
            calibrateButton.setVisibility(View.VISIBLE);
            submitButton.setVisibility(View.VISIBLE);
            editButton.setVisibility(View.INVISIBLE);
            waterBottleName.setEnabled(true);
            waterBottleCapacity.setEnabled(true);


        } else{
            calibrateButton.setVisibility(View.INVISIBLE);
            submitButton.setVisibility(View.INVISIBLE);
            editButton.setVisibility(View.VISIBLE);
            waterBottleName.setEnabled(false);
            waterBottleCapacity.setEnabled(false);


        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==RESULT_OK){
            if(requestCode==calibrationActivityRequestCode){
                hasBeenCalibrated=true;
                waterBottle=data.getParcelableExtra(getString(R.string.water_bottle));
                Log.d(TAG, "onActivityResult: bottleName"+ waterBottle.getBottleName());
                Log.d(TAG, "onActivityResult: calibrateActivity");
                Log.d(TAG, "onActivityResult: "+waterBottle.getBottleFillDataPoints().size());

                backendlessUpdateBottle();
            }
        }

    }
}
