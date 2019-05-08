package com.example.per6.hydrationapp;
import android.app.Application;
import android.arch.core.util.Function;
import android.arch.lifecycle.AndroidViewModel;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MediatorLiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.Transformations;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.example.per6.hydrationapp.ble.BlePeripheral;
import com.example.per6.hydrationapp.ble.BleScanner;
import com.example.per6.hydrationapp.utils.LocalizationManager;
import com.example.per6.hydrationapp.utils.SingleLiveEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;

public class ScannerViewModel extends AndroidViewModel implements BleScanner.BleScannerListener {
    // region Constants
    private final static String TAG = ScannerViewModel.class.getSimpleName();
    // endregion

    // region Data - Scanning
    private BleScanner mScanner = BleScanner.getInstance();
    private final MutableLiveData<Boolean> mIsScanning = new MutableLiveData<>();
    private final MutableLiveData<List<BlePeripheral>> mBlePeripherals = new MutableLiveData<>();
    private final SingleLiveEvent<Integer> mScanningErrorCode = new SingleLiveEvent<>();
    // endregion

    // region Data - Filters
    private final MutableLiveData<String> mFilterName = new MutableLiveData<>();
    private final MutableLiveData<Integer> mRssiFilterValue = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mIsUnnamedEnabled = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mIsOnlyUartEnabled = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mIsFilterNameExact = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mIsFilterNameCaseInsensitive = new MutableLiveData<>();
    private final MediatorLiveData<FilterData> mFiltersLiveDataMerger = new MediatorLiveData<>();
    private final MediatorLiveData<ScanData> mScanFilterLiveDataMerger = new MediatorLiveData<>();

    private final MutableLiveData<Integer> mNumPeripheralsFilteredOut = new MutableLiveData<>();
    private final MutableLiveData<Integer> mNumPeripheralsFiltered = new MutableLiveData<>();

    private LiveData<String> mRssiFilterDescription = Transformations.map(mRssiFilterValue, new Function<Integer, String>() {
        @Override
        public String apply(Integer rssi) {
            return String.format(Locale.ENGLISH, ScannerViewModel.this.getApplication().getString(R.string.scanner_filter_rssivalue_format), rssi);
        }
    });

    //todo determine how to fix

    private LiveData<Boolean> mIsAnyFilterEnabled = Transformations.map(mFiltersLiveDataMerger, FilterData::isAnyFilterEnabled);

    private LiveData<String> mFiltersDescription = Transformations.map(mFiltersLiveDataMerger, new Function<FilterData, String>() {
        @Override
        public String apply(FilterData input) {
            String filtersDescription = input.getDescription();
            return filtersDescription != null ? String.format(Locale.ENGLISH, ScannerViewModel.this.getApplication().getString(R.string.scanner_filter_currentfilter_format), filtersDescription) : ScannerViewModel.this.getApplication().getString(R.string.scanner_filter_nofilter);
        }
    });


