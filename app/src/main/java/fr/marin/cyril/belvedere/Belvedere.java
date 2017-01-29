package fr.marin.cyril.belvedere;

import io.realm.Realm;

/**
 * Created by marin on 29/01/2017.
 */

public class Belvedere extends android.support.multidex.MultiDexApplication {
    @Override
    public void onCreate() {
        super.onCreate();

        Realm.init(this.getApplicationContext());
    }
}
