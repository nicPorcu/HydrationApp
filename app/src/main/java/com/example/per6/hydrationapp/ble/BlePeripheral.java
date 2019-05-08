package com.example.per6.hydrationapp.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;


import com.example.per6.hydrationapp.BuildConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import no.nordicsemi.android.support.v18.scanner.ScanRecord;
import no.nordicsemi.android.support.v18.scanner.ScanResult;

@SuppressWarnings({"ConstantConditions", "PointlessBooleanExpression"})
public class BlePeripheral {
    // Log
    private final static String TAG = BlePeripheral.class.getSimpleName();

    // Constants
    public static final int STATE_DISCONNECTED = BluetoothProfile.STATE_DISCONNECTED;
    public static final int STATE_CONNECTING = BluetoothProfile.STATE_CONNECTING;
    public static final int STATE_CONNECTED = BluetoothProfile.STATE_CONNECTED;
    public static final int STATE_DISCONNECTING = BluetoothProfile.STATE_DISCONNECTING;

    //  Config
    private final static boolean kDebugCommands = BuildConfig.DEBUG && true;         // Set a identifier for each command and verifies that the command processed is the one expected
    private final static boolean kProfileTimeouts = BuildConfig.DEBUG && true;
    private final static String kPrefix = "com.adafruit.bluefruit.bleperipheral.";
    public final static String kBlePeripheral_OnConnecting = kPrefix + "connecting";
    public final static String kBlePeripheral_OnConnected = kPrefix + "connected";
    public final static String kBlePeripheral_OnDisconnected = kPrefix + "disconnected";
    public final static String kBlePeripheral_OnRssiUpdated = kPrefix + "rssiUpdated";
    public final static String kExtra_deviceAddress = kPrefix + "extra_deviceAddress";
    public final static String kExtra_expectedDisconnect = kPrefix + "extra_expectedDisconnect";

    public static UUID kClientCharacteristicConfigUUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private final static boolean kForceWriteWithResponse = false;                    // Possible Android bug?: writing without response still calls onCharacteristicWrite
    private final static boolean kForceWriteWithoutResponse = true;                  // Force without response, or take into account that write response (onCharacteristicWrite) could be reported AFTER onCharacteristicChanged on expecting a response
    private static boolean kHackToAvoidProblemsWhenWriteIsReceivedBeforeChangedOnWriteWithResponse = true;   // On Android when writing on a characteristic with writetype WRITE_TYPE_DEFAULT, onCharacteristicChanged (when a response is expected) can be called before onCharacteristicWrite. This weird behaviour has to be taken into account!!

    // Data
    private ScanResult mScanResult;
    private LocalBroadcastManager mLocalBroadcastManager;
    private BluetoothGatt mBluetoothGatt;

    private int mConnectionState = STATE_DISCONNECTED;
    private CommandQueue mCommmandQueue = new CommandQueue();
    private Map<String, NotifyHandler> mNotifyHandlers = new HashMap<>();
    private List<CaptureReadHandler> mCaptureReadHandlers = new ArrayList<>();

    private int mRssi = 0;


