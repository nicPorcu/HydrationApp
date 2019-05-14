package com.example.per6.hydrationapp;

import android.app.AlertDialog;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.per6.hydrationapp.ble.BleManager;
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
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;


import com.backendless.Backendless;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import static com.example.per6.hydrationapp.ConnectedPeripheralFragment.createFragmentArgs;

public class HomepageFragment extends Fragment implements UartPacketManagerBase.Listener{
    private static final String TAG = "Homepage Fragment" ;


    private static String singlePeripheralIdentifierMaster;

    private View rootView;

    private ImageView imageDog;
    private ProgressBar progressBarWater;
    private TextView textLastSync;
    private double dogLevel;
    private WaterBottle waterBottle;
    private Context context;
    private UartPacketManagerBase mUartData;
    private List<BlePeripheralUart> mBlePeripheralsUart = new ArrayList<>();
    private BlePeripheral mBlePeripheral;
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private boolean uartSetup = false;
    private long numberOfRecievedBytes;
    private int dailyWaterGoal;
    private int currentWaterConsumption;
    private BottleMeasurement dataPoints[];
    private WaterBottle currentBottle;
    private GregorianCalendar calendar;
    private long timeLastSynced;
    private LineGraphSeries<DataPoint> dataForGraph;
    private GraphView graphView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ArrayList<String> values;
    private String text;

    public HomepageFragment() {
        // Required empty public constructor
    }
    public static HomepageFragment newInstance(@Nullable String peripheralIdentifier) {
        HomepageFragment fragment = new HomepageFragment();
        fragment.setArguments(createFragmentArgs(peripheralIdentifier));
        singlePeripheralIdentifierMaster = peripheralIdentifier;

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, //Use as onCreate
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_homepage, container, false);
        wireWidgets();
        return rootView;
    }

    private void wireWidgets() {

        //dailyWaterGoal = (Integer) Backendless.UserService.CurrentUser().getProperty("dailyWaterGoal");
        dailyWaterGoal = 90;
        currentWaterConsumption = 50;
        //todo, dont crash app if they press skip

        //dogChange(0.0);
        //imageDog.setImageResource(R.drawable.ic_menu_camera);

        graphView = rootView.findViewById(R.id.graphView);
        dataForGraph = new LineGraphSeries<DataPoint>();

        context = getActivity();

        values = new ArrayList<>();
        text = "";

        imageDog = rootView.findViewById(R.id.imageDog);
        imageDog.setImageDrawable(getResources().getDrawable(R.drawable.happy_dog));

        textLastSync = rootView.findViewById(R.id.textLastSync);

        progressBarWater = rootView.findViewById(R.id.progressBarWater);
        progressBarWater.setMax(dailyWaterGoal);
        progressBarWater.setProgress(currentWaterConsumption);

        waterBottle = new WaterBottle(); //todo get current bottle

        calendar = new GregorianCalendar(TimeZone.getDefault());
        timeLastSynced = calendar.getTime().getTime(); //todo change to save between states

        mSwipeRefreshLayout = rootView.findViewById(R.id.syncLayout);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (uartSetup) {
                    send();
                } else {
                    setupUart();
                }
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                }, 500);
            }
        });

        mBlePeripheral = BleScanner.getInstance().getPeripheralWithIdentifier(singlePeripheralIdentifierMaster);
        if(mBlePeripheral != null && !mBlePeripheral.isDisconnected()){
            setupUart();
        }

    }

    private void dogChange(double change){
        dogLevel = 1.5;
        if(!(dogLevel + change > 2) || !(dogLevel + change < 0)){
            dogLevel += change;
        }
        else{
            if(dogLevel + change > 2 ) {dogLevel = 2;}
            else{dogLevel = 0;}
        }
        if(dogLevel < .75) {
            //imageDog.setImageResource(R.drawable.sad_dog_image);
        }
        if(dogLevel >= .75 && dogLevel <= 1.25){
            //imageDog.setImageResource(R.drawable.neutral_dog_image);
        }
        if(dogLevel > 1.25){
            //imageDog.setImageResource(R.drawable.happy_dog_image);
        }
    }

    protected void setupUart() {
        mUartData = new UartPacketManager(context, this, true);
        // Enable uart

        if (!BlePeripheralUart.isUartInitialized(mBlePeripheral, mBlePeripheralsUart)) { // If was not previously setup (i.e. orientation change)
            BlePeripheralUart blePeripheralUart = new BlePeripheralUart(mBlePeripheral);
            mBlePeripheralsUart.add(blePeripheralUart);
            blePeripheralUart.uartEnable(mUartData, status -> mMainHandler.post(() -> {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "Uart enabled");
                    uartSetup = true;
                    send();
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
        } else {
            if(mBlePeripheral != null && !mBlePeripheral.isDisconnected()){
                send();
            }
        }
    }

    public void send() {
        Log.d(TAG, "send: ");
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
        uartData.send(blePeripheralUart, "1");
    }

    @Override
    public void onUartPacket(UartPacket packet) {
        byte[] bytes = packet.getData();
        if(numberOfRecievedBytes != mUartData.getReceivedBytes()){ //if new data
            text = text + new String(bytes, Charset.forName("UTF-8"));
            String[] temp = text.split(",");
            if(temp[temp.length-1].equals("-1")){
                values.addAll(Arrays.asList(temp));
                values.remove(values.size()-1);
                dataPoints = new BottleMeasurement[values.size()];
                for (int i = 0; i < values.size(); i = i+2){
                    long time = timeLastSynced + Integer.parseInt(values.get(i)); //adds millis to millis to get time of measurement
                    dataPoints[i] = new BottleMeasurement(time, Integer.parseInt(values.get(i+1)), currentBottle);
                    Log.d(TAG, "onUartPacket: "+Integer.parseInt(values.get(i))+ Integer.parseInt(values.get(i+1)));
                }
                //fillGraph(dataPoints);
                timeLastSynced = calendar.getTime().getTime();
                numberOfRecievedBytes = mUartData.getReceivedBytes();
            }
        }
    }

    private void fillGraph(BottleMeasurement[] dataPoints) {
        for(BottleMeasurement b: dataPoints){
            int x = (int) b.getDateTime();
            double y = b.getOz();
            dataForGraph.appendData(new DataPoint(x, y), true, dataPoints.length);
        }
        Paint p = new Paint();
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(5);
        p.setColor(getResources().getColor(R.color.colorAccent));
        dataForGraph.setCustomPaint(p);
        graphView.addSeries(dataForGraph);

        currentWaterConsumption += dataPoints[dataPoints.length-1].getOz(); //todo fix to count refills

    }

}
