package fr.marin.cyril.belvedere.database;

import com.google.android.gms.maps.model.LatLng;

import java.util.Collection;

import fr.marin.cyril.belvedere.model.Area;
import io.realm.Realm;
import io.realm.RealmModel;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Created by marin on 28/01/2017.
 */

public class RealmDbHelper {

    private final Realm realm;

    private RealmDbHelper() {
        this.realm = Realm.getDefaultInstance();
    }

    public static RealmDbHelper getInstance() {
        return new RealmDbHelper();
    }

    public <T extends RealmModel> T findById(Long id, Class<T> clazz) {
        return realm.where(clazz)
                .equalTo("id", id)
                .findFirst();
    }

    public <T extends RealmModel> T findByLatLng(LatLng latLng, Class<T> clazz) {
        return realm.where(clazz)
                .equalTo("latitude", latLng.latitude)
                .equalTo("longitude", latLng.longitude)
                .findFirst();
    }

    public <T extends RealmModel> Collection<T> findInArea(Area area, Class<T> clazz) {
        RealmResults<T> results = realm.where(clazz)
                .between("latitude", area.getBottom(), area.getTop())
                .between("longitude", area.getLeft(), area.getRight())
                .findAllSorted("elevation", Sort.DESCENDING);

        int size = results.size() > 0 ? results.size() : 0;
        int limit = Math.min(size, 50);

        return results.subList(0, limit);
    }

    public <T extends RealmModel> void insertAll(Collection<T> data) {
        realm.beginTransaction();
        realm.copyToRealm(data);
        realm.commitTransaction();
    }

    public <T extends RealmModel> void insert(T data) {
        realm.beginTransaction();
        realm.copyToRealm(data);
        realm.commitTransaction();
    }

    public <T extends RealmModel> T update(T data) {
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(data);
        realm.commitTransaction();
        return data;
    }

    public <T extends RealmModel> Collection<T> update(Collection<T> data) {
        realm.beginTransaction();
        realm.copyToRealmOrUpdate(data);
        realm.commitTransaction();
        return data;
    }
}