    // region BluetoothGattCallback
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.w(TAG, "onConnectionStateChange from: " + status + " to:" + newState);

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnectionState = STATE_CONNECTED;
                localBroadcastUpdate(kBlePeripheral_OnConnected, getIdentifier());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "onConnectionStateChange STATE_DISCONNECTED");
                notifyConnectionFinished(false);
            } else {
                Log.w(TAG, "unknown onConnectionStateChange from: " + status + " to:" + newState);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            finishExecutingCommand(status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            Log.d(TAG, "onCharacteristicRead");
            if (kDebugCommands) {
                final String identifier = getCharacteristicIdentifier(characteristic);
                BleCommand command = mCommmandQueue.first();
                if (command.mType == BleCommand.BLECOMMANDTYPE_READCHARACTERISTIC && identifier.equals(command.mIdentifier)) {
                    finishExecutingCommand(status);
                } else {
                    Log.w(TAG, "Warning: onCharacteristicRead with no matching command");
                }
            } else {
                finishExecutingCommand(status);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);

            BleCommand command = mCommmandQueue.first();
            if (command != null && !command.mIsCancelled && command.mType == BleCommand.BLECOMMANDTYPE_WRITECHARACTERISTICANDWAITNOTIFY) {
                if (kHackToAvoidProblemsWhenWriteIsReceivedBeforeChangedOnWriteWithResponse) {
                    Log.d(TAG, "onCharacteristicWrite. Ignored");
                    // TODO: fixit
                    // This is not totally correct. If onCharacteristicChanged arrives before onCharacteristicWrite, onCharacteristicChanged should not finishExecutingCommand and wait should be executed when this funtion is called
                } else {
                    Log.d(TAG, "onCharacteristicWrite. Waiting for reponse");
                    final String identifier = getCharacteristicIdentifier(characteristic);
                    if (kDebugCommands && !identifier.equals(command.mIdentifier)) {
                        Log.w(TAG, "Warning: onCharacteristicWrite with no matching command");
                    }

                    BleCommandCaptureReadParameters readParameters = (BleCommandCaptureReadParameters) command.mExtra;
                    CaptureReadHandler captureReadHandler = new CaptureReadHandler(readParameters.readIdentifier, readParameters.completionHandler, readParameters.timeout, mTimeoutRemoveCaptureHandler);
                    Log.d(TAG, "onCharacteristicWrite: add captureReadHandler");
                    mCaptureReadHandlers.add(captureReadHandler);
                }
            } else {
                Log.d(TAG, "onCharacteristicWrite. Finished");
                finishExecutingCommand(status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);

            Log.d(TAG, "onCharacteristicChanged. numCaptureReadHandlers: " + mCaptureReadHandlers.size());

            final String identifier = getCharacteristicIdentifier(characteristic);
            final int status = BluetoothGatt.GATT_SUCCESS;          // On Android, there is no error reported for this callback, so we assume it is SUCCESS

            // Check if waiting to capture this read
            boolean isNotifyOmitted = false;
            boolean hasCaptureHandler = false;

            // Remove capture handler
            final int captureHandlerIndex = getCaptureHandlerIndex(identifier);
            if (captureHandlerIndex >= 0) {
                hasCaptureHandler = true;
                CaptureReadHandler captureReadHandler = mCaptureReadHandlers.remove(captureHandlerIndex);

                // Cancel timeout handler
                if (captureReadHandler.mTimeoutTimer != null) {
                    if (kProfileTimeouts) {
                        Log.d(TAG, "Cancel timeout: " + captureReadHandler.mIdentifier + ". elapsed millis:" + (System.currentTimeMillis() - captureReadHandler.mTimeoutStartingMillis));
                    }
                    captureReadHandler.mTimeoutTimer.cancel();
                    captureReadHandler.mTimeoutTimer = null;
                }

                // Send result
                byte[] value = characteristic.getValue();
                Log.d(TAG, "onCharacteristicChanged: send result to captureReadHandler:" + BleUtils.bytesToHex2(value));
                captureReadHandler.mResult.read(status, value);

                isNotifyOmitted = captureReadHandler.mIsNotifyOmitted;
            }

            // Notify
            if (!isNotifyOmitted) {
                NotifyHandler notifyHandler = mNotifyHandlers.get(identifier);
                if (notifyHandler != null) {
                    notifyHandler.notify(status);
                }
            }

            if (hasCaptureHandler) {
                finishExecutingCommand(status);
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);

            if (kDebugCommands) {
                final String identifier = getDescriptorIdentifier(descriptor.getCharacteristic().getService().getUuid(), descriptor.getCharacteristic().getUuid(), descriptor.getUuid());
                BleCommand command = mCommmandQueue.first();
                if (command.mType == BleCommand.BLECOMMANDTYPE_READDESCRIPTOR && identifier.equals(command.mIdentifier)) {
                    finishExecutingCommand(status);
                } else {
                    Log.w(TAG, "Warning: onDescriptorRead with no matching command");
                }
            } else {
                finishExecutingCommand(status);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);

            //final String identifier = getDescriptorIdentifier(descriptor.getCharacteristic().getService().getUuid(), descriptor.getCharacteristic().getUuid(), descriptor.getUuid());
            BleCommand command = mCommmandQueue.first();
            if (command != null && command.mType == BleCommand.BLECOMMANDTYPE_SETNOTIFY) {
                if (kDebugCommands) {
                    final String identifier = getCharacteristicIdentifier(descriptor.getCharacteristic());
                    if (identifier.equals(command.mIdentifier)) {
                        //Log.d(TAG, "Set Notify descriptor write: " + status);
                        finishExecutingCommand(status);
                    } else {
                        Log.w(TAG, "Warning: onDescriptorWrite for BLECOMMANDTYPE_SETNOTIFY with no matching command");
                    }
                } else {
                    finishExecutingCommand(status);
                }
            } else {
                Log.w(TAG, "Warning: onDescriptorWrite with no matching command");
            }
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                mRssi = rssi;

                localBroadcastUpdate(kBlePeripheral_OnRssiUpdated, getIdentifier());
            } else {
                Log.w(TAG, "onReadRemoteRssi error: " + status);
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
        }
    };

    //
    BlePeripheral(ScanResult scanResult) {
        replaceScanResult(scanResult);
    }

    public static boolean isCharacteristicNotifyingForCachedClientConfigDescriptor(@NonNull BluetoothGattCharacteristic characteristic) {
        // Note: client characteristic descriptor should have been read previously
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(kClientCharacteristicConfigUUID);
        if (descriptor != null) {
            byte[] configValue = descriptor.getValue();
            return Arrays.equals(configValue, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        } else {
            return false;
        }
    }

    void replaceScanResult(ScanResult scanResult) {
        mScanResult = scanResult;
    }

    public int getConnectionState() {
        return mConnectionState;
    }

    public String getIdentifier() {
        return mScanResult.getDevice().getAddress();
    }

    public String getName() {

        String name = mScanResult.getDevice().getName();
        if (name == null) {
            name = getScanRecord().getDeviceName();
        }

        return name;
    }

    public ScanRecord getScanRecord() {
        return mScanResult.getScanRecord();
    }

    public int getRssi() {
        return mScanResult.getRssi();
    }

    public void reset() {
        mRssi = 0;
        mNotifyHandlers.clear();
        mCaptureReadHandlers.clear();
        BleCommand firstCommand = mCommmandQueue.first();
        if (firstCommand != null) {
            firstCommand.cancel();  // Stop current command if is processing
        }
        mCommmandQueue.clear();
    }

    public BluetoothDevice getDevice() {
        return mScanResult.getDevice();
    }

    @MainThread
    public void connect(Context context) {
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(context);
        BluetoothDevice device = mScanResult.getDevice();
        mCommmandQueue.clear();
        mConnectionState = STATE_CONNECTING;
        localBroadcastUpdate(kBlePeripheral_OnConnecting, getIdentifier());
        BleManager.getInstance().cancelDiscovery();        // Always cancel discovery before connecting

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mBluetoothGatt = device.connectGatt(context, false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
        } else {
            mBluetoothGatt = device.connectGatt(context, false, mGattCallback);
        }

        if (mBluetoothGatt == null) {
            Log.e(TAG, "connectGatt Error. Returns null");
        }
    }

    @MainThread
    public void disconnect() {
        if (mBluetoothGatt != null) {
            final boolean wasConnecting = mConnectionState == STATE_CONNECTING;
            mConnectionState = STATE_DISCONNECTING;
            mBluetoothGatt.disconnect();

            if (wasConnecting) {        // Force a disconnect broadcast because it will not be generated by the OS
                notifyConnectionFinished(true);
            }
        }
    }

    public boolean isDisconnected() {
        return mConnectionState == STATE_DISCONNECTED;
    }

    private void notifyConnectionFinished(boolean isExpected) {
        mConnectionState = STATE_DISCONNECTED;
        if (isExpected) {
            localBroadcastUpdate(kBlePeripheral_OnDisconnected, getIdentifier(), kExtra_expectedDisconnect, kExtra_expectedDisconnect);     // Send a extra parameter (kExtra_expectedDisconnect) with any value, so it is known that was expected (and no message errors are displayed to the user)
        } else {
            localBroadcastUpdate(kBlePeripheral_OnDisconnected, getIdentifier());
        }
        closeBluetoothGatt();
        mLocalBroadcastManager = null;
    }

    // endregion


    // region CommandQueue

    @MainThread
    private void closeBluetoothGatt() {
        if (mBluetoothGatt != null) {
            mBluetoothGatt.close();
            mCommmandQueue.clear();
            mBluetoothGatt = null;
        }
    }

    private void localBroadcastUpdate(@NonNull final String action, @NonNull String deviceAddress) {
        localBroadcastUpdate(action, deviceAddress, null, null);
    }

    private void localBroadcastUpdate(@NonNull final String action, @NonNull String deviceAddress, @Nullable String extraParamKey, @Nullable String extraParamValue) {
        if (mLocalBroadcastManager != null) {
            final Intent intent = new Intent(action);
            intent.putExtra(kExtra_deviceAddress, deviceAddress);
            if (extraParamKey != null) {
                intent.putExtra(extraParamKey, extraParamValue);
            }
            mLocalBroadcastManager.sendBroadcast(intent);
        }
    }

    private void finishExecutingCommand(int status) {
        BleCommand command = mCommmandQueue.first();
        if (command != null && !command.mIsCancelled) {
            command.completion(status);
        }
        mCommmandQueue.executeNext();
    }

    public void discoverServices(CompletionHandler completionHandler) {
        BleCommand command = new BleCommand(BleCommand.BLECOMMANDTYPE_DISCOVERSERVICES, null, completionHandler) {
            @Override
            public void execute() {
                final boolean isDiscoveryInProgress = mBluetoothGatt != null && mBluetoothGatt.discoverServices();
                if (!isDiscoveryInProgress) {
                    Log.w(TAG, "Warning: discoverServices failed");
                    finishExecutingCommand(BluetoothGatt.GATT_FAILURE);
                }
            }
        };
        mCommmandQueue.add(command);
    }

    // endregion

    // region Commands

    private String getCharacteristicIdentifier(@NonNull BluetoothGattCharacteristic characteristic) {
        return getCharacteristicIdentifier(characteristic.getService().getUuid(), characteristic.getUuid());
    }

    private String getCharacteristicIdentifier(@NonNull UUID serviceUUID, @NonNull UUID characteristicUUID) {
        return serviceUUID.toString() + characteristicUUID.toString();
    }

    private String getDescriptorIdentifier(@NonNull UUID serviceUUID, @NonNull UUID characteristicUUID, @NonNull UUID descriptorUUID) {
        return serviceUUID.toString() + characteristicUUID.toString() + descriptorUUID.toString();
    }

    public @Nullable
    BluetoothGattService getService(@NonNull UUID uuid) {
        // This function requires that service discovery has been completed for the given device.
        // If multiple instance of the service exist, it returns the first one
        return mBluetoothGatt == null ? null : mBluetoothGatt.getService(uuid);
    }

    public @Nullable
    BluetoothGattCharacteristic getCharacteristic(@NonNull UUID characteristicUUID, @NonNull UUID serviceUUID) {
        // This function requires that service discovery has been completed for the given device.
        BluetoothGattService service = getService(serviceUUID);
        return service == null ? null : service.getCharacteristic(characteristicUUID);
    }

    public void characteristicEnableNotify(@NonNull final BluetoothGattCharacteristic characteristic, final NotifyHandler notifyHandler, CompletionHandler completionHandler) {
        final String identifier = getCharacteristicIdentifier(characteristic);
        BleCommand command = new BleCommand(BleCommand.BLECOMMANDTYPE_SETNOTIFY, kDebugCommands ? identifier : null, completionHandler) {

            @Override
            public void execute() {
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(kClientCharacteristicConfigUUID);
                if (mBluetoothGatt != null && descriptor != null && (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                    mNotifyHandlers.put(identifier, notifyHandler);
                    mBluetoothGatt.setCharacteristicNotification(characteristic, true);
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    mBluetoothGatt.writeDescriptor(descriptor);
                    //characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
                } else {
                    Log.w(TAG, "enable notify: client config descriptor not found for characteristic: " + characteristic.getUuid().toString());
                    finishExecutingCommand(BluetoothGatt.GATT_FAILURE);
                }
            }
        };
        mCommmandQueue.add(command);
    }

    public void characteristicDisableNotify(@NonNull final BluetoothGattCharacteristic characteristic, CompletionHandler completionHandler) {
        final String identifier = getCharacteristicIdentifier(characteristic);
        BleCommand command = new BleCommand(BleCommand.BLECOMMANDTYPE_SETNOTIFY, kDebugCommands ? identifier : null, completionHandler) {

            @Override
            public void execute() {

                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(kClientCharacteristicConfigUUID);
                if (mBluetoothGatt != null && descriptor != null && (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                    mNotifyHandlers.remove(identifier);
                    descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                    mBluetoothGatt.writeDescriptor(descriptor);
                } else {
                    Log.w(TAG, "disable notify: client config descriptor not found for characteristic: " + characteristic.getUuid().toString());
                    finishExecutingCommand(BluetoothGatt.GATT_FAILURE);
                }
            }
        };
        mCommmandQueue.add(command);
    }

    public void characteristicUpdateNotify(@NonNull final BluetoothGattCharacteristic characteristic, NotifyHandler notifyHandler) {
        final String identifier = getCharacteristicIdentifier(characteristic);
        NotifyHandler previousNotifyHandler = mNotifyHandlers.put(identifier, notifyHandler);
        if (previousNotifyHandler == null) {
            Log.d(TAG, "trying to update inexistent notifyHandler for characteristic: " + characteristic.getUuid().toString());
        }
    }

    public void writeCharacteristic(@NonNull final BluetoothGattCharacteristic characteristic, final int writeType, @NonNull final byte[] data, @Nullable CompletionHandler completionHandler) {
        BleCommand command = new BleCommand(BleCommand.BLECOMMANDTYPE_WRITECHARACTERISTIC, kDebugCommands ? getCharacteristicIdentifier(characteristic) : null, completionHandler) {
            @Override
            public void execute() {
                if (mBluetoothGatt != null) {
                    // Write value
                    int selectedWriteType;
                    if (kForceWriteWithoutResponse) {
                        selectedWriteType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE;
                    } else if (kForceWriteWithResponse) {
                        selectedWriteType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT;
                    } else {
                        selectedWriteType = writeType;
                    }

                    characteristic.setWriteType(selectedWriteType);
                    characteristic.setValue(data);
                    final boolean success = mBluetoothGatt.writeCharacteristic(characteristic);
                    if (success) {

                        // Simulate response if needed
                        if (selectedWriteType == BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE) {
                            finishExecutingCommand(BluetoothGatt.GATT_SUCCESS);
                        }
                    } else {
                        Log.w(TAG, "writeCharacteristic could not be initiated");
                        finishExecutingCommand(BluetoothGatt.GATT_FAILURE);
                    }

                } else {
                    Log.w(TAG, "mBluetoothGatt is null");
                    finishExecutingCommand(BluetoothGatt.GATT_FAILURE);
                }
            }
        };
        mCommmandQueue.add(command);
    }

    public void readDescriptor(@NonNull final BluetoothGattCharacteristic characteristic, final UUID descriptorUUID, CompletionHandler completionHandler) {
        final String identifier = kDebugCommands ? getDescriptorIdentifier(characteristic.getService().getUuid(), characteristic.getUuid(), descriptorUUID) : null;
        BleCommand command = new BleCommand(BleCommand.BLECOMMANDTYPE_READDESCRIPTOR, identifier, completionHandler) {
            @Override
            public void execute() {
                // Read Descriptor
                final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(descriptorUUID);
                if (descriptor != null) {
                    mBluetoothGatt.readDescriptor(descriptor);
                } else {
                    Log.w(TAG, "read: descriptor not found: " + descriptorUUID.toString());
                    finishExecutingCommand(BluetoothGatt.GATT_READ_NOT_PERMITTED);
                }
            }
        };
        mCommmandQueue.add(command);
    }

    public interface CompletionHandler {
        void completion(int status);
    }

    public interface NotifyHandler {
        void notify(int status);
    }

    public interface CaptureReadCompletionHandler {
        void read(int status, @Nullable byte[] value);

        interface TimeoutAction {
            void execute(String identifier);
        }
    }

    // endregion

    // region CaptureReadHandler
    public static final int kPeripheralReadTimeoutError = -1;       // Value should be different that errors defined in BluetoothGatt.GATT_*

    static class CaptureReadHandler {
        private String mIdentifier;
        private CaptureReadCompletionHandler mResult;
        private Timer mTimeoutTimer;
        private long mTimeoutStartingMillis;        // only used for debug (kProfileTimeouts)
        private CaptureReadCompletionHandler.TimeoutAction mTimeoutAction;
        private boolean mIsNotifyOmitted;

        CaptureReadHandler(String identifier, CaptureReadCompletionHandler result, int timeout, @Nullable CaptureReadCompletionHandler.TimeoutAction timeoutAction) {
            this(identifier, result, timeout, timeoutAction, false);
        }

        CaptureReadHandler(final String identifier, CaptureReadCompletionHandler result, int timeout, @Nullable CaptureReadCompletionHandler.TimeoutAction timeoutAction, boolean isNotifyOmitted) {
            mIdentifier = identifier;
            mResult = result;
            mIsNotifyOmitted = isNotifyOmitted;

            // Setup timeout if not zero
            if (timeout > 0 && timeoutAction != null) {
                mTimeoutAction = timeoutAction;

                mTimeoutTimer = new Timer();
                if (kProfileTimeouts) {
                    mTimeoutStartingMillis = System.currentTimeMillis();
                    Log.d(TAG, "Start timeout:  " + identifier + ". millis:" + mTimeoutStartingMillis);
                }
                mTimeoutTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (kProfileTimeouts) {
                            Log.d(TAG, "Fire timeout:   " + identifier + ". elapsed millis:" + (System.currentTimeMillis() - mTimeoutStartingMillis));
                        }
                        mResult.read(kPeripheralReadTimeoutError, null);
                        mTimeoutAction.execute(identifier);
                    }
                }, timeout);
            }
        }
    }

    private int getCaptureHandlerIndex(String identifier) {
        boolean found = false;
        int i = 0;
        if (mCaptureReadHandlers.size() > 0) {
            while (i < mCaptureReadHandlers.size() && !found) {
                if (mCaptureReadHandlers.get(i).mIdentifier.equals(identifier)) {
                    found = true;
                } else {
                    i++;
                }
            }
        }
        return found ? i : -1;
    }

    private CaptureReadCompletionHandler.TimeoutAction mTimeoutRemoveCaptureHandler = new CaptureReadCompletionHandler.TimeoutAction() {
        @Override
        public void execute(String identifier) {        // Default behaviour for a capture handler timeout
            // Remove capture handler
            final int captureHandlerIndex = BlePeripheral.this.getCaptureHandlerIndex(identifier);
            if (captureHandlerIndex >= 0) {
                mCaptureReadHandlers.remove(captureHandlerIndex);
            }
            BlePeripheral.this.finishExecutingCommand(kPeripheralReadTimeoutError);
        }
    };

    static class BleCommandCaptureReadParameters {
        String readIdentifier;
        CaptureReadCompletionHandler completionHandler;
        int timeout;

        BleCommandCaptureReadParameters(@NonNull String readIdentifier, @Nullable CaptureReadCompletionHandler completionHandler, int timeout) {
            this.readIdentifier = readIdentifier;
            this.completionHandler = completionHandler;
            this.timeout = timeout;
        }
    }

    // endregion

    // region BleCommand
    abstract class BleCommand {
        // Command types
        static final int BLECOMMANDTYPE_DISCOVERSERVICES = 1;
        static final int BLECOMMANDTYPE_SETNOTIFY = 2;       // TODO: add support for indications
        static final int BLECOMMANDTYPE_READCHARACTERISTIC = 3;
        static final int BLECOMMANDTYPE_WRITECHARACTERISTIC = 4;
        static final int BLECOMMANDTYPE_WRITECHARACTERISTICANDWAITNOTIFY = 5;
        static final int BLECOMMANDTYPE_READDESCRIPTOR = 6;

        // Data
        private int mType;
        private String mIdentifier;
        private boolean mIsCancelled = false;
        private CompletionHandler mCompletionHandler;
        private Object mExtra;

        //
        BleCommand(int type, @Nullable String identifier, @Nullable CompletionHandler completionHandler) {
            this(type, identifier, completionHandler, null);
        }

        BleCommand(int type, @Nullable String identifier, @Nullable CompletionHandler completionHandler, @Nullable Object extra) {
            mType = type;
            mIdentifier = identifier;
            mCompletionHandler = completionHandler;
            mExtra = extra;
        }

        void cancel() {
            mIsCancelled = true;
        }

        void completion(int status) {
            if (mCompletionHandler != null) {
                mCompletionHandler.completion(status);
            }
        }

        // @return true if finished
        abstract void execute();
    }

    class CommandQueue {
        @NonNull
        private final List<BleCommand> mQueue = new ArrayList<>();

        void add(@NonNull BleCommand command) {

            boolean shouldExecute;
            synchronized (mQueue) {
                shouldExecute = mQueue.isEmpty();
                mQueue.add(command);
            }

            if (shouldExecute) {
                command.execute();
            }
        }

        BleCommand first() {
            synchronized (mQueue) {
                return mQueue.isEmpty() ? null : mQueue.get(0);
            }
        }

        void clear() {
            synchronized (mQueue) {
                mQueue.clear();
            }
        }

        void executeNext() {
            BleCommand nextCommand = null;
            synchronized (mQueue) {
                if (!mQueue.isEmpty()) {
                    mQueue.remove(0);
                }
                if (!mQueue.isEmpty()) {
                    nextCommand = mQueue.get(0);
                }
            }

            if (nextCommand != null) {
                nextCommand.execute();
            }
        }

        boolean containsCommandType(int type) {
            synchronized (mQueue) {
                int i = 0;
                boolean found = false;
                while (!found && i < mQueue.size()) {
                    found = mQueue.get(i).mType == type;
                    i++;
                }
                return found;
            }
        }
    }

    // endregion
}