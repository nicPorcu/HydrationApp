package com.example.per6.hydrationapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;

import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;
import static com.example.per6.hydrationapp.ConnectedPeripheralFragment.createFragmentArgs;

public class BottleEditorFragment extends Fragment {

    private static final String TAG = "BottleEditorFragment";
    private static String singlePeripheralIdentifierMaster;
    private EditText waterBottleName, waterBottleCapacity;
    private Button editButton,calibrateButton, submitButton;
    private Boolean editMode, hasBeenCalibrated;
    private WaterBottle waterBottle;

    private Context context;
    private View rootView;
    private static final int calibrationActivityRequestCode=23;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, //Use as onCreate
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.activity_bottle_editor, container, false);
        waterBottle=new WaterBottle();
        wireWidgets();
        setOnClickListeners();
        setEditMode();

        //dogChange(0.0);
        //imageDog.setImageResource(R.drawable.ic_menu_camera);
        return rootView;
    }

    public static BottleEditorFragment newInstance(@Nullable String peripheralIdentifier) {
        BottleEditorFragment fragment = new BottleEditorFragment();
        fragment.setArguments(createFragmentArgs(peripheralIdentifier));
        singlePeripheralIdentifierMaster = peripheralIdentifier;

        return fragment;
    }




    private void wireWidgets() {
        context = getContext();
        waterBottleName = rootView.findViewById(R.id.water_bottle_name);
        waterBottleCapacity = rootView.findViewById(R.id.capacity_edittext);
        editButton = rootView.findViewById(R.id.edit);
        calibrateButton = rootView.findViewById(R.id.calibrate);
        submitButton = rootView.findViewById(R.id.button_submit_myinfo);






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
                i.putExtra("singlePeripheralIdentifier", singlePeripheralIdentifierMaster);
                startActivityForResult(i, calibrationActivityRequestCode);
            }
        });
        submitButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if(waterBottleName.getText().toString()!=""&& waterBottleCapacity.getText().toString()!="") {
                    waterBottle.setBottleName(waterBottleName.getText().toString());
                    waterBottle.setCapacity(Integer.parseInt(waterBottleCapacity.getText().toString()));
                    checkCalibration();
                    if (hasBeenCalibrated) {
                        backendlessUpdateBottle();
                        Intent resultIntent = new Intent();
                        ((SetupActivity) getActivity()).onReturnToActivity();

                    } else {
                        Toast.makeText(context, "Please Calibrate Water Bottle", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(context, "Please enter Information", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(getContext(), "yay", Toast.LENGTH_SHORT).show();
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

            calibrateButton.setVisibility(View.VISIBLE);
            submitButton.setVisibility(View.VISIBLE);
            editButton.setVisibility(View.INVISIBLE);
            waterBottleName.setEnabled(true);
            waterBottleCapacity.setEnabled(true);


    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
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
