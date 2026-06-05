package com.loginactivity.ftdriver;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Menu;
import io.sentry.Sentry;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.loginactivity.ftdriver.LoginModule.GetLoginOTPActivity;
import com.loginactivity.ftdriver.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private SharedPreferences sharedPreferences;
    private String LoginUserID, LoginUserName;
    private static final String TAG = "MainActivity";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        try {
            throw new Exception("Test Sentry Exception - FTDriver App");
        } catch (Exception e) {
            Sentry.captureException(e);
        }

        sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);

        if (sharedPreferences.getBoolean("isLoggedIn", false)) {
            LoginUserID = sharedPreferences.getString("UserId", "");
            LoginUserName = sharedPreferences.getString("Name", "");

            Log.v(TAG, "User Details - User ID: " + LoginUserID + ", Name: " + LoginUserName);
        }

        Log.v( TAG,"Main Activity : SharedPreferences UserID = " + LoginUserID);
        if (LoginUserID == null || LoginUserID.isEmpty()) {
            Intent intent = new Intent(this, GetLoginOTPActivity.class);
            startActivity(intent);
            return;
        }



        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
               R.id.nav_f_t_driver,R.id.nav_upload_p_o_d)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(MainActivity.this, GetLoginOTPActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }
}