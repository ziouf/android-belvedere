package fr.marin.cyril.belvedere.async;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import fr.marin.cyril.belvedere.Preferences;
import fr.marin.cyril.belvedere.R;
import fr.marin.cyril.belvedere.datasource.WikiDataQueryManager;
import fr.marin.cyril.belvedere.model.Country;
import fr.marin.cyril.belvedere.model.Placemark;
import fr.marin.cyril.belvedere.tools.SparqlResponseJsonParser;
import io.realm.Realm;
import io.realm.RealmQuery;
import okhttp3.ConnectionPool;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by cyril on 15/11/17.
 */

public class DataInitLoader extends AsyncTaskLoader<Void> {
    private static final String TAG = DataInitLoader.class.getSimpleName();
    private static final int MAX_IDLE_CNX = 25;
    private static final int KEEP_ALIVE_SECONDS = 5;
    private static final int HTTP_TIMEOUT_SECONDS = 120;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .connectionPool(new ConnectionPool(MAX_IDLE_CNX, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS))
            .build();

    private final Resources res;
    private final SharedPreferences pref;
    private final String[] country_ids;

    private long max = 0;
    private long cur = 0;

    public DataInitLoader(Context context) {
        super(context);
        this.res = context.getResources();
        this.pref = PreferenceManager.getDefaultSharedPreferences(context);
        this.country_ids = pref.getStringSet(
                Preferences.COUNTRIES.name(),
//                Collections.singleton("http://www.wikidata.org/entity/Q142")
                Collections.emptySet()
        ).toArray(new String[0]);
    }

    private static HttpUrl wikidataHttpUrl() {
        return new HttpUrl.Builder().scheme("https").host("query.wikidata.org").addPathSegment("sparql").build();
    }

    @Override
    protected void onStartLoading() {
        final boolean shouldUpdate = this.shouldUpdateData();
        Log.i(TAG + ".onStartLoading()", "Shoudl update : " + shouldUpdate);
        if (shouldUpdate) {
            Log.i(TAG + ".onStartLoading()", "Force load");
            this.forceLoad();
        } else {
            Log.i(TAG + ".onStartLoading()", "Abandon");
            this.deliverResult(null);
        }
    }

    @Override
    protected void onAbandon() {
        super.onAbandon();
        this.cancelLoad();
    }

    @Override
    protected void onStopLoading() {
        super.onStopLoading();
        this.cancelLoad();
    }

    @Override
    public void onCanceled(Void data) {
        super.onCanceled(data);
    }

    @Override
    protected void onReset() {
        super.onReset();
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
    }

    @Override
    public Void loadInBackground() {
        try (final Realm realm = Realm.getDefaultInstance()) {
            // init Countries
            if (realm.where(Country.class).count() == 0) this.initCountries(realm);

            if (country_ids.length > 0) {
                final RealmQuery<Country> selected_countries = realm.where(Country.class)
                        .in("id", country_ids);

                // init Placemarks
                if (selected_countries.count() > 0) {
                    this.max = selected_countries.count();
                    for (Country c : selected_countries.findAll()) {
                        this.cur++;
                        this.initMountainsByCountry(c, realm);
                    }
                }

                // set last update date
                pref.edit()
                        .putLong(Preferences.LAST_UPDATE_DATE.name(), new Date().getTime())
                        .apply();
            } else {
                Log.i(TAG + ".loadInBackground()", "No country selected");
            }

        }
        return null;
    }

    private void initCountries(Realm realm) {
        Log.i(TAG + ".initCountries()", "Start");
        final SparqlResponseJsonParser<Country> parser = new SparqlResponseJsonParser<>(Country.class);

        try (final InputStream in = res.openRawResource(R.raw.raw_countries)) {
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

    private void initMountainsByCountry(Country country, Realm realm) {
        Log.i(TAG + ".initMountainsByCountry()", "Start (" + country + ")");
        final SparqlResponseJsonParser<Placemark> parser = new SparqlResponseJsonParser<>(Placemark.class);
        final String query = WikiDataQueryManager.WikiDataQueries.GET_ALL_MOUTAINS_IN_COUNTRY.getQuery(country.getId());

        final Request request = new Request.Builder()
                .header("Accept", "application/sparql-results+json")
                .url(wikidataHttpUrl().newBuilder().addQueryParameter("query", query).build())
                .get()
                .build();

        try (final Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                realm.beginTransaction();
                parser.parseJsonResponse(response.body().byteStream(), realm);
                realm.commitTransaction();
            }
        } catch (Exception e) {
            realm.cancelTransaction();
            Log.e(TAG + ".loadInBackground()", e.getClass().getSimpleName() + " : " + e.getMessage());
            Log.d(TAG + ".loadInBackground()", e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        Log.i(TAG + ".initMountainsByCountry()", "End (count : " + realm.where(Placemark.class).count() + ")");
    }

    private boolean shouldUpdateData() {
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
