package com.example.per6.hydrationapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.example.per6.hydrationapp.LeaderboardFragment.LeaderboardFragment;
import com.example.per6.hydrationapp.ble.BlePeripheral;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "MainActivity";
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    private Context context;
    private Fragment currentFragment;
    private FragmentManager fm;
    private static final int bluetoothActivityRequestCode=3;

    private boolean hasUserAlreadyBeenAskedAboutBluetoothStatus = false;
    private boolean isConnectedToBluetooth;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        context = this;
        sharedPref = context.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        fm = getSupportFragmentManager();

        if (savedInstanceState == null) {
            Intent i = new Intent(this, BluetoothActivity.class);
            startActivityForResult(i, bluetoothActivityRequestCode );
        } else {
            //todo fix
//            hasUserAlreadyBeenAskedAboutBluetoothStatus = savedInstanceState.getBoolean("hasUserAlreadyBeenAskedAboutBluetoothStatus");
//            mMainFragment = (MainFragment) fm.findFragmentByTag("Main");
//            fm.beginTransaction()
//                    .add(R.id.contentLayout, mMainFragment, "Main")
//                    .commit();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            currentFragment = new HomepageFragment();

        } else if (id == R.id.nav_leaderboard) {
            currentFragment = new LeaderboardFragment();

        } else if (id == R.id.nav_myInfo) {
            currentFragment = new MyWaterBottlesFragment();

        } else if (id == R.id.nav_customization) {
            currentFragment = new CustomizationFragment();

        } else if (id == R.id.nav_settings) {
            currentFragment = new SettingsFragment();

        }
        if(currentFragment != null){
            fm.beginTransaction()
                    .replace(R.id.fragment_container, currentFragment)
                    .commit();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("hasUserAlreadyBeenAskedAboutBluetoothStatus", hasUserAlreadyBeenAskedAboutBluetoothStatus);
    }

    // endregion

    @Override
    protected void onResume() {
        super.onResume();

        // Observe disconnections
        registerGattReceiver();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

  //      popFragmentsIfNoPeripheralsConnected();         // check if peripherals were disconnected while the app was in background
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterGattReceiver();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == bluetoothActivityRequestCode){
            if(resultCode == RESULT_OK) {
                String peripheralIdentifier=data.getStringExtra("peripheralIdentifier");

                if(peripheralIdentifier.equals("")){
                    isConnectedToBluetooth = false;
                }else{
                    isConnectedToBluetooth = true;
                }
                HomepageFragment fragment = HomepageFragment.newInstance(peripheralIdentifier);
                FragmentManager fragmentManager = getSupportFragmentManager();
                if (fragmentManager != null) {
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_right, R.anim.slide_out_left)
                            .replace(R.id.fragment_container, fragment, "Modules");
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                }
            }


        }
    }

    // region Broadcast Listener
    private void registerGattReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BlePeripheral.kBlePeripheral_OnDisconnected);
        LocalBroadcastManager.getInstance(this).registerReceiver(mGattUpdateReceiver, filter);
    }

    private void unregisterGattReceiver() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mGattUpdateReceiver);
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BlePeripheral.kBlePeripheral_OnDisconnected.equals(action)) {
            }
        }
    };
}
