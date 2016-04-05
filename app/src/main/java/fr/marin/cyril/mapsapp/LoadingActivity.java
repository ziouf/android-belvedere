package fr.marin.cyril.mapsapp;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import fr.marin.cyril.mapsapp.database.DatabaseService;

public class LoadingActivity extends AppCompatActivity {

    /** Defines callbacks for service binding, passed to bindService() */
    private final ServiceConnection databaseServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            final DatabaseService.DatabaseServiceBinder binder = (DatabaseService.DatabaseServiceBinder) service;
            final AsyncTask databaseInitAsyncTask = new AsyncTask<Object, Void, Void>() {
                private TextView loadingInfoTextView = (TextView) LoadingActivity.this.findViewById(R.id.loading_info);

                @Override
                protected void onPreExecute() {
                    loadingInfoTextView.setText(R.string.loading_database_init);
                }

                @Override
                protected Void doInBackground(Object... params) {
                    binder.getService().initDataIfNeeded();
                    return null;
                }

                @Override
                protected void onPostExecute(Void value) {
                    loadingInfoTextView.setText(R.string.loading_application_openning);

                    LoadingActivity.this.startActivity(new Intent(LoadingActivity.this, MapsActivity.class));
                    LoadingActivity.this.finish();
                }
            };

            databaseInitAsyncTask.execute();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // BugFix mode immersif pour api < 23
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            getSupportActionBar().hide();
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        // Passage en mode plein ecran immerssif
        getWindow().getDecorView()
                .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);


        setContentView(R.layout.activity_loading);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        // Bind database services
        this.bindService(new Intent(getApplicationContext(), DatabaseService.class),
                this.databaseServiceConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onDestroy() {
        this.unbindService(databaseServiceConnection);
        super.onDestroy();
    }

}
