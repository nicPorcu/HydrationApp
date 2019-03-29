package com.example.per6.hydrationapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;


public class HydrationApp extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener{
    private static final String TAG = "HydrationApp";
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    private Context context;
    private Fragment currentFragment;
    private FragmentManager fm;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Backendless.initApp(this, BackendlessSettings.APP_ID, BackendlessSettings.API_KEY);





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
        fm= getSupportFragmentManager();
        currentFragment=new HomepageFragment();

        fm.beginTransaction()
                .replace(R.id.fragment_container, currentFragment)
                .commit();
        logIn();





    }


    private void logIn() {

        sharedPref = getSharedPreferences(
                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        int userExists = sharedPref.getInt(getString(R.string.user), 0);

        //checks if previous user exists
        if (userExists == 0) {
            Intent i = new Intent(this, LoginActivity.class);
            startActivity(i);
            Log.d(TAG, "logIn: ");
        }
        if (userExists == 1) {
            editor.clear();
            editor.putInt(getString(R.string.user), 0);
            editor.commit();
            Toast.makeText(this, "Next time you'll need to login again", Toast.LENGTH_SHORT).show();
            String name=sharedPref.getString("userUserName", "null");
            String password=sharedPref.getString("userPassword", "null");
            Log.d(TAG, "logIn: "+name+"   "+password);
            Backendless.UserService.login(name,password, new AsyncCallback<BackendlessUser>() {
                @Override
                public void handleResponse(BackendlessUser response) {
                    Toast.makeText(HydrationApp.this, "Hello"+ response.getProperty("name"), Toast.LENGTH_SHORT).show();
                    //works as Async
                }

                @Override
                public void handleFault(BackendlessFault fault) {
                    Log.d(TAG, "handleFault: "+ fault.getMessage());
                    Toast.makeText(HydrationApp.this, "failed to log in", Toast.LENGTH_SHORT).show();

                }
            });
        }

//        sharedPref = getSharedPreferences(
//                getString(R.string.preference_file_key), Context.MODE_PRIVATE);
//        SharedPreferences.Editor editor = sharedPref.edit();

//        int userExists = sharedPref.getInt(getString(R.string.user), 0);
////        userExists = 0; //todo delete me later
//        //checks if previous user exists
//        if (userExists == 0) {
//            Intent i = new Intent(this, LoginScreen.class);
//            startActivity(i);
//        }
//        if (userExists == 1) {
//            editor.clear();
//            editor.putInt(getString(R.string.user), 0);
//            editor.commit();
//            Toast.makeText(this, "Next time you'll need to login again", Toast.LENGTH_SHORT).show();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home_page, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            currentFragment=new HomepageFragment();

        } else if (id == R.id.nav_leaderboard) {
            currentFragment=new leaderboardFragment();

        } else if (id == R.id.nav_myInfo) {
            currentFragment=new UserInfoFragment();

        } else if (id == R.id.nav_customization) {
            currentFragment=new CustomizationFragment();

        } else if (id == R.id.nav_settings) {
            currentFragment=new SettingsFragment();

        }
        if(currentFragment != null){
            fm.beginTransaction()
                    .replace(R.id.fragment_container, currentFragment)
                    .commit();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
