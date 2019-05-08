package com.example.per6.hydrationapp;

import android.app.Activity;
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

public class bottleEditorActivity extends AppCompatActivity {

    private static final String TAG = "BottleEditorActivity";
    private EditText waterBottleName, waterBottleCapacity;
    private Button editButton,calibrateButton, submitButton;
    private Boolean editMode;
    private WaterBottle waterBottle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bottle_editor);
        Intent i=getIntent();
        editMode = i.getBooleanExtra("editMode", false);
        waterBottle=  i.getParcelableExtra("waterBottle");
        wireWidgets();
        setOnClickListeners();
        setEditMode();

    }




    private void wireWidgets() {
        waterBottleName=findViewById(R.id.water_bottle_name);
        waterBottleCapacity=findViewById(R.id.capacity_edittext);
        editButton=findViewById(R.id.edit);
        calibrateButton=findViewById(R.id.calibrate);
        submitButton=findViewById(R.id.submit);

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

            }
        });
        submitButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                backendlessUpdateBottle();
                Intent resultIntent = new Intent();
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
            }
        });
    }

    private void backendlessUpdateBottle() {
        waterBottle.setBottleName(waterBottleName.getText().toString());
        waterBottle.setCapacity(Integer.parseInt(waterBottleCapacity.getText().toString()));

        List<BottleFillDataPoint> bottleFillValues=new ArrayList<>();
        bottleFillValues.add(new BottleFillDataPoint(1.2));
        bottleFillValues.add(new BottleFillDataPoint(1.2));
        waterBottle.setBottleFillValues(bottleFillValues);
        saveDataPoints(bottleFillValues);



    }

    private void saveDataPoints(List<BottleFillDataPoint> bottleFillValues) {
        for(BottleFillDataPoint t:bottleFillValues) {

            Backendless.Persistence.save(t, new AsyncCallback<BottleFillDataPoint>() {
                @Override
                public void handleResponse(BottleFillDataPoint response) {
                    Log.d(TAG, "handleResponse: dataPointsSaved");
                    saveBottle();
                }

                @Override
                public void handleFault(BackendlessFault fault) {
                    Log.d(TAG, "handleFault: "+fault.getMessage());
                }
            });
        }
    }

    private void saveBottle() {
        Backendless.Persistence.save(waterBottle, new AsyncCallback<WaterBottle>() {
            public void handleResponse(WaterBottle response) {
                Toast.makeText(bottleEditorActivity.this, "yay", Toast.LENGTH_SHORT).show();
            }

            public void handleFault(BackendlessFault fault) {
                Log.d(TAG, "handleFault: error " + fault.getMessage());
                // an error has occurred, the error code can be retrieved with fault.getCode()
            }

        });
    }

    private void setChildren(List<BottleFillDataPoint> bottleFillValues) {
        Backendless.Data.of( WaterBottle.class ).setRelation( waterBottle, "bottleFillDataPoints", bottleFillValues,
                new AsyncCallback<Integer>()
                {
                    @Override
                    public void handleResponse( Integer response )
                    {

                        Log.i( "MYAPP", "relation has been set");
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


}
