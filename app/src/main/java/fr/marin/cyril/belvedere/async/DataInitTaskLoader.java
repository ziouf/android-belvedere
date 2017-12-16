package fr.marin.cyril.belvedere.async;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

import fr.marin.cyril.belvedere.Belvedere;
import fr.marin.cyril.belvedere.Preferences;
import fr.marin.cyril.belvedere.async.interfaces.OnProgressListener;
import fr.marin.cyril.belvedere.datasource.WikiDataQueryManager;
import fr.marin.cyril.belvedere.model.Country;
import fr.marin.cyril.belvedere.model.Placemark;
import fr.marin.cyril.belvedere.tools.Objects;
import fr.marin.cyril.belvedere.tools.SparqlResponseJsonParser;
import io.realm.Realm;
import io.realm.RealmQuery;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Created by cyril on 15/11/17.
 */

public class DataInitTaskLoader extends AsyncTaskLoader<Void> {
    private static final String TAG = DataInitTaskLoader.class.getSimpleName();

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

    private final SharedPreferences pref;
    private final ConnectivityManager cm;

    private final OnProgressListener<Long> onProgressListener;

    public DataInitTaskLoader(Context context, SharedPreferences pref, OnProgressListener<Long> progressListener) {
        super(context);
        this.pref = pref;
        this.onProgressListener = progressListener;
        this.cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    private static HttpUrl wikidataHttpUrl() {
        return new HttpUrl.Builder().scheme("https").host("query.wikidata.org").addPathSegment("sparql").build();
    }

    private void onProgress(Long... args) {
        if (Objects.nonNull(this.onProgressListener))
            this.onProgressListener.onProgress(args);
    }

    @Override
    public Void loadInBackground() {
        if (!this.isNetworkOk()) return null;

        try (final Realm realm = Realm.getDefaultInstance()) {
            this.initMountainsBySelectedCountries(realm);
        }

        return null;
    }

    private boolean isNetworkOk() {
        final NetworkInfo netInfo = cm.getActiveNetworkInfo();

        if (Objects.isNull(netInfo)) return false;
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

    private void initMountainsBySelectedCountries(Realm realm) {
        final RealmQuery<Country> selected_countries = realm.where(Country.class)
                .in("id", pref.getStringSet(Preferences.COUNTRIES, Collections.emptySet()).toArray(new String[0]));

        // init Placemarks
        long cur = 0, max = selected_countries.count();
        if (max > 0) {
            for (Country c : selected_countries.findAll()) {
                this.onProgress(++cur, max);
                this.initMountainsByCountry(c, realm);
            }
        }

        // set last update date
        pref.edit().putLong(Preferences.LAST_UPDATE_DATE, new Date().getTime()).apply();
    }

    private void initMountainsByCountry(Country country, Realm realm) {
        Log.i(TAG, "Start (" + country + ")");
        final SparqlResponseJsonParser<Placemark> parser = new SparqlResponseJsonParser<>(Placemark.class);
        final String query = WikiDataQueryManager.WikiDataQueries.GET_ALL_MOUTAINS_IN_COUNTRY.getQuery(country.getId());

        final Request request = new Request.Builder()
                .header("Accept", "application/sparql-results+json")
                .url(wikidataHttpUrl().newBuilder().addQueryParameter("query", query).build())
                .get()
                .build();

        try (final Response response = Belvedere.getHttpClient().newCall(request).execute()) {
            if (response.isSuccessful()) {
                final ResponseBody body = response.body();
                if (Objects.nonNull(body)) {
                    realm.beginTransaction();
                    parser.parseJsonResponse(body.byteStream(), realm);
                    realm.commitTransaction();
                }
            }
        } catch (Exception e) {
            realm.cancelTransaction();
            Log.e(TAG, e.getClass().getSimpleName() + " : " + e.getMessage());
            Log.d(TAG, e.getClass().getSimpleName() + " : " + e.getMessage(), e);
        }
        Log.i(TAG, "End (count : " + realm.where(Placemark.class).count() + ")");
    }
}
