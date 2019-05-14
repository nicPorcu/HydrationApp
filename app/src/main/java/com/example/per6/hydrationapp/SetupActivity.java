package com.example.per6.hydrationapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
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


    public void onReturnToctivity() {
        Log.d(TAG, "onBackPressed: "+getFragmentManager().getBackStackEntryCount());

        if(currentFragment instanceof CreateAccountFragment){
            currentFragment=new MyInfoFragment();
            fm.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right, R.anim.slide_in_right, R.anim.slide_out_left)
                    .replace(R.id.fragment_container, currentFragment, "My Info")
                    .commit();
        }
        if(currentFragment instanceof MyInfoFragment){
            Intent i= new Intent(SetupActivity.this, MainActivity.class);
            startActivity(i);
        }
    }






}




