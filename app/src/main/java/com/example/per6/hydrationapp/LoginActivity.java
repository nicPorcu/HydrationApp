package com.example.per6.hydrationapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private Button submitButton;
    private EditText usernameEdittext, passwordEdittext;
    private SharedPreferences sharedPref;
    private TextView googleSigninTextview;
    private Context context;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        context=this;
        sharedPref = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        wireWidgets();

    }

    private void wireWidgets() {
        Log.d(TAG, "wireWidgets: wire widgets");
        submitButton=(Button)findViewById(R.id.button_submit);
        usernameEdittext=(EditText) findViewById(R.id.login_edittext_username);
        passwordEdittext=(EditText) findViewById(R.id.login_edittext_password);
        googleSigninTextview=(TextView) findViewById(R.id.textview_google_signin);
        googleSigninTextview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //googleSignIn();
            }
        });
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Backendless.UserService.login(usernameEdittext.getText().toString(), passwordEdittext.getText().toString(), new AsyncCallback<BackendlessUser>() {
                    @Override
                    public void handleResponse(BackendlessUser response) {
                        Log.d(TAG, "handleResponse: handleResponse");
                        String username = (String) response.getProperty("name");
                        String password = (String) passwordEdittext.getText().toString();

                        Log.d(TAG, "handleResponse: password "+password);
                        Toast.makeText(LoginActivity.this, "Hello " +username, Toast.LENGTH_SHORT).show();
                        //save info
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString(getString(R.string.user_ID), response.getUserId());
                        Log.d(TAG, "handleResponse: "+username);
                        editor.putString("userUserName", username);
                        editor.putString( getString(R.string.password), password);
                        editor.putInt(getString(R.string.user), 1);
                        editor.apply();
                        String check = sharedPref.getString("userUserName", null);
                        Log.d(TAG, "handleResponse: "+check);
                        //start main activity
                        Intent i = new Intent(context, MainActivity.class);
                        startActivity(i);
                }

                    @Override
                    public void handleFault(BackendlessFault fault) {
                        Log.d(TAG, "handleFault: "+fault.getMessage());
                    }

            });
        }

    });



    }

//    public void googleSignIn(){
//        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                .requestEmail()
//                .build();
//
//    }
}
