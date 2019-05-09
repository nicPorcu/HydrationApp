package com.example.per6.hydrationapp.ble;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;


import java.nio.charset.Charset;

public class UartPacketManager extends UartPacketManagerBase {
    // Log
    private final static String TAG = UartPacketManager.class.getSimpleName();

    // region Lifecycle
    public UartPacketManager(@NonNull Context context, @Nullable UartPacketManagerBase.Listener listener, boolean isPacketCacheEnabled) {
        super(context, listener, isPacketCacheEnabled);

    }
    // endregion

    // region Send data

    public void send(@NonNull BlePeripheralUart uartPeripheral, @NonNull String text) {
        // Create data and send to Uart
        byte[] data = text.getBytes(Charset.forName("UTF-8"));
        final UartPacket uartPacket = new UartPacket(uartPeripheral.getIdentifier(), UartPacket.TRANSFERMODE_TX, data);

        try {
            mPacketsSemaphore.acquire();        // don't append more data, till the delegate has finished processing it
        } catch (InterruptedException e) {
            Log.w(TAG, "InterruptedException: " + e.toString());
        }
        mPacketsSemaphore.release();

        final Listener listener = mWeakListener.get();
        mPackets.add(uartPacket);
        if (listener != null) {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    listener.onUartPacket(uartPacket);
                }
            });
        }
        mSentBytes += data.length;
        uartPeripheral.uartSend(data, null);
    }

    // endregion
}
