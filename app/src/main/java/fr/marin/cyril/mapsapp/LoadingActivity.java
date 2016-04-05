package fr.marin.cyril.mapsapp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import fr.marin.cyril.mapsapp.database.DatabaseService;

public class LoadingActivity extends AppCompatActivity {

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection databaseServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            final DatabaseService.DatabaseServiceBinder binder = (DatabaseService.DatabaseServiceBinder) service;

            AsyncTask insertAll = new AsyncTask<Object, Void, Object>() {
                TextView loadingInfoTV = (TextView) findViewById(R.id.loading_info);
                @Override
                protected void onPreExecute() {
                    loadingInfoTV.setText(R.string.loading_database_init);
                }

                @Override
                protected void onPostExecute(Object value) {
                    loadingInfoTV.setText(R.string.loading_application_openning);

                    startActivity(new Intent(LoadingActivity.this, MapsActivity.class));
                    finish();
                }

                @Override
                protected String doInBackground(Object... params) {
                    binder.getService().initDataIfNeeded();
                    return null;
                }
            };

            insertAll.execute();

        }
        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_loading);

        // Passage en mode plein ecran immerssif
        getWindow().getDecorView()
                .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        // Bind database services
        bindService(new Intent(getApplicationContext(), DatabaseService.class),
                databaseServiceConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onDestroy() {
        this.unbindService(databaseServiceConnection);
        super.onDestroy();
    }

}
