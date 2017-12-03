package fr.marin.cyril.belvedere.activities;

import android.Manifest;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

import fr.marin.cyril.belvedere.Preferences;
import fr.marin.cyril.belvedere.R;
import fr.marin.cyril.belvedere.async.DataInitTaskLoader;
import fr.marin.cyril.belvedere.fragments.CountrySelectDialogFragment;
import fr.marin.cyril.belvedere.model.Country;
import fr.marin.cyril.belvedere.tools.SparqlResponseJsonParser;
import io.realm.Realm;
import io.realm.Sort;

public class LoadingActivity extends Activity
        implements ActivityCompat.OnRequestPermissionsResultCallback, LoaderManager.LoaderCallbacks<Void> {

    private static final String TAG = LoadingActivity.class.getSimpleName();

    private static final int PERMISSIONS_CODE = new Random(0).nextInt(Short.MAX_VALUE);
    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    private static final int SYSTEM_UI_VISIBILITY = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_FULLSCREEN;

    private View decorView;

    private SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(SYSTEM_UI_VISIBILITY);

        this.pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        this.setContentView(R.layout.activity_loading);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

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
        Log.i(TAG + ".start()", "Init Loader");

        final Set<String> countrySet = pref.getStringSet(Preferences.COUNTRIES, Collections.emptySet());

        if (countrySet.isEmpty()) {
            try (final Realm realm = Realm.getDefaultInstance()) {
                if (realm.where(Country.class).count() == 0)
                    this.initCountries(realm);

                // Prepare fragment
                final List<Country> countries = realm.copyFromRealm(
                        realm.where(Country.class).findAllSorted("label", Sort.ASCENDING)
                );

                final Bundle bundle = new Bundle();
                bundle.putParcelableArrayList(Preferences.COUNTRIES, new ArrayList<>(countries));

                final CountrySelectDialogFragment newFragment = CountrySelectDialogFragment.newInstance();
                newFragment.setArguments(bundle);
                newFragment.setSharedPreferences(pref);
                newFragment.setOnClickOkListener((dialogInterface, i) -> initData());
                newFragment.setOnClickKoListener((dialogInterface, i) -> LoadingActivity.this.onLoadFinished(null, null));

                // Open Fragment
                newFragment.show(this.getFragmentManager(), CountrySelectDialogFragment.class.getSimpleName());
            }
        } else {
            this.initData();
        }
    }

    private void initData() {
        this.getLoaderManager().initLoader(0, null, this);

        if (this.shouldUpdateData()) {
            this.getLoaderManager().getLoader(0).forceLoad();
        } else {
            this.onLoadFinished(null, null);
        }
    }

    private void initCountries(Realm realm) {
        Log.i(TAG + ".initCountries()", "Start");
        final SparqlResponseJsonParser<Country> parser = new SparqlResponseJsonParser<>(Country.class);

        try (final InputStream in = getApplicationContext().getResources().openRawResource(R.raw.raw_countries)) {
            realm.beginTransaction();
            parser.parseJsonResponse(in, realm);
            realm.commitTransaction();
        } catch (Exception e) {
            realm.cancelTransaction();
            Log.e(TAG + ".loadInBackground()", e.getClass().getSimpleName() + " : " + e.getMessage());
            Log.d(TAG + ".loadInBackground()", e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        Log.i(TAG + ".initCountries()", "End (count : " + realm.where(Country.class).count() + ")");
    }

    /**
     * MAJ de la barre de progression
     *
     * @param args
     */
    private void onProgressUpdate(final Long... args) {
        final ProgressBar progressBar = findViewById(R.id.loading_progress);
        progressBar.setMax(args[1].intValue());
        progressBar.setProgress(args[0].intValue());
    }

    @Override
    public Loader<Void> onCreateLoader(int i, Bundle bundle) {
        Log.i(TAG + ".onCreateLoader()", "Loader id : " + i);
        return new DataInitTaskLoader(getApplicationContext(), this.pref, this::onProgressUpdate);
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
        final long last_update_long = pref.getLong(Preferences.LAST_UPDATE_DATE, 0);
        // Obtention de la fréquence de mise à jour des données
        final int update_frequency_days = pref.getInt(Preferences.UPDATE_FREQUENCY_DAYS, Preferences.UPDATE_FREQUENCY_DAYS_DEFAULT);

        final Calendar last_update_cal = Calendar.getInstance();
        last_update_cal.setTimeInMillis(last_update_long);
        final Calendar update_limit_cal = Calendar.getInstance();
        update_limit_cal.add(Calendar.DAY_OF_YEAR, -1 * update_frequency_days);

        if (last_update_long == 0
                || last_update_cal.before(update_limit_cal)) {
            Log.i(TAG, "Mise à jour des données nécessaire");
            return true;
        } else {
            Log.i(TAG, "Mise à jour des données inutile");
            return false;
        }
    }
}