    private LiveData<List<BlePeripheral>> mFilteredBlePeripherals = Transformations.switchMap(mScanFilterLiveDataMerger, new Function<ScanData, LiveData<List<BlePeripheral>>>() {
        @Override
        public LiveData<List<BlePeripheral>> apply(ScanData input) {
            FilterData filterData = input.filterData;
            if (filterData == null) return null;     // Filter Data not initialized yet

            // Copy all existing results
            List<BlePeripheral> results = new ArrayList<>(input.blePeripherals);

            // Sort devices alphabetically
            Collections.sort(results, new Comparator<BlePeripheral>() {
                @Override
                public int compare(BlePeripheral o1, BlePeripheral o2) {
                    return ScannerViewModel.this.getResultNameForOrdering(o1).compareToIgnoreCase(ScannerViewModel.this.getResultNameForOrdering(o2));
                }
            });

            // Apply filters
            if (filterData.isOnlyUartEnabled) {
                for (Iterator<BlePeripheral> it = results.iterator(); it.hasNext(); ) {
                    if (BleScanner.getDeviceType(it.next()) != BleScanner.kDeviceType_Uart) {
                        it.remove();
                    }
                }
            }

            if (!filterData.isUnnamedEnabled) {
                for (Iterator<BlePeripheral> it = results.iterator(); it.hasNext(); ) {
                    if (it.next().getDevice().getName() == null) {
                        it.remove();
                    }
                }
            }

            if (filterData.name != null && !filterData.name.isEmpty()) {
                for (Iterator<BlePeripheral> it = results.iterator(); it.hasNext(); ) {
                    String name = it.next().getDevice().getName();
                    boolean testPassed = false;
                    if (name != null) {
                        if (filterData.isNameMatchExact) {
                            if (filterData.isNameMatchCaseInSensitive) {
                                testPassed = name.compareToIgnoreCase(filterData.name) == 0;
                            } else {
                                testPassed = name.compareTo(filterData.name) == 0;
                            }
                        } else {
                            if (filterData.isNameMatchCaseInSensitive) {
                                testPassed = name.toLowerCase().contains(filterData.name.toLowerCase());
                            } else {
                                testPassed = name.contains(filterData.name);
                            }
                        }
                    }
                    if (!testPassed) {
                        it.remove();
                    }
                }
            }

            for (Iterator<BlePeripheral> it = results.iterator(); it.hasNext(); ) {
                if (it.next().getRssi() < filterData.rssi) {
                    it.remove();
                }
            }

            // Update related variables
            mNumPeripheralsFiltered.setValue(results.size());
            final int numPeripheralsFilteredOut = input.blePeripherals.size() - results.size();
            mNumPeripheralsFilteredOut.setValue(numPeripheralsFilteredOut);

            // Create result
            MutableLiveData<List<BlePeripheral>> liveResults = new MutableLiveData<>();
            liveResults.setValue(results);

            return liveResults;
        }
    });


    // endregion

    // region Data - Connection
    private final SingleLiveEvent<BlePeripheral> mBlePeripheralsConnectionChanged = new SingleLiveEvent<>();
    private final SingleLiveEvent<String> mBlePeripheralsConnectionErrorMessage = new SingleLiveEvent<>();
    private final SingleLiveEvent<BlePeripheral> mBlePeripheralDiscoveredServices = new SingleLiveEvent<>();
    private final MutableLiveData<Boolean> mIsMultiConnectEnabled = new MutableLiveData<>();
    private final MutableLiveData<Integer> mNumDevicesConnected = new MutableLiveData<>();
    // endregion

