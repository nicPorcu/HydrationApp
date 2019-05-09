package com.example.per6.hydrationapp;


import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanCallback;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;


import com.example.per6.hydrationapp.ble.BleManager;
import com.example.per6.hydrationapp.ble.BlePeripheral;
import com.example.per6.hydrationapp.ble.BleUtils;
import com.example.per6.hydrationapp.style.StyledSnackBar;
import com.example.per6.hydrationapp.utils.DialogUtils;

import java.util.List;
import java.util.Locale;

import no.nordicsemi.android.support.v18.scanner.ScanRecord;

import static android.content.Context.CLIPBOARD_SERVICE;
import static android.content.Context.MODE_PRIVATE;

public class BluetoothActivity extends AppCompatActivity implements ScannerStatusFragmentDialog.onScannerStatusCancelListener {
    private static final String TAG = "BluetoothActivity";
    // Constants


    private boolean hasUserAlreadyBeenAskedAboutBluetoothStatus = false;


    private final static String kPreferences = "Scanner";
    private final static String kPreferences_filtersPanelOpen = "filtersPanelOpen";

    private final static int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int kActivityRequestCode_EnableBluetooth = 1;


    // Data -  Scanned Devices
    private Context context;
    private ScannerViewModel mScannerViewModel;
    private BlePeripheralsAdapter mBlePeripheralsAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private Button skip;
    private FragmentManager fm;
    private AlertDialog mRequestLocationDialog;

    // Data - Dialogs
    private ScannerStatusFragmentDialog mConnectingDialog;

    // region Fragment lifecycle


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        context = this;


        fm = getSupportFragmentManager();
        onActivityCreatedStuff();
        onViewCreatedStuff();


        //setHasOptionsMenu(true);

