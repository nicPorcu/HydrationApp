package com.example.per6.hydrationapp.ble;

public class UartPacket {
    public static final int TRANSFERMODE_TX = 0;
    public static final int TRANSFERMODE_RX = 1;

    private String mPeripheralId;
    private long mTimestamp;        // in millis
    private int mMode;
    private byte[] mData;


    public UartPacket(String peripheralId, int mode, byte[] data) {
        this(peripheralId, 0, mode, data);
    }

    public UartPacket(String peripheralId, long timestamp, int mode, byte[] data) {
        mPeripheralId = peripheralId;
        mTimestamp = timestamp;
        mMode = mode;
        mData = data;
    }

    public byte[] getData() {
        return mData;
    }
}