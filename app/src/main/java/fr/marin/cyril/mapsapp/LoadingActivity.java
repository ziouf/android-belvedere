package fr.marin.cyril.mapsapp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.util.logging.Handler;

import fr.marin.cyril.mapsapp.database.DatabaseService;

public class LoadingActivity extends AppCompatActivity {

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection databaseServiceConnection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // remove title bar
        getWindow()
                .getDecorView()
                .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);

        setContentView(R.layout.activity_loading);

        this.databaseServiceConnection = new ServiceConnection() {
            private int delay = 3000;

            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                DatabaseService.DatabaseServiceBinder binder = (DatabaseService.DatabaseServiceBinder) service;

                binder.getService().initData();

                new android.os.Handler().postDelayed(startMapsActivity(), delay);
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        };

        // Bind database services
        this.bindService(new Intent(getApplicationContext(), DatabaseService.class),
                databaseServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        this.unbindService(databaseServiceConnection);
        super.onDestroy();
    }
    
    Runnable startMapsActivity() {
        return new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(LoadingActivity.this, MapsActivity.class));
                finish();
            }
        };
    }
}
