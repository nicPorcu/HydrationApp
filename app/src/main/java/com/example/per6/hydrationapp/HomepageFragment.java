package com.example.per6.hydrationapp;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;

import static com.example.per6.hydrationapp.ConnectedPeripheralFragment.createFragmentArgs;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * to handle interaction events.
 * Use the {@link HomepageFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomepageFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER




    private static String singlePeripheralIdentifierMaster;

    private View rootView;

    private ImageView imageDog;
    private ProgressBar progressBarWater;
    private TextView textLastSync;
    private double dogLevel;
    private int dailyWaterGoal;
    private int currentWaterConsumption;

    public HomepageFragment() {
        // Required empty public constructor
    }
    public static HomepageFragment newInstance(@Nullable String peripheralIdentifier) {
        HomepageFragment fragment = new HomepageFragment();
        fragment.setArguments(createFragmentArgs(peripheralIdentifier));
        singlePeripheralIdentifierMaster=peripheralIdentifier;

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, //Use as onCreate
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_homepage, container, false);
        dailyWaterGoal= (Integer) Backendless.UserService.CurrentUser().getProperty("dailyWaterGoal");
        currentWaterConsumption=50;//todo change this
        wireWidgets();

        //dogChange(0.0);
        //imageDog.setImageResource(R.drawable.ic_menu_camera);
        return rootView;
    }

    private void wireWidgets() {
        imageDog = rootView.findViewById(R.id.imageDog);
        progressBarWater = rootView.findViewById(R.id.progressBarWater);
        textLastSync = rootView.findViewById(R.id.textLastSync);
        progressBarWater.setMax(dailyWaterGoal);
        progressBarWater.setProgress(currentWaterConsumption);

    }

    private void dogChange(double change){
        dogLevel = 1.5;
        if(!(dogLevel + change > 2) || !(dogLevel + change < 0)){
            dogLevel += change;
        }
        else{
            if(dogLevel + change > 2 ) {dogLevel = 2;}
            else{dogLevel = 0;}
        }
        if(dogLevel < .75) {
            //imageDog.setImageResource(R.drawable.sad_dog_image);
        }
        if(dogLevel >= .75 && dogLevel <= 1.25){
            //imageDog.setImageResource(R.drawable.neutral_dog_image);
        }
        if(dogLevel > 1.25){
            //imageDog.setImageResource(R.drawable.happy_dog_image);
        }
    }

}
