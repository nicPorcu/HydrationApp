package com.example.per6.hydrationapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;

import static com.example.per6.hydrationapp.ConnectedPeripheralFragment.createFragmentArgs;


public class MyInfoFragment extends Fragment {
    private static final String TAG = "MyInfoFragment";
    private static String singlePeripheralIdentifierMaster;

    private View rootview;
    private ToggleButton areYaPregnantButton;
    private EditText weightEditText, exerciseEditText;
    private TextView estimateTextView;
    private Button submitButton;
    private int dailyWaterGoal;
    private Context context;
    private SharedPreferences sharedPref;
    private TextView infoTextView;
    private SharedPreferences.Editor editor;
    private BackendlessUser user;



    public MyInfoFragment() {
        // Required empty public constructor
    }


    public static MyInfoFragment newInstance(@Nullable String peripheralIdentifier) {
        MyInfoFragment fragment = new MyInfoFragment();
        fragment.setArguments(createFragmentArgs(peripheralIdentifier));
        singlePeripheralIdentifierMaster = peripheralIdentifier;
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: oncreatev 1");

        // Inflate the layout for this fragment
        rootview=inflater.inflate(R.layout.fragment_my_info, container, false);
        context=getContext();
        wireWidgets();
        sharedPref = context.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        user= Backendless.UserService.CurrentUser();
        Log.d(TAG, "onCreateView: oncreatev 2");

        getSharedPref();

        Log.d(TAG, "onCreateView: oncreatev ");

        return rootview;

    }


    private void wireWidgets() {
        Log.d(TAG, "wireWidgets: wirewidgets 1");
         submitButton= rootview.findViewById(R.id.button_submit_myinfo);
         weightEditText=rootview.findViewById(R.id.weight_edittext);
         exerciseEditText=rootview.findViewById(R.id.exercise_edittext);
         areYaPregnantButton=rootview.findViewById(R.id.pregnancy_togglebutton);
         areYaPregnantButton.setTextOn(getString(R.string.yes));
         areYaPregnantButton.setTextOff(getString(R.string.no));
         estimateTextView=rootview.findViewById(R.id.estimate_textview);
         infoTextView=rootview.findViewById(R.id.info_textview);
         infoTextView.setText("Please enter the following infomation about yourself:");
         submitButton.setOnClickListener(v -> {
             updateDailyWaterGoal();
             Log.d(TAG, "wireWidgets: why am I here");
             ((SetupActivity)(getActivity())).onReturnToActivity();

         });
        Log.d(TAG, "wireWidgets: wirewidgets2");


    }

    private void updateDailyWaterGoal() {
        double dailyExercise, weight;
        boolean isPregnant;
        dailyExercise= Double.parseDouble(exerciseEditText.getText().toString());
        weight= Double.parseDouble(weightEditText.getText().toString());
        isPregnant=areYaPregnantButton.isChecked();

        calculateDailyWaterGoal(weight,dailyExercise, isPregnant);

        estimateTextView.setText("Your goal is to drink "+ dailyWaterGoal + " ounces of water a day");
        estimateTextView.setVisibility(View.VISIBLE);
        updateSharedPreferences(weight, dailyExercise, areYaPregnantButton.isChecked(), dailyWaterGoal);
        updateBackendless();


    }

    private void updateBackendless() {
        user.setProperty("dailyWaterGoal", dailyWaterGoal );
        Backendless.UserService.update( user, new AsyncCallback<BackendlessUser>()
        {
            public void handleResponse( BackendlessUser user )
            {
                Log.d(TAG, "handleResponse: has been updated");
            }

            public void handleFault( BackendlessFault fault )
            {
                Log.d(TAG, "handleFault: "+fault.getMessage());
                // user update failed, to get the error code call fault.getCode()
            }
        });
    }



    private void calculateDailyWaterGoal(double weight, double dailyExercise, boolean isPregnant) {
        dailyWaterGoal = (int) (weight*0.5+ 24*dailyExercise);
        if(isPregnant){
            dailyWaterGoal +=24;
        }
    }

    private void updateSharedPreferences(double weight, double dailyExercise, boolean isPregnant, int dailyWaterGoal) {
        editor = sharedPref.edit();
        editor.putLong("weight", Double.doubleToRawLongBits(weight));
        editor.putLong("dailyExercise", Double.doubleToRawLongBits(dailyExercise));
        editor.putBoolean("isPregnantOrBreastfeeding", isPregnant);
        editor.putInt("dailyWaterGoal", dailyWaterGoal);
        editor.apply();

    }

    private void getSharedPref() {
        double dailyExercise, weight;
        boolean isPregnant;



        weight=getDouble("weight", -298);
        dailyExercise=getDouble("dailyExercise", -279);
        isPregnant= sharedPref.getBoolean("isPregnantOrBreastfeeding", false);
        dailyWaterGoal =sharedPref.getInt("dailyWaterGoal", 0);
       if(weight!=-298&& dailyExercise!=-279) {
           weightEditText.setText(weight + "");
           exerciseEditText.setText(dailyExercise + "");
           areYaPregnantButton.setChecked(isPregnant);
           estimateTextView.setText("Your goal is to drink " + dailyWaterGoal + " ounces of water a day");
       }else{
           try {
               dailyWaterGoal = (Integer) Backendless.UserService.CurrentUser().getProperty("dailyWaterGoal");
               estimateTextView.setText("Your goal is to drink " + dailyWaterGoal + " ounces of water a day");
           }catch (NullPointerException n){
               Log.d(TAG, "getSharedPref: null pointer"+n.getMessage());
               estimateTextView.setVisibility(View.INVISIBLE);

           }
       }

    }




    double getDouble( final String key, final double defaultValue) {
        return Double.longBitsToDouble(sharedPref.getLong(key, Double.doubleToLongBits(defaultValue)));
    }










    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    @Override
    public void onDetach() {
        super.onDetach();

    }




}
