package fr.marin.cyril.mapsapp.activities.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

import fr.marin.cyril.mapsapp.R;
import fr.marin.cyril.mapsapp.database.DatabaseHelper;

public class LoadingActivity extends Activity
        implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int PERMISSIONS_CODE = 0;
    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
    };
    private static final int SYSTEM_UI_VISIBILITY = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

    private View decorView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(SYSTEM_UI_VISIBILITY);

        this.setContentView(R.layout.activity_loading);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Check permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Request LOCATION and CAMERA permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSIONS_CODE);
            }

        } else {
            this.start();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            decorView.setSystemUiVisibility(SYSTEM_UI_VISIBILITY);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_CODE && grantResults.length == PERMISSIONS.length) {
            if (grantResults[Arrays.asList(PERMISSIONS).indexOf(Manifest.permission.ACCESS_FINE_LOCATION)] != PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this, "La permission ACCESS_FINE_LOCATION est necessaire pour centrer la vue MAPS sur votre position", Toast.LENGTH_SHORT).show();

            if (grantResults[Arrays.asList(PERMISSIONS).indexOf(Manifest.permission.CAMERA)] != PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this, "La permission CAMERA est necessaire pour utiliser la fonction RA", Toast.LENGTH_SHORT).show();
        }

        this.start();

    }

    private void start() {
        (new DbInitAsyncTask()).execute();
    }

    private class DbInitAsyncTask extends AsyncTask<String, Integer, Boolean> {
        private TextView loadingInfoTextView;

        @Override
        protected void onPreExecute() {
            this.loadingInfoTextView = (TextView) LoadingActivity.this.findViewById(R.id.loading_info);
            this.loadingInfoTextView.setText(R.string.loading_database_init);
        }

        @Override
        protected Boolean doInBackground(String[] params) {
            DatabaseHelper db = new DatabaseHelper(getApplicationContext());
            db.initDataIfNeeded();
            return null;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            this.loadingInfoTextView.setText(R.string.loading_application_openning);
            LoadingActivity.this.startActivity(new Intent(LoadingActivity.this, MapsActivity.class));
            LoadingActivity.this.finish();
        }
    }
}
