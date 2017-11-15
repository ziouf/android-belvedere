package fr.marin.cyril.belvedere.activities;

import android.Manifest;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
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
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Random;

import fr.marin.cyril.belvedere.Preferences;
import fr.marin.cyril.belvedere.R;
import fr.marin.cyril.belvedere.async.DataInitLoader;

// TODO : Affichage de la liste des Pays dans une popup si la liste des pays est vide dans les SharedPreferences
public class LoadingActivity extends Activity
        implements ActivityCompat.OnRequestPermissionsResultCallback, LoaderManager.LoaderCallbacks<Void> {

    private static final String TAG = LoadingActivity.class.getSimpleName();

    private static final int PERMISSIONS_CODE = new Random(0).nextInt(Integer.MAX_VALUE);
    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION
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

        this.cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        // Check permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request LOCATION permissions
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSIONS_CODE);

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
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_CODE && grantResults.length == PERMISSIONS.length) {
            if (grantResults[Arrays.asList(PERMISSIONS).indexOf(Manifest.permission.ACCESS_FINE_LOCATION)] != PackageManager.PERMISSION_GRANTED)
                Toast.makeText(this, "La permission ACCESS_FINE_LOCATION est necessaire pour centrer la vue MAPS sur votre position", Toast.LENGTH_SHORT).show();
        }

        this.start();
    }

    private void start() {
        // TODO : Déplacer le test de connectivité dans la AsyncTaskLoader
        // Test de la connectivité réseau du terminal
        if (!this.isNetworkOk()) return;

        Log.i(TAG + ".start()", "Init Loader");
        if (this.getLoaderManager().getLoader(0) == null)
            this.getLoaderManager().initLoader(0, null, this);
        else
            this.getLoaderManager().restartLoader(0, null, this);

        if (this.shouldUpdateData()) {
            this.getLoaderManager().getLoader(0).forceLoad();
        } else {
            this.onLoadFinished(null, null);
        }
    }

    // TODO : Ajouter un listener dans la AsyncTaskLoader pour mettre à jour la barre de progression

    /**
     * MAJ de la barre de progression
     *
     * @param values
     */
    private void onProgressUpdate(final Integer... values) {
        final ProgressBar progressBar = findViewById(R.id.loading_progress);
        progressBar.setMax(values[1]);
        progressBar.setProgress(values[0]);
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

    @Override
    public Loader<Void> onCreateLoader(int i, Bundle bundle) {
        Log.i(TAG + ".onCreateLoader()", "Loader id : " + i);
        return new DataInitLoader(getApplicationContext());
    }

    @Override
    public void onLoadFinished(Loader<Void> loader, Void d) {
        if (loader != null)
            Log.i(TAG + ".onLoaderFinished()", "Loader id : " + loader.getId());

        LoadingActivity.this.startActivity(new Intent(LoadingActivity.this, MainActivity.class));
        LoadingActivity.this.finish();
    }

    @Override
    public void onLoaderReset(Loader<Void> loader) {
        Log.i(TAG + ".onLoaderReset()", "Loader id : " + loader.getId());
    }

    private boolean shouldUpdateData() {
        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        // Obtention de la date de la dernière mise à jour
        final long last_update_long = pref.getLong(Preferences.LAST_UPDATE_DATE.name(), Preferences.LAST_UPDATE_DATE.defaultValue());
        // Obtention de la fréquence de mise à jour des données
        final int update_frequency_days = pref.getInt(Preferences.UPDATE_FREQUENCY_DAYS.name(), (int) Preferences.UPDATE_FREQUENCY_DAYS.defaultValue());

        final Calendar last_update_cal = Calendar.getInstance();
        last_update_cal.setTimeInMillis(last_update_long);
        final Calendar update_limit_cal = Calendar.getInstance();
        update_limit_cal.add(Calendar.DAY_OF_YEAR, -1 * update_frequency_days);

        if (last_update_long == Preferences.LAST_UPDATE_DATE.defaultValue()
                || last_update_cal.before(update_limit_cal)) {
            Log.i(TAG, "Mise à jour des données nécessaire");
            return true;
        } else {
            Log.i(TAG, "Mise à jour des données inutile");
            return false;
        }
    }
}
