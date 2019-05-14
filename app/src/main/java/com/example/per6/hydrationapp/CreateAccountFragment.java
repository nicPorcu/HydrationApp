package com.example.per6.hydrationapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;

public class CreateAccountFragment extends Fragment {

    private static final String TAG = "CreateAccountFragment";
    private View rootView;
    private EditText usernameEdittext, passwordEdittext, emailEdittext;
    private Button submitButton;
    private SharedPreferences sharedPref;
    private Context context;



    public CreateAccountFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, //Use as onCreate
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.activity_create_account, container, false);
        wireWidgets();

        //dogChange(0.0);
        //imageDog.setImageResource(R.drawable.ic_menu_camera);
        return rootView;
    }

    private void wireWidgets() {
        Log.d(TAG, "wireWidgets: wire widgets");
        submitButton = (Button) rootView.findViewById(R.id.button_submit);
        usernameEdittext = (EditText) rootView.findViewById(R.id.editText_username);
        passwordEdittext = (EditText) rootView.findViewById(R.id.editText_password);
        context=getContext();
        emailEdittext = rootView.findViewById(R.id.editText_email);
        sharedPref = getActivity().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);





        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                BackendlessUser b = new BackendlessUser();
                b.setEmail(emailEdittext.getText().toString());
                b.setProperty("name", usernameEdittext.getText().toString());
                b.setPassword( passwordEdittext.getText().toString());

                Backendless.UserService.register(b, new AsyncCallback<BackendlessUser>() {
                    public void handleResponse(BackendlessUser registeredUser) {
                        Log.d(TAG, "handleResponse: userRegistered");
                        userLogin(registeredUser);
                        // user has been registered and now can login
                    }

                    public void handleFault(BackendlessFault fault) {
                        Log.d(TAG, "handleFault: " + fault.getMessage());
                        // an error has occurred, the error code can be retrieved with fault.getCode()
                    }
                });


            }

        });
    }
    private void userLogin(BackendlessUser registeredUser) {
        Backendless.UserService.login(registeredUser.getProperty("name").toString(), registeredUser.getProperty("password").toString(), new AsyncCallback<BackendlessUser>() {


            @Override
            public void handleResponse(BackendlessUser response) {
                Log.d(TAG, "handleResponse: handleResponse");
                String username = (String) response.getProperty("name");
                String password = (String) passwordEdittext.getText().toString();

                Log.d(TAG, "handleResponse: password "+password);
                Toast.makeText(getContext(), "Hello " +username, Toast.LENGTH_SHORT).show();
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

                ((SetupActivity)(getActivity())).onReturnToctivity();


            }

            @Override
            public void handleFault(BackendlessFault fault) {

            }
        });
    }


}
