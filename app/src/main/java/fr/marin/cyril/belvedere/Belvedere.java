package fr.marin.cyril.belvedere;

import io.realm.Realm;
import io.realm.RealmConfiguration;

/**
 * Created by marin on 29/01/2017.
 */

public class Belvedere extends android.support.multidex.MultiDexApplication {
    private static final Long REALM_SCHEMA_VERSION = 1L;

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
