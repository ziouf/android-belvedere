package fr.marin.cyril.belvedere.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.UUID;

import fr.marin.cyril.belvedere.Preferences;
import fr.marin.cyril.belvedere.R;
import fr.marin.cyril.belvedere.async.DbpediaDataGetterAsync;
import fr.marin.cyril.belvedere.dbpedia.QueryManager;
import fr.marin.cyril.belvedere.model.Placemark;
import fr.marin.cyril.belvedere.model.PlacemarkType;
import io.realm.Realm;

public class LoadingActivity extends Activity
        implements ActivityCompat.OnRequestPermissionsResultCallback {
    private static final String TAG = LoadingActivity.class.getSimpleName();

    private static final int PERMISSIONS_CODE = UUID.randomUUID().hashCode();
    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
    };
    private static final int SYSTEM_UI_VISIBILITY = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_FULLSCREEN;

    private static final Collection<Integer> NET_TYPES = Arrays.asList(
            ConnectivityManager.TYPE_MOBILE,
            ConnectivityManager.TYPE_MOBILE_DUN,
            ConnectivityManager.TYPE_WIMAX,
            ConnectivityManager.TYPE_ETHERNET
    );
    private static final Collection<Integer> NET_SUB_TYPES = Arrays.asList(
            TelephonyManager.NETWORK_TYPE_HSDPA,
            TelephonyManager.NETWORK_TYPE_HSPA,
            TelephonyManager.NETWORK_TYPE_HSPAP,
            TelephonyManager.NETWORK_TYPE_HSUPA,
            TelephonyManager.NETWORK_TYPE_LTE
    );

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
        // Test de la connectivité réseau du terminal
        if (!this.isNetworkOk()) return;

        if (this.shouldUpdateData()) {
            // Initialisation du jeu de données
            final DbpediaDataGetterAsync async = DbpediaDataGetterAsync.getInstance(getApplicationContext());
            async.setOnPostExecuteListener(new DbpediaDataGetterAsync.OnPostExecuteListener() {
                @Override
                public void onPostExecute() {
                    LoadingActivity.this.startMainActivity();
                }
            });
            async.execute(
                    DbpediaDataGetterAsync.Param.of(QueryManager.MOUNTAINS_QUERY, PlacemarkType.MOUNTAIN),
                    DbpediaDataGetterAsync.Param.of(QueryManager.PEAKS_QUERY, PlacemarkType.PEAK)
            );
        } else {
            // Ouverture de l'activité principale
            LoadingActivity.this.startMainActivity();
        }
    }

    private boolean shouldUpdateData() {

        // Si la base est vide alors => true
        if (realm.where(Placemark.class).count() == 0)
            return true;

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        // Obtention de la date de la dernière mise à jour
        long last_update_long = preferences.getLong(Preferences.LAST_UPDATE_DATE.name(), Preferences.LAST_UPDATE_DATE.defaultValue());
        // Obtention de la fréquence de mise à jour des données
        long update_frequency_days = preferences.getLong(Preferences.UPDATE_FREQUENCY_DAYS.name(), Preferences.UPDATE_FREQUENCY_DAYS.defaultValue());
        int update_frequency_days_int = Long.valueOf(update_frequency_days).intValue();

        Calendar last_update_cal = Calendar.getInstance();
        last_update_cal.setTimeInMillis(last_update_long);
        Calendar update_limit_cal = Calendar.getInstance();
        update_limit_cal.add(Calendar.DAY_OF_YEAR, -1 * update_frequency_days_int);

        if (last_update_long == Preferences.LAST_UPDATE_DATE.defaultValue()
                || last_update_cal.before(update_limit_cal)) {
            Log.i(TAG, "Mise à jour des données nécessaire");
            return true;
        } else {
            Log.i(TAG, "Mise à jour des données inutile");
            return false;
        }
    }

    private boolean isNetworkOk() {
        final NetworkInfo netInfo = cm.getActiveNetworkInfo();

        if (netInfo == null) return false;
        if (!netInfo.isConnected()) return false;

        if (netInfo.getType() == ConnectivityManager.TYPE_WIFI)
            return true;

        if (NET_TYPES.contains(netInfo.getType()) && NET_SUB_TYPES.contains(netInfo.getSubtype()))
            return true;

        if (Build.VERSION.PREVIEW_SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            if (netInfo.getType() == ConnectivityManager.TYPE_VPN)
                return true;

        return false;
    }

    private void startMainActivity() {
        LoadingActivity.this.startActivity(new Intent(LoadingActivity.this, MainActivity.class));
        LoadingActivity.this.finish();
    }
}
