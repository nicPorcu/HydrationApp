package com.example.per6.hydrationapp;

import android.app.AlertDialog;
import android.bluetooth.BluetoothGatt;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.per6.hydrationapp.ble.BlePeripheral;
import com.example.per6.hydrationapp.ble.BlePeripheralUart;
import com.example.per6.hydrationapp.ble.BleScanner;
import com.example.per6.hydrationapp.ble.UartPacket;
import com.example.per6.hydrationapp.ble.UartPacketManager;
import com.example.per6.hydrationapp.ble.UartPacketManagerBase;
import com.example.per6.hydrationapp.utils.DialogUtils;

import java.lang.ref.WeakReference;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class CalibrationActivity extends AppCompatActivity implements UartPacketManagerBase.Listener {
    private static final String TAG = "CalibrationActivity";
    private FloatingActionButton nextButton;
    private Button finish;
    private int fullBottle;
    private TextView instructions;
    private int[] measurements;
    private int measurementNumber;
    private Context context;
    private WaterBottle waterBottle;
    private UartPacketManagerBase mUartData;
    private List<BlePeripheralUart> mBlePeripheralsUart = new ArrayList<>();
    private BlePeripheral mBlePeripheral;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private boolean uartSetup = false;
    private long numberOfRecievedBytes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration);
        Intent i = getIntent();
        waterBottle = i.getParcelableExtra("waterBottle");
        String singlePeripheralIdentifier = i.getStringExtra("singlePeripheralIdentifier");
        mBlePeripheral = BleScanner.getInstance().getPeripheralWithIdentifier(singlePeripheralIdentifier);
        wireWidgets();

    }

    private void wireWidgets() {
        //todo make sure connected, and cancel if bottle disconnects
        context = this;
        fullBottle = 40; //todo update
        measurementNumber = 0;
        numberOfRecievedBytes = 0;
        measurements = new int[fullBottle];
        nextButton = findViewById(R.id.nextButton);
        nextButton.setOnClickListener(view -> {
            if (measurementNumber < fullBottle) {
                    instructions.setText("Pour 1oz of water into the bottle, screw the lid back on and press next, press done when the water bottle is full");
                    if (uartSetup) {
                        send();
                        nextButton.setClickable(false);
                    } else {
                        setupUart();
                    }
                } else {
                    instructions.setText("Thank you! Now your water bottle is ready for use");
                }
        });

        finish = findViewById(R.id.doneButton);
        finish.setText("Finish");
        finish.setOnClickListener((View view2) -> {
            //todo save in backendless
        });
        instructions = findViewById(R.id.instruction);
        instructions.setText("Ready to Calibrate?\n Empty your water bottle and place the Kiwi Companion lid on and turn it on.\n Press next when you have done this");
        setupUart();
    }

    protected void setupUart() {
        mUartData = new UartPacketManager(context, this, true);
        mUartData.getReceivedBytes();
        // Enable uart

        if (!BlePeripheralUart.isUartInitialized(mBlePeripheral, mBlePeripheralsUart)) { // If was not previously setup (i.e. orientation change)
            BlePeripheralUart blePeripheralUart = new BlePeripheralUart(mBlePeripheral);
            mBlePeripheralsUart.add(blePeripheralUart);
            blePeripheralUart.uartEnable(mUartData, status -> mMainHandler.post(() -> {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    // Done
                    Log.d(TAG, "Uart enabled");
                    uartSetup = true;
                } else {
                    WeakReference<BlePeripheralUart> weakBlePeripheralUart = new WeakReference<>(blePeripheralUart);
                    Context context1 = context;
                    if (context1 != null) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context1);
                        AlertDialog dialog = builder.setMessage(R.string.uart_error_peripheralinit)
                                .setPositiveButton(android.R.string.ok, (dialogInterface, which) -> {
                                    BlePeripheralUart strongBlePeripheralUart = weakBlePeripheralUart.get();
                                    if (strongBlePeripheralUart != null) {
                                        strongBlePeripheralUart.disconnect();
                                    }
                                })
                                .show();
                        DialogUtils.keepDialogOnOrientationChanges(dialog);
                    }
                }
            }));
        }
    }

    public void send() {
        if (!(mUartData instanceof UartPacketManager)) {
            Log.e(TAG, "Error send with invalid uartData class");
            return;
        }

        if (mBlePeripheralsUart.size() == 0) {
            Log.e(TAG, "mBlePeripheralsUart not initialized");
            return;
        }
        UartPacketManager uartData = (UartPacketManager) mUartData;

        BlePeripheralUart blePeripheralUart = mBlePeripheralsUart.get(0);
        uartData.send(blePeripheralUart, "0");
    }

    @Override
    public void onUartPacket(UartPacket packet) {
        byte[] bytes = packet.getData();
        String text = new String(bytes, Charset.forName("UTF-8"));
        measurements[measurementNumber] = Integer.parseInt(text); //placeholder
        Log.d(TAG, "onUartPacket: "+measurements[measurementNumber]);
        measurementNumber++;
        if(numberOfRecievedBytes != mUartData.getReceivedBytes()){ //if new data
            nextButton.setClickable(true);
            numberOfRecievedBytes = mUartData.getReceivedBytes();
        }
    }
}
