package com.example.per6.hydrationapp.ble;

import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;


import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

public class UartPacketManagerBase implements BlePeripheralUart.UartRxHandler {
    // Log
    private final static String TAG = UartPacketManagerBase.class.getSimpleName();

    // Listener
    public interface Listener {
        void onUartPacket(UartPacket packet);
    }

    // Data
    //private boolean mIsEnabled = false;
    protected final Handler mMainHandler = new Handler(Looper.getMainLooper());
    protected WeakReference<Listener> mWeakListener;
    protected List<UartPacket> mPackets = new ArrayList<>();
    protected Semaphore mPacketsSemaphore = new Semaphore(1, true);
    private boolean mIsPacketCacheEnabled;
    protected Context mContext;

    protected long mReceivedBytes = 0;
    protected long mSentBytes = 0;

    public UartPacketManagerBase(@NonNull Context context, @Nullable Listener listener, boolean isPacketCacheEnabled) {
        mContext = context.getApplicationContext();
        mIsPacketCacheEnabled = isPacketCacheEnabled;
        mWeakListener = new WeakReference<>(listener);
    }

    // region Received data: UartRxHandler

    @Override
    public void onRxDataReceived(@NonNull byte[] data, @Nullable String identifier, int status) {

        Log.d(TAG, "onRxDataReceived: data recieved");
        
        if (status != BluetoothGatt.GATT_SUCCESS) {
            Log.w(TAG, "onRxDataReceived error:" + status);
            return;
        }

        final UartPacket uartPacket = new UartPacket(identifier, UartPacket.TRANSFERMODE_RX, data);

        try {
            mPacketsSemaphore.acquire();
        } catch (InterruptedException e) {
            Log.w(TAG, "InterruptedException: " + e.toString());
        }
        mReceivedBytes += data.length;
        if (mIsPacketCacheEnabled) {
            mPackets.add(uartPacket);
        }

        // Send data to delegate
        final Listener listener = mWeakListener.get();
        if (listener != null) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onUartPacket(uartPacket);
                }
            });
        }
        Log.d(TAG, "onRxDataReceived: data 0" +data[0]);
        Log.d(TAG, "onRxDataReceived: data 1" +data[1]);
        Log.d(TAG, "onRxDataReceived: data 2" +data[2]);
        if (data.length>3) {
            Log.d(TAG, "onRxDataReceived: data 3" + data[3]);
            Log.d(TAG, "onRxDataReceived: data 4" + data[4]);
            Log.d(TAG, "onRxDataReceived: data 5" + data[5]);
        }


        //mReceivedBytes += data.length;
        mPacketsSemaphore.release();
    }

    public void clearPacketsCache() {
        mPackets.clear();
    }

    public List<UartPacket> getPacketsCache() {
        return mPackets;
    }

    public long getReceivedBytes() {
        return mReceivedBytes;
    }

    // endregion
}
