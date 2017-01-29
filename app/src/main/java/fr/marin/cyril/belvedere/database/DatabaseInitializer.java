package fr.marin.cyril.belvedere.database;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.AsyncTask;
import android.util.Log;

import java.util.Collection;

import fr.marin.cyril.belvedere.R;
import fr.marin.cyril.belvedere.dbpedia.JsonResponseParser;
import fr.marin.cyril.belvedere.model.DbFile;
import fr.marin.cyril.belvedere.model.Placemark;
import fr.marin.cyril.belvedere.tools.ConnectivityStatus;
import fr.marin.cyril.belvedere.tools.Utils;
import io.realm.Realm;

/**
 * Created by Cyril on 18/05/2016.
 */
public class DatabaseInitializer extends AsyncTask<Void, Integer, Void> {
    private static final String TAG = "InitDBTask";

    private final Context context;
    private final JsonResponseParser parser;
    private final ConnectivityStatus connectivityStatus;

    public DatabaseInitializer(Context context) {
        this.context = context;
        this.parser = new JsonResponseParser();
        this.connectivityStatus = new ConnectivityStatus(context);
    }

    @Override
    protected Void doInBackground(Void... params) {
        final Realm realm = Realm.getDefaultInstance();
        // Obtention des fichiers de resource
        final TypedArray ta = context.getResources().obtainTypedArray(R.array.sparql_json_array);

        for (int i = 0; i < ta.length(); ++i) {
            publishProgress(i, ta.length());

            final int id = ta.getResourceId(i, -1);
            final String key = ta.getString(i);
            final String hash = Utils.getSHA1FromResource(context, id);
            DbFile dbFile = RealmDbHelper.findByFileName(realm, key, DbFile.class);

            if (dbFile == null) {
                dbFile = new DbFile();
                dbFile.setFileName(key);
                dbFile.setHash(hash);
            }

            if (!dbFile.getHash().equals(hash)) {
                Log.i(TAG, String.format("Importation du fichier : %s (%s)", dbFile.getFileName(), dbFile.getHash()));
                final Collection<Placemark> placemarks = parser.readJsonStream(context.getResources().openRawResource(id));
                RealmDbHelper.saveAll(realm, placemarks);
                RealmDbHelper.save(realm, dbFile);
            }
        }
        publishProgress(ta.length(), ta.length());

        realm.close();
        ta.recycle();
        return null;
    }
}
