package fr.marin.cyril.belvedere.async;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import fr.marin.cyril.belvedere.async.interfaces.OnPostExecuteListener;
import fr.marin.cyril.belvedere.database.RealmDbHelper;
import fr.marin.cyril.belvedere.model.Area;
import fr.marin.cyril.belvedere.model.Placemark;
import io.realm.Realm;

/**
 * Created by cyril on 16/11/17.
 */

public class AreaQueryAsyncTask extends AsyncTask<Area, Void, Collection<Placemark>> {

    private OnPostExecuteListener<Collection<Placemark>> onPostExecuteListener;

    @Override
    protected Collection<Placemark> doInBackground(Area... areas) {
        final Collection<Placemark> results = new ArrayList<>();
        for (Area area : areas) {
            try (final Realm realm = Realm.getDefaultInstance()) {
                results.addAll(RealmDbHelper.getInstance(realm).findInArea(area, Placemark.class));
            }
        }
        return results;
    }

    @Override
    protected void onPostExecute(Collection<Placemark> placemarks) {
        if (Objects.nonNull(this.onPostExecuteListener))
            this.onPostExecuteListener.onPostExecute(placemarks);
    }

    public void setOnPostExecuteListener(OnPostExecuteListener<Collection<Placemark>> onPostExecuteListener) {
        this.onPostExecuteListener = onPostExecuteListener;
    }
}
