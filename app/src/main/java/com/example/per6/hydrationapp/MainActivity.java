package com.example.per6.hydrationapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;


import com.example.per6.hydrationapp.ble.BlePeripheral;
import com.example.per6.hydrationapp.ble.BleScanner;
import com.example.per6.hydrationapp.ble.ScannerViewModel;

import static android.support.v4.app.NotificationCompat.VISIBILITY_PUBLIC;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = "MainActivity";
    private static final String CHANNEL_ID = "channelId";
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    private Context context;
    private Fragment currentFragment;
    private FragmentManager fm;
    private static final int bluetoothActivityRequestCode=3;


    private boolean hasUserAlreadyBeenAskedAboutBluetoothStatus = false;
    private boolean isConnectedToBluetooth;
    private String peripheralIdentifier;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        context = this;
        sharedPref = context.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        fm = getSupportFragmentManager();

        if(savedInstanceState == null){
            Intent i = new Intent(this, BluetoothActivity.class);
            startActivityForResult(i, bluetoothActivityRequestCode );
        } else {
            Intent i=getIntent();
            peripheralIdentifier=i.getStringExtra("peripheralIdentifier");
            HomepageFragment fragment = HomepageFragment.newInstance(peripheralIdentifier);
            if (fm != null) {
                FragmentTransaction fragmentTransaction = fm.beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_right, R.anim.slide_out_left)
                        .replace(R.id.fragment_container, fragment, "Modules");
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            }
        }
//        createNotificationChannel();
//        buildNotification();
//        peripheralIdentifier = sharedPref.getString(getResources().getString(R.string.peripheralIdentiferSharedPref), "");
//        if(peripheralIdentifier.equals("")){
//            Intent i = new Intent(this, BluetoothActivity.class);
//            startActivityForResult(i, bluetoothActivityRequestCode );
//        } else {
//            BlePeripheral blePeripheral = BleScanner.getInstance().getPeripheralWithIdentifier(peripheralIdentifier);
//            Log.d(TAG, "onCreate: "+peripheralIdentifier);
//            if(blePeripheral == null) {
//                Log.d(TAG, "onCreate: null");
//                Intent i = new Intent(this, BluetoothActivity.class);
//                startActivityForResult(i, bluetoothActivityRequestCode );
//            } else {
//                blePeripheral.connect(this);
//                if (blePeripheral.isDisconnected()) {
//                    Log.d(TAG, "onCreate: diconnected/null");
//                    Intent i = new Intent(this, BluetoothActivity.class);
//                    startActivityForResult(i, bluetoothActivityRequestCode);
//                }
//            }
//        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            currentFragment = HomepageFragment.newInstance(peripheralIdentifier);

        } else if (id == R.id.nav_leaderboard) {
            currentFragment = LeaderboardFragment.newInstance(peripheralIdentifier);

        } else if (id == R.id.nav_myInfo) {
            currentFragment = MyWaterBottlesFragment.newInstance(peripheralIdentifier);

        } else if (id == R.id.nav_customization) {
            currentFragment = MyInfoFragment.newInstance(peripheralIdentifier);

        } else if (id == R.id.nav_settings) {
            currentFragment = SettingsFragment.newInstance(peripheralIdentifier);

        }
        if(currentFragment != null){
            fm.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_right, R.anim.slide_out_left)
                    .replace(R.id.fragment_container, currentFragment, "MyWaterBottles")
                    .commit();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("hasUserAlreadyBeenAskedAboutBluetoothStatus", hasUserAlreadyBeenAskedAboutBluetoothStatus);
    }

    // endregion

    @Override
    protected void onResume() {
        super.onResume();

        // Observe disconnections
        registerGattReceiver();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();

  //      popFragmentsIfNoPeripheralsConnected();         // check if peripherals were disconnected while the app was in background
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterGattReceiver();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == bluetoothActivityRequestCode){
            if(resultCode == RESULT_OK) {
                peripheralIdentifier = data.getStringExtra("peripheralIdentifier");

                if(peripheralIdentifier.equals("")){
                    isConnectedToBluetooth = false;
                }else{
                    isConnectedToBluetooth = true;
                }
                HomepageFragment fragment = HomepageFragment.newInstance(peripheralIdentifier);
                FragmentManager fragmentManager = getSupportFragmentManager();
                if (fragmentManager != null) {
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_right, R.anim.slide_out_left)
                            .replace(R.id.fragment_container, fragment, "Modules");
                    fragmentTransaction.addToBackStack(null);
                    fragmentTransaction.commit();
                }
            }


        }
    }

    // region Broadcast Listener
    private void registerGattReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BlePeripheral.kBlePeripheral_OnDisconnected);
        LocalBroadcastManager.getInstance(this).registerReceiver(mGattUpdateReceiver, filter);
    }

    private void unregisterGattReceiver() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mGattUpdateReceiver);
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BlePeripheral.kBlePeripheral_OnDisconnected.equals(action)) {
            }
        }
    };


    private void buildNotification(){
        Intent intent = new Intent(this, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        //todo get notification freq and set it to be eaul to shared pref at getString(R.string.notification_frequency)

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("")
                .setContentText("Drink water")
                .setSmallIcon(R.drawable.happy_dog)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(VISIBILITY_PUBLIC)
                .setAutoCancel(true);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        int notificationId=5;
        notificationManager.notify(notificationId, builder.build());


    }


    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
