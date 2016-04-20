package fr.marin.cyril.mapsapp;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;

import fr.marin.cyril.mapsapp.database.DatabaseHelper;

public class LoadingActivity extends Activity
        implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int PERMISSIONS_CODE = 0;
    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
    };
    private DbInitAsyncTask dbInit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_loading);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        this.dbInit = new DbInitAsyncTask();

        // Check permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Request LOCATION and CAMERA permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSIONS_CODE);
            }

        } else {

            this.dbInit.execute();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();
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

        this.dbInit.execute();

    }

    private class DbInitAsyncTask extends AsyncTask {
        private DatabaseHelper db = new DatabaseHelper(LoadingActivity.this);
        private TextView loadingInfoTextView = (TextView) LoadingActivity.this.findViewById(R.id.loading_info);
        private ServiceConnection serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {

            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };

        @Override
        protected void onPreExecute() {
            this.loadingInfoTextView.setText(R.string.loading_database_init);

            LoadingActivity.this.bindService(new Intent(getApplicationContext(), SensorService.class),
                    serviceConnection, BIND_AUTO_CREATE);
        }

        @Override
        protected Object doInBackground(Object[] params) {
            this.db.initDataIfNeeded();
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            this.loadingInfoTextView.setText(R.string.loading_application_openning);

            LoadingActivity.this.startActivity(new Intent(LoadingActivity.this, MapsActivity.class));
            LoadingActivity.this.unbindService(serviceConnection);
            LoadingActivity.this.finish();
        }
    }
}
