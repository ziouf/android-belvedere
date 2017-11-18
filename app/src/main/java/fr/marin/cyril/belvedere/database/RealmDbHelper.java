package fr.marin.cyril.belvedere.database;

import java.io.Closeable;

import fr.marin.cyril.belvedere.model.Area;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmModel;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Created by marin on 28/01/2017.
 */

public class RealmDbHelper implements Closeable {

    private Realm realm;

    public static RealmDbHelper getInstance() {
        final RealmDbHelper dbHelper = new RealmDbHelper();
        dbHelper.realm = Realm.getDefaultInstance();
        return dbHelper;
    }

    public static RealmDbHelper getInstance(Realm realm) {
        final RealmDbHelper dbHelper = new RealmDbHelper();
        dbHelper.realm = realm;
        return dbHelper;
    }

    @Override
    public void close() {
        this.realm.close();
    }

    public void addChangeListener(RealmChangeListener<Realm> listener) {
        realm.addChangeListener(listener);
    }

    public <T extends RealmModel> T copyFromRealm(T data) {
        return realm.copyFromRealm(data);
    }

    public <T extends RealmModel> void findInArea(Area area,
                                                  Class<T> clazz,
                                                  RealmChangeListener<RealmResults<T>> listener) {

        final RealmResults<T> results = realm.where(clazz)
                .between("latitude", area.getBottom(), area.getTop())
                .between("longitude", area.getLeft(), area.getRight())
                .findAllSorted("elevation", Sort.DESCENDING);

        results.addChangeListener(listener);
    }
}
