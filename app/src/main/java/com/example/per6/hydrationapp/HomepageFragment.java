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

import static com.example.per6.hydrationapp.ConnectedPeripheralFragment.createFragmentArgs;

public class HomepageFragment extends Fragment {
    public static HomepageFragment newInstance(@Nullable String singlePeripheralIdentifier) {
        HomepageFragment fragment = new HomepageFragment();
        fragment.setArguments(createFragmentArgs(singlePeripheralIdentifier));
        singlePeripheralIdentifierMaster = singlePeripheralIdentifier;
        return fragment;
    }


    private static String singlePeripheralIdentifierMaster;

    private View rootView;

    private ImageView imageDog;
    private ProgressBar progressBarWater;
    private TextView textLastSync;

    private double dogLevel;

    public HomepageFragment() {
        // Required empty public constructor
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
        wireWidgets();
        //dogChange(0.0);
        //imageDog.setImageResource(R.drawable.ic_menu_camera);
        //todo incorporate background sync methods...
        return rootView;
    }

    private void wireWidgets() {
        imageDog = rootView.findViewById(R.id.imageDog);
        progressBarWater = rootView.findViewById(R.id.progressBarWater);
        textLastSync = rootView.findViewById(R.id.textLastSync);
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
