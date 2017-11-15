package fr.marin.cyril.belvedere.database;

import java.io.Closeable;
import java.util.Collection;

import fr.marin.cyril.belvedere.Preferences;
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

    public <T extends RealmModel> Collection<T> findInArea(Area area, Class<T> clazz) {
        final RealmResults<T> results = realm.where(clazz)
                .between("latitude", area.getBottom(), area.getTop())
                .between("longitude", area.getLeft(), area.getRight())
                .findAllSorted("elevation", Sort.DESCENDING);

        int size = results.size() > 0 ? results.size() : 0;
        int limit = Math.min(size, Preferences.MAX_ON_MAP);

        return realm.copyFromRealm(results.subList(0, limit));
    }

    public <T extends RealmModel> T save(final T data) {
        realm.beginTransaction();
        final T saved = realm.copyToRealmOrUpdate(data);
        realm.commitTransaction();
        return saved;
    }

    public <T extends RealmModel> Collection<T> saveAll(final Collection<T> data) {
        realm.beginTransaction();
        final Collection<T> saved = realm.copyToRealmOrUpdate(data);
        realm.commitTransaction();
        return saved;
    }

    public <T extends RealmModel> Long count(Class<T> clazz) {
        return realm.where(clazz).count();
    }

    public <T extends RealmModel> Long countByType(String type, Class<T> clazz) {
        return realm.where(clazz)
                .equalTo("type", type)
                .count();
    }
}
