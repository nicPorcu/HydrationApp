package com.example.per6.hydrationapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DataSetObserver;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Switch;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.example.per6.hydrationapp.style.StyledSnackBar;

import static com.example.per6.hydrationapp.ConnectedPeripheralFragment.createFragmentArgs;


public class SettingsFragment extends Fragment {

    private static String singlePeripheralIdentifierMaster;
    private View rootView;
    private Button logOut, connectToBottle;
    private Spinner freq;
    private Switch allow;
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;

    public SettingsFragment() {
        // Required empty public constructor
    }

    public static SettingsFragment newInstance(@Nullable String peripheralIdentifier) {
        SettingsFragment fragment = new SettingsFragment();
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
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_settings, container, false);
        wireWidgets();
        return rootView;

    }

    private void wireWidgets() {

        sharedPref = getActivity().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        editor = sharedPref.edit();

        allow = rootView.findViewById(R.id.allowNotify);
        allow.setChecked(true);
        allow.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked){
                    freq.setVisibility(View.VISIBLE);
                } else {
                    freq.setVisibility(View.GONE);
                }
            }
        });

        freq = rootView.findViewById(R.id.notificationFreq);
        //spiner item
//        String choice = freq.getSelectedItem().toString();
//        editor.putString(getString(R.string.notification_frequency), choice);
//        editor.apply();
        if(!sharedPref.getString(getString(R.string.notification_frequency), "").equals("")){
            //todo set selected item to be shared pref
        }

        logOut = rootView.findViewById(R.id.logOut);
        logOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.clear();
                editor.commit();
                Backendless.UserService.logout(new AsyncCallback<Void>() {
                    @Override
                    public void handleResponse(Void response) {
                        Intent i = new Intent(getActivity(), LoginActivity.class);
                        startActivity(i);
                    }

                    @Override
                    public void handleFault(BackendlessFault fault) {

                    }
                });
            }
        });

        connectToBottle = rootView.findViewById(R.id.connectToBottleButton);
        connectToBottle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(singlePeripheralIdentifierMaster == null) {
                    Intent i = new Intent(getActivity(), BluetoothActivity.class);
                    startActivity(i);
                } else {
                    Snackbar snackbar = Snackbar.make(view, "You are already connected, would you like to disconnect", Snackbar.LENGTH_LONG);
                    snackbar.setAction(R.string.pleaseDisconnect, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent i = new Intent(getActivity(), BluetoothActivity.class);
                            startActivity(i);
                        }
                    });
                    snackbar.show();
                }
            }
        });


    }

}