    // region Setup
    public ScannerViewModel(Application application) {
        super(application);

        // Add broadcast receiver
        registerGattReceiver();

        // Setup scanning
        mIsScanning.setValue(false);
        mScanner.setListener(this);

        // Setup mFiltersLiveDataMerger
        setDefaultFilters(true);
        mFiltersLiveDataMerger.addSource(mFilterName, new Observer<String>() {
            @Override
            public void onChanged(@Nullable String name) {
                FilterData filter = mFiltersLiveDataMerger.getValue();
                if (filter != null) {
                    filter.name = name;
                    mFiltersLiveDataMerger.setValue(filter);
                }
            }
        });

        mFiltersLiveDataMerger.addSource(mRssiFilterValue, new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer rssiValue) {
                FilterData filter = mFiltersLiveDataMerger.getValue();
                if (filter != null && rssiValue != null) {
                    filter.rssi = rssiValue;
                    mFiltersLiveDataMerger.setValue(filter);
                }
            }
        });

        mFiltersLiveDataMerger.addSource(mIsUnnamedEnabled, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean enabled) {
                FilterData filter = mFiltersLiveDataMerger.getValue();
                if (filter != null && enabled != null) {
                    filter.isUnnamedEnabled = enabled;
                    mFiltersLiveDataMerger.setValue(filter);
                }
            }
        });

        mFiltersLiveDataMerger.addSource(mIsOnlyUartEnabled, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean enabled) {
                FilterData filter = mFiltersLiveDataMerger.getValue();
                if (filter != null && enabled != null) {
                    filter.isOnlyUartEnabled = enabled;
                    mFiltersLiveDataMerger.setValue(filter);
                }
            }
        });

        mFiltersLiveDataMerger.addSource(mIsFilterNameExact, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean enabled) {
                FilterData filter = mFiltersLiveDataMerger.getValue();
                if (filter != null && enabled != null) {
                    filter.isNameMatchExact = enabled;
                    mFiltersLiveDataMerger.setValue(filter);
                }
            }
        });

        mFiltersLiveDataMerger.addSource(mIsFilterNameCaseInsensitive, new Observer<Boolean>() {
            @Override
            public void onChanged(@Nullable Boolean enabled) {
                FilterData filter = mFiltersLiveDataMerger.getValue();
                if (filter != null && enabled != null) {
                    filter.isNameMatchCaseInSensitive = enabled;
                    mFiltersLiveDataMerger.setValue(filter);
                }
            }
        });

        // Setup mScanFilterLiveDataMerger
        ScanData scanData = new ScanData();
        mScanFilterLiveDataMerger.setValue(scanData);
        mScanFilterLiveDataMerger.addSource(mBlePeripherals, new Observer<List<BlePeripheral>>() {
            @Override
            public void onChanged(@Nullable List<BlePeripheral> blePeripherals) {
                ScanData data = mScanFilterLiveDataMerger.getValue();
                if (data != null) {
                    data.blePeripherals = blePeripherals;
                    mScanFilterLiveDataMerger.setValue(data);
                }
            }
        });
        mScanFilterLiveDataMerger.addSource(mFiltersLiveDataMerger, new Observer<FilterData>() {
            @Override
            public void onChanged(@Nullable FilterData filterData) {
                ScanData data = mScanFilterLiveDataMerger.getValue();
                if (filterData != null && data != null) {
                    data.filterData = filterData;
                    mScanFilterLiveDataMerger.setValue(data);
                }
            }
        });

        // Setup Connection
        mBlePeripheralDiscoveredServices.setValue(null);
        mIsMultiConnectEnabled.setValue(false);
        mNumDevicesConnected.setValue(0);
        mNumPeripheralsFilteredOut.setValue(0);
        mNumPeripheralsFiltered.setValue(0);
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        // Stop and remove listener
        stop();
        if (mScanner.getListener() == this) {       // Replace only if is still mself
            mScanner.setListener(null);
        }
        saveFilters();      // optional: save filters (useful while debugging because onDestroy is not called and filters are not saved)

        // Unregister receiver
        unregisterGattReceiver();

        mScanner = null;
    }

    // endregion

    //  region Getters / Setters
    public LiveData<Boolean> isScanning() {
        return mIsScanning;
    }

    public LiveData<List<BlePeripheral>> getFilteredBlePeripherals() {
        return mFilteredBlePeripherals;
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    public @Nullable
    BlePeripheral getPeripheralAtFilteredPosition(int position) {
        List<BlePeripheral> blePeripherals = mFilteredBlePeripherals.getValue();
        if (blePeripherals != null && position >= 0 && position < blePeripherals.size()) {
            final BlePeripheral blePeripheral = blePeripherals.get(position);
            return blePeripheral;
        } else {
            return null;
        }
    }

    public @Nullable
    BlePeripheral getPeripheralWithIdentifier(@NonNull String identifier) {
        BlePeripheral result = null;
        int i = 0;
        List<BlePeripheral> blePeripherals = mBlePeripherals.getValue();
        if (blePeripherals != null) {
            while (i < blePeripherals.size() && result == null) {
                BlePeripheral blePeripheral = blePeripherals.get(i);
                if (identifier.equals(blePeripheral.getIdentifier())) {
                    result = blePeripheral;
                } else {
                    i++;
                }
            }
        }

        return result;
    }

    public LiveData<Integer> getScanningErrorCode() {
        return mScanningErrorCode;
    }

    public void setDefaultFilters(boolean shouldRestoreSavedValues) {
        FilterData filterData = new FilterData(shouldRestoreSavedValues);

        mFilterName.setValue(filterData.name);
        mRssiFilterValue.setValue(filterData.rssi);
        mIsOnlyUartEnabled.setValue(true);
        mIsUnnamedEnabled.setValue(filterData.isUnnamedEnabled);
        mIsFilterNameExact.setValue(filterData.isNameMatchExact);
        mIsFilterNameCaseInsensitive.setValue(filterData.isNameMatchCaseInSensitive);

        mFiltersLiveDataMerger.setValue(filterData);
    }

    public SingleLiveEvent<BlePeripheral> getBlePeripheralsConnectionChanged() {
        return mBlePeripheralsConnectionChanged;
    }

    public SingleLiveEvent<String> getConnectionErrorMessage() {
        return mBlePeripheralsConnectionErrorMessage;
    }

    public LiveData<BlePeripheral> getBlePeripheralDiscoveredServices() {
        return mBlePeripheralDiscoveredServices;
    }

    // endregion

    // region Actions
    public void refresh() {
        mScanner.refresh();
    }

    public void start() {
        if (!mScanner.isScanning()) {
            Log.d(TAG, "start scanning");
            mScanner.start();
        } else {
            Log.d(TAG, "start scanning: already was scanning");
        }
    }

    public void stop() {
        if (mScanner.isScanning()) {
            Log.d(TAG, "stop scanning");
            mScanner.stop();
        }
    }

    public void disconnectAllPeripherals() {
        if (mScanner != null) {
            mScanner.disconnectFromAll();
        }
    }

    public void saveFilters() {
        ScanData data = mScanFilterLiveDataMerger.getValue();
        if (data != null) {
            data.saveFilters();
        }
    }

    // endregion

    // region BleScannerListener
    @Override
    public void onScanPeripheralsUpdated(List<BlePeripheral> blePeripherals) {
        mBlePeripherals.setValue(blePeripherals);
    }

    @Override
    public void onScanPeripheralsFailed(int errorCode) {
        mScanningErrorCode.setValue(errorCode);
    }

    @Override
    public void onScanStatusChanged(boolean isScanning) {
        mIsScanning.setValue(false);
    }

    // endregion

    // region Utils
    private @NonNull
    String getResultNameForOrdering(BlePeripheral result) {
        BluetoothDevice device = result.getDevice();
        String name = device.getName();
        if (name == null) {
            name = "~" + device.getAddress();     // Prefix with symbol so all the unknowns are pushed to the bottom
        }
        return name;
    }

    // endregion


    // region Data Classes
    private class FilterData {
        // Constants
        final static int kMaxRssiValue = -100;

        private final static String kPreferences = "PeripheralList_prefs";
        private final static String kPreferences_filtersName = "filtersName";
        private final static String kPreferences_filtersIsNameExact = "filtersIsNameExact";
        private final static String kPreferences_filtersIsNameCaseInsensitive = "filtersIsNameCaseInsensitive";
        private final static String kPreferences_filtersRssi = "filtersRssi";
        private final static String kPreferences_filtersUnnamedEnabled = "filtersUnnamedEnabled";
        private final static String kPreferences_filtersUartEnabled = "filtersUartEnabled";

        // Data
        String name;
        int rssi = kMaxRssiValue;
        boolean isOnlyUartEnabled = false;
        boolean isUnnamedEnabled = true;
        boolean isNameMatchExact = false;
        boolean isNameMatchCaseInSensitive = true;

        //
        FilterData(boolean shouldRestoreSavedValues) {
            if (shouldRestoreSavedValues) {
                load();
            }
        }

        private void load() {
            Log.d(TAG, "FilterData load");

            SharedPreferences preferences = getApplication().getSharedPreferences(kPreferences, MODE_PRIVATE);
            name = preferences.getString(kPreferences_filtersName, null);
            isNameMatchExact = preferences.getBoolean(kPreferences_filtersIsNameExact, false);
            isNameMatchCaseInSensitive = preferences.getBoolean(kPreferences_filtersIsNameCaseInsensitive, true);
            rssi = preferences.getInt(kPreferences_filtersRssi, kMaxRssiValue);
            isUnnamedEnabled = preferences.getBoolean(kPreferences_filtersUnnamedEnabled, true);
            isOnlyUartEnabled = preferences.getBoolean(kPreferences_filtersUartEnabled, false);
        }

        public void save() {
            Log.d(TAG, "FilterData save");
            SharedPreferences.Editor preferencesEditor = getApplication().getSharedPreferences(kPreferences, MODE_PRIVATE).edit();
            preferencesEditor.putString(kPreferences_filtersName, name);
            preferencesEditor.putBoolean(kPreferences_filtersIsNameExact, isNameMatchExact);
            preferencesEditor.putBoolean(kPreferences_filtersIsNameCaseInsensitive, isNameMatchCaseInSensitive);
            preferencesEditor.putInt(kPreferences_filtersRssi, rssi);
            preferencesEditor.putBoolean(kPreferences_filtersUnnamedEnabled, isUnnamedEnabled);
            preferencesEditor.putBoolean(kPreferences_filtersUartEnabled, isOnlyUartEnabled);
            preferencesEditor.apply();
        }

        boolean isAnyFilterEnabled() {
            return (name != null && !name.isEmpty()) || rssi > kMaxRssiValue || isOnlyUartEnabled || !isUnnamedEnabled;
        }

        String getDescription() {
            String filtersTitle = null;

            if (name != null && !name.isEmpty()) {
                filtersTitle = name;
            }

            if (rssi > FilterData.kMaxRssiValue) {
                String rssiString = String.format(Locale.ENGLISH, getApplication().getString(R.string.scanner_filter_rssi_description_format), rssi);
                if (filtersTitle != null && !filtersTitle.isEmpty()) {
                    filtersTitle = filtersTitle + ", " + rssiString;
                } else {
                    filtersTitle = rssiString;
                }
            }

            if (!isUnnamedEnabled) {
                String namedString = getApplication().getString(R.string.scanner_filter_unnamed_description);
                if (filtersTitle != null && !filtersTitle.isEmpty()) {
                    filtersTitle = filtersTitle + ", " + namedString;
                } else {
                    filtersTitle = namedString;
                }
            }

            if (isOnlyUartEnabled) {
                String uartString = getApplication().getString(R.string.scanner_filter_uart_description);
                if (filtersTitle != null && !filtersTitle.isEmpty()) {
                    filtersTitle = filtersTitle + ", " + uartString;
                } else {
                    filtersTitle = uartString;
                }
            }

            return filtersTitle;
        }
    }

    private class ScanData {
        // Data
        FilterData filterData;
        List<BlePeripheral> blePeripherals;

        ScanData() {
            blePeripherals = new ArrayList<>();
        }

        void saveFilters() {
            if (filterData != null) {
                filterData.save();
            }
        }
    }
    // endregion

    // region Broadcast Listener
    private void registerGattReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BlePeripheral.kBlePeripheral_OnConnecting);
        filter.addAction(BlePeripheral.kBlePeripheral_OnConnected);
        filter.addAction(BlePeripheral.kBlePeripheral_OnDisconnected);
        LocalBroadcastManager.getInstance(getApplication()).registerReceiver(mGattUpdateReceiver, filter);
    }

    private void unregisterGattReceiver() {
        LocalBroadcastManager.getInstance(getApplication()).unregisterReceiver(mGattUpdateReceiver);
    }

    private List<String> mPeripheralsDiscoveringConnectingOrDiscoveringServices = new ArrayList<>();            // Contains identifiers of peripherals that are connecting (connect + discovery)

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            final String identifier = intent.getStringExtra(BlePeripheral.kExtra_deviceAddress);
            final BlePeripheral blePeripheral = getPeripheralWithIdentifier(identifier);

            if (blePeripheral != null) {
                if (BlePeripheral.kBlePeripheral_OnConnected.equals(action)) {
                    // If connected, start service discovery
                    blePeripheral.discoverServices(new BlePeripheral.CompletionHandler() {
                        @Override
                        public void completion(final int status) {
                            final Handler mainHandler = new Handler(Looper.getMainLooper());
                            final Runnable discoveredServicesRunnable = new Runnable() {
                                @Override
                                public void run() {
                                    mPeripheralsDiscoveringConnectingOrDiscoveringServices.remove(identifier);          // Connection setup finished
                                    Log.d(TAG, "kBlePeripheral_OnConnected ConnectingOrDiscovering: " + Arrays.toString(mPeripheralsDiscoveringConnectingOrDiscoveringServices.toArray()));
                                    if (status == BluetoothGatt.GATT_SUCCESS) {
                                        // Discovery finished
                                        mBlePeripheralDiscoveredServices.setValue(blePeripheral);

                                    } else {
                                        final String message = LocalizationManager.getInstance().getString(getApplication(), "peripheraldetails_errordiscoveringservices");
                                        blePeripheral.disconnect();
                                        mBlePeripheralsConnectionErrorMessage.setValue(message);
                                    }
                                }
                            };
                            mainHandler.post(discoveredServicesRunnable);
                        }
                    });
                } else if (BlePeripheral.kBlePeripheral_OnDisconnected.equals(action)) {
                    Log.d(TAG, "kBlePeripheral_OnDisconnected ConnectingOrDiscovering: " + Arrays.toString(mPeripheralsDiscoveringConnectingOrDiscoveringServices.toArray()));
                    if (mPeripheralsDiscoveringConnectingOrDiscoveringServices.contains(identifier)) {          // If connection setup was still ongoing
                        final boolean isExpected = intent.getStringExtra(BlePeripheral.kExtra_expectedDisconnect) != null;      // If parameter kExtra_expectedDisconnect is non-null, the disconnect was expected (and no message errors are displayed to the user)
                        Log.d(TAG, "Expected disconnect: " + isExpected);
                        if (!isExpected) {
                            final String message = LocalizationManager.getInstance().getString(getApplication(), "bluetooth_connecting_error");
                            mBlePeripheralsConnectionErrorMessage.setValue(message);
                        }
                        mPeripheralsDiscoveringConnectingOrDiscoveringServices.remove(identifier);
                    }
                } else if (BlePeripheral.kBlePeripheral_OnConnecting.equals(action)) {
                    if (!mPeripheralsDiscoveringConnectingOrDiscoveringServices.contains(identifier)) {         // peripheral starts connection setup
                        mPeripheralsDiscoveringConnectingOrDiscoveringServices.add(identifier);
                    }
                    Log.d(TAG, "kBlePeripheral_OnConnecting ConnectingOrDiscovering: " + Arrays.toString(mPeripheralsDiscoveringConnectingOrDiscoveringServices.toArray()));
                }

                mBlePeripheralsConnectionChanged.setValue(blePeripheral);
                final int numDevicesConnected = mScanner.getConnectedPeripherals().size();
                mNumDevicesConnected.setValue(numDevicesConnected);

            } else {
                Log.w(TAG, "ScannerViewModel mGattUpdateReceiver with null peripheral");
            }
        }
    };
// endregion

}
