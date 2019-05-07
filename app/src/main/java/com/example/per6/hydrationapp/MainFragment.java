package com.example.per6.hydrationapp;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;

public class MainFragment extends Fragment {
    /*Helper class for scanner fragment*/
    // Log
    private final static String TAG = MainFragment.class.getSimpleName();

    // Data
    private WeakReference<Fragment> mCurrentFragmentReference;
    private int selectedFragmentId = 0;
    private boolean mIsInitialNavigationItemSelected = false;

    // region Fragment Lifecycle
    public MainFragment() {
        // Required empty public constructor
    }

    public static MainFragment newInstance() {
        return new MainFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retain this fragment across configuration changes
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //instantly starts the scanning fragment
        Fragment selectedFragment = ScannerFragment.newInstance();
        FragmentManager fragmentManager = getChildFragmentManager();

        FragmentTransaction transaction = fragmentManager.beginTransaction();

        transaction.replace(R.id.navigationContentLayout, selectedFragment).commit();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        FragmentActivity activity = getActivity();
        if (activity != null) {
            // update options menu with current values
            activity.invalidateOptionsMenu();
            setActionBarTitle("Watermelon Hydration");
        }
    }

    // endregion

    // region Fragments
    private Fragment getCurrentFragment() {
        return mCurrentFragmentReference == null ? null : mCurrentFragmentReference.get();
    }


    private void setActionBarTitle(String title) {
        AppCompatActivity activity = ((AppCompatActivity) getActivity());
        if (activity != null) {
            ActionBar actionBar = activity.getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(title);
                actionBar.setDisplayHomeAsUpEnabled(false);     // Don't show caret for MainFragment
            }
        }
    }

    // endregion

    // region Actions
    public void startScanning() {
        // Send the message to the peripheral mode fragment, or ignore it if is not selected
        if (getCurrentFragment() instanceof ScannerFragment) {
            ((ScannerFragment) getCurrentFragment()).startScanning();
        }
    }

    public void disconnectAllPeripherals() {
        // Send the message to the peripheral mode fragment, or ignore it if is not selected
        if (getCurrentFragment() instanceof ScannerFragment) {
            ((ScannerFragment) getCurrentFragment()).disconnectAllPeripherals();
        }
    }

    // endregion
}