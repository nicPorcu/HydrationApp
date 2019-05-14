package com.example.per6.hydrationapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;

// based on: https://www.bignerdranch.com/blog/splash-screens-the-right-way/
public class SplashActivity extends AppCompatActivity {


    private static final String TAG = "SplashActivity";
    private Context context;
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Backendless.initApp(this, BackendlessSettings.APP_ID, BackendlessSettings.API_KEY);


        context = this;
        sharedPref = context.getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        int userExists = sharedPref.getInt(getString(R.string.user), 0);
        //checks if previous user exists
        if (userExists == 0) {
            Log.d(TAG, "onCreate: launching login");
            Intent i = new Intent(this, LoginActivity.class);
            startActivity(i);
            //goes and logs in
        } else if (userExists == 1) {
            String name = sharedPref.getString("userUserName", null);
            String password = sharedPref.getString(getString(R.string.password), null);
            Log.d(TAG, "onCreate: Welcome " + name+ password);

            if (name != null && password != null) {
                Backendless.UserService.login(name, password, new AsyncCallback<BackendlessUser>() {
                    @Override
                    public void handleResponse(BackendlessUser response) {
                        Toast.makeText(context, "Hello" + response.getProperty("name"), Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "handleResponse: logged in");
                        
                        //works as Async
                    }

                    @Override
                    public void handleFault(BackendlessFault fault) {
                        Log.d(TAG, "handleFault: " + fault.getMessage());
                        Toast.makeText(context, "Failed to log in", Toast.LENGTH_SHORT).show();

                    }
                });
            } else {
                editor.putInt(getString(R.string.user), 0);
                editor.commit();
                Intent i = new Intent(this, LoginActivity.class);
                startActivity(i);
            }
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
