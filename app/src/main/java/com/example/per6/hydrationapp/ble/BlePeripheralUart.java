package com.example.per6.hydrationapp.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.ParcelUuid;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class BlePeripheralUart {
    // Log
    private final static String TAG = BlePeripheralUart.class.getSimpleName();

    // Constants
    private static final UUID kUartServiceUUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID kUartTxCharacteristicUUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
    private static final UUID kUartRxCharacteristicUUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");

    private static final int kUartTxMaxBytes = 20;
    public static final int kUartReplyDefaultTimeout = 2000;       // in millis

    // Interfaces
    public interface UartRxHandler {
        void onRxDataReceived(@NonNull byte[] data, @Nullable String identifier, int status);
    }

    // Data
    @NonNull
    private BlePeripheral mBlePeripheral;
    private BluetoothGattCharacteristic mUartTxCharacteristic;
    private BluetoothGattCharacteristic mUartRxCharacteristic;
    private int mUartTxCharacteristicWriteType;

    // region Initialization
    public BlePeripheralUart(@NonNull BlePeripheral blePeripheral) {
        super();
        mBlePeripheral = blePeripheral;
    }

    public void uartEnable(@Nullable UartRxHandler uartRxHandler, @Nullable final BlePeripheral.CompletionHandler completionHandler) {

        // Get uart communications characteristic
        mUartTxCharacteristic = mBlePeripheral.getCharacteristic(kUartTxCharacteristicUUID, kUartServiceUUID);
        mUartRxCharacteristic = mBlePeripheral.getCharacteristic(kUartRxCharacteristicUUID, kUartServiceUUID);
        if (mUartTxCharacteristic != null && mUartRxCharacteristic != null) {
            Log.d(TAG, "Uart Enable for: " + getName());

            mUartTxCharacteristicWriteType = mUartTxCharacteristic.getWriteType();

            // Prepare notification handler
            final WeakReference<UartRxHandler> weakUartRxHandler = new WeakReference<>(uartRxHandler);
            final String identifier = mBlePeripheral.getIdentifier();
            final BlePeripheral.NotifyHandler notifyHandler = uartRxHandler == null ? null : new BlePeripheral.NotifyHandler() {
                @Override
                public void notify(int status) {
                    UartRxHandler handler = weakUartRxHandler.get();
                    if (handler != null) {
                        byte[] data = mUartRxCharacteristic.getValue();
                        Log.d(TAG, "uartEnable: " + data.toString());
                        handler.onRxDataReceived(data, identifier, status);
                    }
                }
            };

            // Check if already notifying (read client characteristic config descriptor to check it)
            mBlePeripheral.readDescriptor(mUartRxCharacteristic, BlePeripheral.kClientCharacteristicConfigUUID, new BlePeripheral.CompletionHandler() {
                @Override
                public void completion(int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        // Enable notifications
                        if (!BlePeripheral.isCharacteristicNotifyingForCachedClientConfigDescriptor(mUartRxCharacteristic)) {
                            mBlePeripheral.characteristicEnableNotify(mUartRxCharacteristic, notifyHandler, completionHandler);
                        } else {
                            mBlePeripheral.characteristicUpdateNotify(mUartRxCharacteristic, notifyHandler);
                            if (completionHandler != null) {
                                completionHandler.completion(BluetoothGatt.GATT_SUCCESS);
                            }
                        }
                    } else {
                        if (completionHandler != null) {
                            completionHandler.completion(status);
                        }
                    }
                }
            });
        }
    }

    public boolean isUartEnabled() {
        return mUartRxCharacteristic != null && mUartTxCharacteristic != null && BlePeripheral.isCharacteristicNotifyingForCachedClientConfigDescriptor(mUartRxCharacteristic);
    }

    public void uartDisable() {
        // Disable notify
        if (mUartRxCharacteristic != null) {
            Log.d(TAG, "Uart Disable");
            mBlePeripheral.characteristicDisableNotify(mUartRxCharacteristic, null);
        }

        // Clear all Uart specific data
        mUartRxCharacteristic = null;
        mUartTxCharacteristic = null;
    }

    public void disconnect() {
        mBlePeripheral.disconnect();
    }

    public String getIdentifier() {
        return mBlePeripheral.getIdentifier();
    }

    public String getName() {
        return mBlePeripheral.getName();
    }

    // endregion

    // region Send
    public void uartSend(@NonNull final byte[] data, @Nullable final BlePeripheral.CompletionHandler completionHandler) {
        if (mUartTxCharacteristic == null) {
            Log.e(TAG, "Command Error: characteristic no longer valid");
            if (completionHandler != null) {
                completionHandler.completion(BluetoothGatt.GATT_FAILURE);
            }
            return;
        }

        // Split data in kUartTxMaxBytes bytes packets
        int offset = 0;
        do {
            final int packetSize = Math.min(data.length - offset, kUartTxMaxBytes);
            final byte[] packet = Arrays.copyOfRange(data, offset, offset + packetSize);
            offset += packetSize;

            final int finalOffset = offset;
            mBlePeripheral.writeCharacteristic(mUartTxCharacteristic, mUartTxCharacteristicWriteType, packet, new BlePeripheral.CompletionHandler() {
                @Override
                public void completion(int status) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        Log.d(TAG, "uart tx write (hex): " + BleUtils.bytesToHex2(packet));
                    } else {
                        Log.w(TAG, "Error " + status + " writing packet at offset: " + finalOffset);
                    }

                    if (finalOffset >= data.length && completionHandler != null) {
                        completionHandler.completion(status);
                    }
                }
            });

        } while (offset < data.length);
    }

    // endregion

    // region Utils

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isUartInitialized(@NonNull BlePeripheral blePeripheral, @NonNull List<BlePeripheralUart> blePeripheralUarts) {
        BlePeripheralUart blePeripheralUart = getBlePeripheralUart(blePeripheral, blePeripheralUarts);
        return blePeripheralUart != null && blePeripheralUart.isUartEnabled();
    }

    private @Nullable
    static BlePeripheralUart getBlePeripheralUart(@NonNull BlePeripheral blePeripheral, @NonNull List<BlePeripheralUart> blePeripheralUarts) {

        BlePeripheralUart result = null;

        String identifier = blePeripheral.getIdentifier();
        boolean found = false;
        int i = 0;
        while (i < blePeripheralUarts.size() && !found) {
            BlePeripheralUart blePeripheralUart = blePeripheralUarts.get(i);
            if (blePeripheralUart.getIdentifier().equals(identifier)) {
                found = true;
                result = blePeripheralUart;
            } else {
                i++;
            }
        }
        return result;
    }

    // endregion
}
