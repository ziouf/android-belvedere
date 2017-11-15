package fr.marin.cyril.belvedere;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

/**
 * Created by marin on 29/01/2017.
 */

public class Belvedere extends android.support.multidex.MultiDexApplication {
    private static final Long REALM_SCHEMA_VERSION = 1L;
    private static final int MAX_IDLE_CNX = 25;
    private static final int KEEP_ALIVE_SECONDS = 5;
    private static final int HTTP_TIMEOUT_SECONDS = 120;

    private static OkHttpClient httpClientSingleton;

    public static OkHttpClient getHttpClient() {
        if (Objects.isNull(httpClientSingleton)) httpClientSingleton = new OkHttpClient.Builder()
                .connectTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(HTTP_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .retryOnConnectionFailure(true)
                .connectionPool(new ConnectionPool(MAX_IDLE_CNX, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS))
                .build();

        return httpClientSingleton;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Realm.init(this.getApplicationContext());

        RealmConfiguration realmConfiguration = new RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .schemaVersion(REALM_SCHEMA_VERSION)
                .build();

        Realm.setDefaultConfiguration(realmConfiguration);

    }
}
