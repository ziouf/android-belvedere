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

    public static <T extends RealmModel> T findByFileName(Realm realm, String fileName, Class<T> clazz) {
        return realm.where(clazz)
                .equalTo("fileName", fileName)
                .findFirst();
    }

    public static <T extends RealmModel> T findByLatLng(Realm realm, LatLng latLng, Class<T> clazz) {
        return realm.where(clazz)
                .equalTo("latitude", latLng.latitude)
                .equalTo("longitude", latLng.longitude)
                .findFirst();
    }

    public static <T extends RealmModel> Collection<T> findInArea(Realm realm, Area area, Class<T> clazz) {
        RealmResults<T> results = realm.where(clazz)
                .between("latitude", area.getBottom(), area.getTop())
                .between("longitude", area.getLeft(), area.getRight())
                .findAllSorted("elevation", Sort.DESCENDING)
                .distinct("id");

        int size = results.size() > 0 ? results.size() : 0;
        int limit = Math.min(size, 50);

        return results.subList(0, limit);
    }

    public static <T extends RealmModel> void saveAll(Realm realm, final Collection<T> data) {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.copyToRealmOrUpdate(data);
            }
        });
    }

    public static <T extends RealmModel> void save(Realm realm, final T data) {
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm realm) {
                realm.copyToRealmOrUpdate(data);
            }
        });
    }
}
