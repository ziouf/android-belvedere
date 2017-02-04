package fr.marin.cyril.belvedere.database;

import java.io.Closeable;
import java.util.Collection;

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
        RealmResults<T> results = realm.where(clazz)
                .between("latitude", area.getBottom(), area.getTop())
                .between("longitude", area.getLeft(), area.getRight())
                .findAllSorted("elevation", Sort.DESCENDING)
                .distinct("id");

        int size = results.size() > 0 ? results.size() : 0;
        int limit = Math.min(size, Runtime.getRuntime().availableProcessors() * 30);

        return results.subList(0, limit);
    }

    public <T extends RealmModel> Collection<T> saveAll(final Collection<T> data) {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.copyToRealmOrUpdate(data);
            }
        });
        return data;
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