        // Retain this fragment across configuration changes
        //setRetainInstance(true);
    }

    private void onActivityCreatedStuff() {
        mScannerViewModel = ViewModelProviders.of(this).get(ScannerViewModel.class);

        // Scan results
        mScannerViewModel.getFilteredBlePeripherals().observe(this, new Observer<List<BlePeripheral>>() {
            @Override
            public void onChanged(@Nullable List<BlePeripheral> blePeripherals) {
                mBlePeripheralsAdapter.setBlePeripherals(blePeripherals);
            }
        });

        // Scanning
        mScannerViewModel.getScanningErrorCode().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer errorCode) {
                Log.d(TAG, "Scanning error: " + errorCode);

                if (errorCode != null && errorCode == ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED) {       // Check for known errors
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    AlertDialog dialog = builder.setTitle(R.string.dialog_error).setMessage(R.string.bluetooth_scanner_errorregisteringapp)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                    DialogUtils.keepDialogOnOrientationChanges(dialog);
                } else {        // Ask for location permission
                    requestCoarseLocationPermissionIfNeeded();
                }
            }
        });

        mScannerViewModel.getBlePeripheralsConnectionChanged().observe(this, new Observer<BlePeripheral>() {
            @Override
            public void onChanged(@Nullable BlePeripheral blePeripheral) {
                mBlePeripheralsAdapter.notifyDataSetChanged();
                if (blePeripheral != null) {
                    showConnectionStateDialog(blePeripheral);
                }
            }
        });
        mScannerViewModel.getBlePeripheralDiscoveredServices().observe(this, this::showServiceDiscoveredStateDialog);

        mScannerViewModel.getConnectionErrorMessage().observe(this, this::showConnectionStateError);
    }


    private void onViewCreatedStuff() {
        if (context != null) {
            // Peripherals recycler view
            RecyclerView peripheralsRecyclerView = findViewById(R.id.peripheralsRecyclerView);
            DividerItemDecoration itemDecoration = new DividerItemDecoration(context, DividerItemDecoration.VERTICAL);
            Drawable lineSeparatorDrawable = ContextCompat.getDrawable(context, R.drawable.simpledivideritemdecoration);
            assert lineSeparatorDrawable != null;
            itemDecoration.setDrawable(lineSeparatorDrawable);
            peripheralsRecyclerView.addItemDecoration(itemDecoration);

            RecyclerView.LayoutManager peripheralsLayoutManager = new LinearLayoutManager(context);
            peripheralsRecyclerView.setLayoutManager(peripheralsLayoutManager);

            // Disable update animation
            ((SimpleItemAnimator) peripheralsRecyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

            // Adapter
            mBlePeripheralsAdapter = new BlePeripheralsAdapter(context, new BlePeripheralsAdapter.Listener() {
                @Override
                public void onAdvertisementData(@NonNull BlePeripheral blePeripheral) {
                    ScanRecord scanRecord = blePeripheral.getScanRecord();
                    if (scanRecord != null) {
                        final byte[] advertisementBytes = scanRecord.getBytes();
                        final String packetText = BleUtils.bytesToHexWithSpaces(advertisementBytes);
                        final String clipboardLabel = context.getString(R.string.scanresult_advertisement_rawdata_title);

                        new AlertDialog.Builder(context)
                                .setTitle(R.string.scanresult_advertisement_rawdata_title)
                                .setMessage(packetText)
                                .setPositiveButton(android.R.string.ok, null)
                                .setNeutralButton(android.R.string.copy, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
                                        if (clipboard != null) {
                                            ClipData clip = ClipData.newPlainText(clipboardLabel, packetText);
                                            clipboard.setPrimaryClip(clip);
                                        }
                                    }
                                })
                                .show();
                    }
                }
            });
            peripheralsRecyclerView.setAdapter(mBlePeripheralsAdapter);

            // Swipe to refreshAll
            mSwipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
            mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    if (BleManager.getInstance().isAdapterEnabled()) {
                        mScannerViewModel.refresh();
                    } else {
                        checkPermissions();
                    }
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    }, 500);
                }
            });

            skip = findViewById(R.id.skipButton);
            skip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "onClick: click");

                    AlertDialog.Builder builder=new AlertDialog.Builder(BluetoothActivity.this);
                    builder.setMessage("Are You Sure?");
                    builder.setPositiveButton(R.string.yes, (dialog, which) -> {
                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("peripheralIdentifier", "");
                        setResult(Activity.RESULT_OK, returnIntent);
                        finish();
                    });
                    builder.setNegativeButton(R.string.no, (dialog, which) -> {
                    });
                    builder.create().show();




                }
            });
        }
    }



    @Override
    public void onResume() {
        super.onResume();
        checkPermissions();

        FragmentActivity activity = this;
        if (activity != null) {
            activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
            startScanning();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        mScannerViewModel.stop();
        if (mRequestLocationDialog != null) {
            mRequestLocationDialog.cancel();
            mRequestLocationDialog = null;
        }
    }

    @Override
    public void onDestroy() {
        mScannerViewModel.saveFilters();
        super.onDestroy();
    }

    // endregion

    // region Actions
    public void startScanning() {
        mScannerViewModel.start();
    }

    public void disconnectAllPeripherals() {
        mScannerViewModel.disconnectAllPeripherals();
    }

    // endregion

    // region Dialogs

    private void showConnectionStateDialog(BlePeripheral blePeripheral) {
        final int connectionState = blePeripheral.getConnectionState();

        switch (connectionState) {
            case BlePeripheral.STATE_DISCONNECTED:
                removeConnectionStateDialog();
                break;
            case BlePeripheral.STATE_CONNECTING:
                showConnectionStateDialog(R.string.peripheraldetails_connecting, blePeripheral);
                break;
            case BlePeripheral.STATE_CONNECTED:
                showConnectionStateDialog(R.string.peripheraldetails_discoveringservices, blePeripheral);
                break;
        }
    }

    private void removeConnectionStateDialog() {
        if (mConnectingDialog != null) {
            mConnectingDialog.dismiss();
//            mConnectingDialog.cancel();

            mConnectingDialog = null;
        }
    }

    @Override
    public void scannerStatusCancelled(@NonNull String blePeripheralIdentifier) {
        Log.d(TAG, "Connecting dialog cancelled");

        final BlePeripheral blePeripheral = mScannerViewModel.getPeripheralWithIdentifier(blePeripheralIdentifier);
        if (blePeripheral != null) {
            blePeripheral.disconnect();
        } else {
            Log.w(TAG, "status dialog cancelled for unknown peripheral");
        }
    }

    private void showConnectionStateDialog(@StringRes int messageId,
                                           final BlePeripheral blePeripheral) {
        // Show dialog
        final String message = getString(messageId);
        if (mConnectingDialog == null || !mConnectingDialog.isInitialized()) {
            removeConnectionStateDialog();
            mConnectingDialog = ScannerStatusFragmentDialog.newInstance(message, blePeripheral.getIdentifier());
            mConnectingDialog.show(fm, "ConnectingDialog");

        } else {
            mConnectingDialog.setMessage(message);
        }
    }


    private void showServiceDiscoveredStateDialog(BlePeripheral blePeripheral) {


        if (blePeripheral != null && context != null) {

            if (blePeripheral.isDisconnected()) {
                Log.d(TAG, "Abort connection sequence. Peripheral disconnected");
            } else {
                startPeripheralModules(blePeripheral.getIdentifier());
            }
        }
    }

    private void showConnectionStateError(String message) {
        removeConnectionStateDialog();


        View view = findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
        StyledSnackBar.styleSnackBar(snackbar, context);
        snackbar.show();

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
                        BleManager.getInstance().start(BluetoothActivity.this);
                        // Bluetooth was enabled, resume scanning
                        //mMainFragment.startScanning();
                    }
                }
            }
        }
    }

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
                //checkPermissions();
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

    public void startPeripheralModules(String peripheralIdentifier) {
        //todo make fragement to which you want app to go

        Intent returnIntent = new Intent();
        returnIntent.putExtra("peripheralIdentifier", peripheralIdentifier);
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }

}

