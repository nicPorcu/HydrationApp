package com.example.per6.hydrationapp;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.bluetooth.le.ScanCallback;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
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
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

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

public class ScannerFragment extends Fragment implements ScannerStatusFragmentDialog.onScannerStatusCancelListener {
    // Constants
    private final static String TAG = ScannerFragment.class.getSimpleName();

    private final static String kPreferences = "Scanner";
    private final static String kPreferences_filtersPanelOpen = "filtersPanelOpen";

    // Data -  Scanned Devices
    private ScannerFragmentListener mListener;
    private ScannerViewModel mScannerViewModel;
    private BlePeripheralsAdapter mBlePeripheralsAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private Button skip;

    // Data - Dialogs
    private ScannerStatusFragmentDialog mConnectingDialog;

    // region Fragment lifecycle
    public static ScannerFragment newInstance() {
        return new ScannerFragment();
    }

    public ScannerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mListener = (ScannerFragmentListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement ScannerFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

        // Retain this fragment across configuration changes
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_scanner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Context context = getContext();

        if (context != null) {
            // Peripherals recycler view
            RecyclerView peripheralsRecyclerView = view.findViewById(R.id.peripheralsRecyclerView);
            DividerItemDecoration itemDecoration = new DividerItemDecoration(context, DividerItemDecoration.VERTICAL);
            Drawable lineSeparatorDrawable = ContextCompat.getDrawable(context, R.drawable.simpledivideritemdecoration);
            assert lineSeparatorDrawable != null;
            itemDecoration.setDrawable(lineSeparatorDrawable);
            peripheralsRecyclerView.addItemDecoration(itemDecoration);

            RecyclerView.LayoutManager peripheralsLayoutManager = new LinearLayoutManager(getContext());
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
            mSwipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
            mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    if (BleManager.getInstance().isAdapterEnabled()) {
                        mScannerViewModel.refresh();
                    } else {
                        mListener.bluetoothAdapterIsDisabled();
                    }
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mSwipeRefreshLayout.setRefreshing(false);
                        }
                    }, 500);
                }
            });

            skip = view.findViewById(R.id.skipButton);
            FragmentManager fragmentManager = getFragmentManager();
            skip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //todo add alert dialog are you sure?
                    HomepageFragment fragment =  new HomepageFragment();
                    if (fragmentManager != null) {
                        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction()
                                .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_right, R.anim.slide_out_left)
                                .replace(R.id.contentLayout, fragment, "Modules");
                        fragmentTransaction.addToBackStack(null);
                        fragmentTransaction.commit();
                    }
                }
            });
        }


    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // ViewModel
        FragmentActivity activity = getActivity();
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
                    AlertDialog.Builder builder = new AlertDialog.Builder(ScannerFragment.this.getContext());
                    AlertDialog dialog = builder.setTitle(R.string.dialog_error).setMessage(R.string.bluetooth_scanner_errorregisteringapp)
                            .setPositiveButton(android.R.string.ok, null)
                            .show();
                    DialogUtils.keepDialogOnOrientationChanges(dialog);
                } else {        // Ask for location permission
                    mListener.scannerRequestLocationPermissionIfNeeded();
                }
            }
        });

        mScannerViewModel.getBlePeripheralsConnectionChanged().observe(this, new Observer<BlePeripheral>() {
            @Override
            public void onChanged(@Nullable BlePeripheral blePeripheral) {
                mBlePeripheralsAdapter.notifyDataSetChanged();
                if (blePeripheral != null) {
                    ScannerFragment.this.showConnectionStateDialog(blePeripheral);
                }
            }
        });
        mScannerViewModel.getBlePeripheralDiscoveredServices().observe(this, this::showServiceDiscoveredStateDialog);

        mScannerViewModel.getConnectionErrorMessage().observe(this, this::showConnectionStateError);
    }

    @Override
    public void onResume() {
        super.onResume();

        FragmentActivity activity = getActivity();
        if (activity != null) {
            activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
            startScanning();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        mScannerViewModel.stop();
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

    private void showConnectionStateDialog(@StringRes int messageId, final BlePeripheral blePeripheral) {
        // Show dialog
        final String message = getString(messageId);
        if (mConnectingDialog == null || !mConnectingDialog.isInitialized()) {
            removeConnectionStateDialog();

            FragmentActivity activity = getActivity();
            if (activity != null) {
                FragmentManager fragmentManager = getFragmentManager();
                if (fragmentManager != null) {
                    mConnectingDialog = ScannerStatusFragmentDialog.newInstance(message, blePeripheral.getIdentifier());
                    mConnectingDialog.setTargetFragment(this, 0);
                    mConnectingDialog.show(fragmentManager, "ConnectingDialog");
                }
            }
        } else {
            mConnectingDialog.setMessage(message);
        }
    }

    private void showServiceDiscoveredStateDialog(BlePeripheral blePeripheral) {
        Context context = getContext();

        if (blePeripheral != null && context != null) {

            if (blePeripheral.isDisconnected()) {
                Log.d(TAG, "Abort connection sequence. Peripheral disconnected");
            } else {
                mListener.startPeripheralModules(blePeripheral.getIdentifier());
            }
        }
    }

    private void showConnectionStateError(String message) {
        removeConnectionStateDialog();

        FragmentActivity activity = getActivity();
        if (activity != null) {
            View view = activity.findViewById(android.R.id.content);
            Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
            StyledSnackBar.styleSnackBar(snackbar, activity);
            snackbar.show();
        }
    }

    // region Listeners
    interface ScannerFragmentListener {
        void bluetoothAdapterIsDisabled();

        void scannerRequestLocationPermissionIfNeeded();

        void startPeripheralModules(String singlePeripheralIdentifier);
    }

    // endregion
}
