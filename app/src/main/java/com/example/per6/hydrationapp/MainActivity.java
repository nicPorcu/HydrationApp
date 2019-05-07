package com.example.per6.hydrationapp;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
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
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.example.per6.hydrationapp.ble.BleManager;
import com.example.per6.hydrationapp.ble.BlePeripheral;
import com.example.per6.hydrationapp.ble.BleUtils;
import com.example.per6.hydrationapp.leaderboard.LeaderboardFragment;
import com.example.per6.hydrationapp.utils.DialogUtils;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, ScannerFragment.ScannerFragmentListener {
    private static final String TAG = "MainActivity";
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    private Context context;
    private Fragment currentFragment;
    private MainFragment mMainFragment;
    private FragmentManager fm;

    // Permission requests
    private final static int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    public final static int PERMISSION_REQUEST_FINE_LOCATION = 2;

    // Activity request codes (used for onActivityResult)
    private static final int kActivityRequestCode_EnableBluetooth = 1;
    public static final int kActivityRequestCode_PlayServicesAvailability = 2;

    private AlertDialog mRequestLocationDialog;
    private boolean hasUserAlreadyBeenAskedAboutBluetoothStatus = false;



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
            // Set mainmenu fragment
            mMainFragment = MainFragment.newInstance();
            fm.beginTransaction()
                    .add(R.id.contentLayout, mMainFragment, "Main")
                    .commit();

        } else {
            hasUserAlreadyBeenAskedAboutBluetoothStatus = savedInstanceState.getBoolean("hasUserAlreadyBeenAskedAboutBluetoothStatus");
            mMainFragment = (MainFragment) fm.findFragmentByTag("Main");
            fm.beginTransaction()
                    .add(R.id.contentLayout, mMainFragment, "Main")
                    .commit();
        }
        // Back navigation listener
        fm.addOnBackStackChangedListener(() -> {
            if (fm.getBackStackEntryCount() == 0) {        // Check if coming back
                mMainFragment.disconnectAllPeripherals();
            }
        });
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
            currentFragment=new HomepageFragment();

        } else if (id == R.id.nav_leaderboard) {
            currentFragment=new LeaderboardFragment();

        } else if (id == R.id.nav_myInfo) {
            currentFragment=new MyWaterBottlesFragment();

        } else if (id == R.id.nav_customization) {
            currentFragment=new CustomizationFragment();

        } else if (id == R.id.nav_settings) {
            currentFragment=new SettingsFragment();

        }
        if(currentFragment != null){
            fm.beginTransaction()
                    .replace(R.id.contentLayout, currentFragment)
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
        checkPermissions();

        // Observe disconnections
        registerGattReceiver();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

        popFragmentsIfNoPeripheralsConnected();         // check if peripherals were disconnected while the app was in background
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterGattReceiver();

        // Remove location dialog if present
        if (mRequestLocationDialog != null) {
            mRequestLocationDialog.cancel();
            mRequestLocationDialog = null;
        }
    }


    private void checkPermissions() {

        final boolean areLocationServicesReadyForScanning = manageLocationServiceAvailabilityForScanning();
        if (!areLocationServicesReadyForScanning) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            mRequestLocationDialog = builder.setMessage(R.string.bluetooth_locationpermission_disabled_text)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            //DialogUtils.keepDialogOnOrientationChanges(mRequestLocationDialog);
        } else {
            if (mRequestLocationDialog != null) {
                mRequestLocationDialog.cancel();
                mRequestLocationDialog = null;
            }

            // Bluetooth state
            if (!hasUserAlreadyBeenAskedAboutBluetoothStatus) {     // Don't repeat the check if the user was already informed to avoid showing the "Enable Bluetooth" system prompt several times
                final boolean isBluetoothEnabled = manageBluetoothAvailability();

                if (isBluetoothEnabled) {
                    // Request Bluetooth scanning permissions
                    final boolean isLocationPermissionGranted = requestCoarseLocationPermissionIfNeeded();

                    if (isLocationPermissionGranted) {
                        // All good. Start Scanning
                        BleManager.getInstance().start(MainActivity.this);
                        // Bluetooth was enabled, resume scanning
                        mMainFragment.startScanning();
                    }
                }
            }
        }
    }

    // region Permissions
    private boolean manageLocationServiceAvailabilityForScanning() {

        boolean areLocationServiceReady = true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {        // Location services are only needed to be enabled from Android 6.0
            int locationMode = Settings.Secure.LOCATION_MODE_OFF;
            try {
                locationMode = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);

            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
            }
            areLocationServiceReady = locationMode != Settings.Secure.LOCATION_MODE_OFF;
        }

        return areLocationServiceReady;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private boolean requestCoarseLocationPermissionIfNeeded() {
        boolean permissionGranted = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android Marshmallow Permission check 
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionGranted = false;
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                mRequestLocationDialog = builder.setTitle(R.string.bluetooth_locationpermission_title)
                        .setMessage(R.string.bluetooth_locationpermission_text)
                        .setPositiveButton(android.R.string.ok, null)
                        .setOnDismissListener(dialog -> requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION))
                        .show();
            }
        }
        return permissionGranted;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults.length >= 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Location permission granted");
                    checkPermissions();

                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle(R.string.bluetooth_locationpermission_notavailable_title);
                    builder.setMessage(R.string.bluetooth_locationpermission_notavailable_text);
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(dialog -> {
                    });
                    builder.show();
                }
                break;
            }
            default:
                break;
        }
    }
    // endregion

    // region Bluetooth Setup
    private boolean manageBluetoothAvailability() {
        boolean isEnabled = true;

        // Check Bluetooth HW status
        int errorMessageId = 0;
        final int bleStatus = BleUtils.getBleStatus(getBaseContext());
        switch (bleStatus) {
            case BleUtils.STATUS_BLE_NOT_AVAILABLE:
                errorMessageId = R.string.bluetooth_unsupported;
                isEnabled = false;
                break;
            case BleUtils.STATUS_BLUETOOTH_NOT_AVAILABLE: {
                errorMessageId = R.string.bluetooth_poweredoff;
                isEnabled = false;      // it was already off
                break;
            }
            case BleUtils.STATUS_BLUETOOTH_DISABLED: {
                isEnabled = false;      // it was already off
                // if no enabled, launch settings dialog to enable it (user should always be prompted before automatically enabling bluetooth)
                Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetoothIntent, kActivityRequestCode_EnableBluetooth);
                // execution will continue at onActivityResult()
                break;
            }
        }

        if (errorMessageId != 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            AlertDialog dialog = builder.setMessage(errorMessageId)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            DialogUtils.keepDialogOnOrientationChanges(dialog);
        }

        return isEnabled;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == kActivityRequestCode_EnableBluetooth) {
            if (resultCode == Activity.RESULT_OK) {
                checkPermissions();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                if (!isFinishing()) {
                    hasUserAlreadyBeenAskedAboutBluetoothStatus = true;     // Remember that
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    AlertDialog dialog = builder.setMessage(R.string.bluetooth_poweredoff)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                    DialogUtils.keepDialogOnOrientationChanges(dialog);
                }
            }
        }
    }

    private void popFragmentsIfNoPeripheralsConnected() {
        final int numConnectedPeripherals = BleManager.getInstance().getConnectedDevices().size();
        final boolean isLastConnectedPeripheral = numConnectedPeripherals == 0;
        if (isLastConnectedPeripheral) {
            Log.d(TAG, "No peripherals connected. Pop all fragments");
            FragmentManager fragmentManager = getSupportFragmentManager();
            if (fragmentManager != null) {
                fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                fragmentManager.executePendingTransactions();
            }
        }
    }
    // endregion
//
    // region ScannerFragmentListener
    public void bluetoothAdapterIsDisabled() {
        checkPermissions();
    }

    public void scannerRequestLocationPermissionIfNeeded() {
        requestCoarseLocationPermissionIfNeeded();
    }

    //automatically goes into uart
    public void startPeripheralModules(String peripheralIdentifier) {
        //todo make fragement to which you want app to go
        HomepageFragment fragment =  HomepageFragment.newInstance(peripheralIdentifier);
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager != null) {
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_right, R.anim.slide_out_left)
                    .replace(R.id.contentLayout, fragment, "Modules");
            fragmentTransaction.addToBackStack(null);
            fragmentTransaction.commit();
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
                popFragmentsIfNoPeripheralsConnected();
            }
        }
    };
}
