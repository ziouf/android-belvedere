package fr.marin.cyril.belvedere.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Toast;

import java.util.Arrays;

import fr.marin.cyril.belvedere.R;
import fr.marin.cyril.belvedere.services.UpdateDataService;
import io.realm.Realm;

public class LoadingActivity extends Activity
        implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int PERMISSIONS_CODE = 0;
    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
    };
    private static final int SYSTEM_UI_VISIBILITY = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_FULLSCREEN;

    private View decorView;
    private ConnectivityManager cm;

    private Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(SYSTEM_UI_VISIBILITY);
        realm = Realm.getDefaultInstance();

        this.setContentView(R.layout.activity_loading);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

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
    protected void onDestroy() {
        realm.close();
        super.onDestroy();
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
        /*
        final InitTask initDBTask = new InitTask(this);
        initDBTask.execute();
        */
        final NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            if (netInfo.getType() == ConnectivityManager.TYPE_WIFI
                    || (netInfo.getType() == ConnectivityManager.TYPE_MOBILE && netInfo.getSubtype() == TelephonyManager.NETWORK_TYPE_LTE))
            this.startService(new Intent(LoadingActivity.this, UpdateDataService.class));
        }

        this.startActivity(new Intent(LoadingActivity.this, MainActivity.class));
        this.finish();
    }

    /*
    private class InitTask extends DbInitializer {
        private final TextView loadingInfoTextView = (TextView) LoadingActivity.this.findViewById(R.id.loading_info);
        private final ProgressBar progressBar = (ProgressBar) LoadingActivity.this.findViewById(R.id.loading_progress);

        public InitTask(Context context) {
            super(context);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            this.loadingInfoTextView.setText(R.string.loading_database_init);
        }

        @Override
        protected void onPostExecute(Void v) {
            super.onPostExecute(v);
            this.loadingInfoTextView.setText(R.string.loading_application_openning);
            LoadingActivity.this.startActivity(new Intent(LoadingActivity.this, MainActivity.class));
            LoadingActivity.this.finish();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            this.progressBar.setProgress(values[0]);
            this.progressBar.setMax(values[1]);
        }
    }
    */
}
