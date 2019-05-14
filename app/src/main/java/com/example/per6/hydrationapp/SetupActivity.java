package com.example.per6.hydrationapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;

public class SetupActivity extends AppCompatActivity {

    private static final String TAG = "SetupActivity";
    private SharedPreferences sharedPref;
    private Context context;
    private Fragment currentFragment;
    private FragmentManager fm;
    private static final int bluetoothActivityRequestCode=3;
    private String peripheralIdentifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        context=this;
        sharedPref = context.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        fm = getSupportFragmentManager();

        currentFragment=new CreateAccountFragment();
        fm.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_right, R.anim.slide_out_left)
                    .replace(R.id.fragment_container, currentFragment, "Create Account")
                    .commit();


    }


    public void onReturnToActivity() {

        if(currentFragment instanceof CreateAccountFragment){
            currentFragment=new MyInfoFragment();
            fm.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_right, R.anim.slide_out_left)
                    .replace(R.id.fragment_container, currentFragment, "My Info")
                    .commit();
        }
        else if(currentFragment instanceof MyInfoFragment){
            Intent i = new Intent(this, BluetoothActivity.class);
            i.putExtra("fromSetup", "hi");
            startActivityForResult(i, bluetoothActivityRequestCode );

        }
        else if(currentFragment instanceof BottleEditorFragment){
            Intent i =new Intent(SetupActivity.this, MainActivity.class);
            i.putExtra("peripheralIdentifier",currentFragment.getArguments().getString("peripheralIdentifier"));
            startActivity(i, currentFragment.getArguments());
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == bluetoothActivityRequestCode){
            if(resultCode == RESULT_OK) {
                peripheralIdentifier = data.getStringExtra("peripheralIdentifier");
                currentFragment= BottleEditorFragment.newInstance(peripheralIdentifier);
                FragmentTransaction fragmentTransaction = fm.beginTransaction()
                        .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_right, R.anim.slide_out_left)
                        .replace(R.id.fragment_container, currentFragment, "Bottle Editor");
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            }


        }
    }






}




