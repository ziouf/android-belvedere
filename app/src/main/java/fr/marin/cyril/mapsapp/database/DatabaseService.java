package fr.marin.cyril.mapsapp.database;

import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;

import com.google.android.gms.maps.model.LatLng;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

import fr.marin.cyril.mapsapp.R;
import fr.marin.cyril.mapsapp.kml.model.Placemark;
import fr.marin.cyril.mapsapp.kml.parser.KmlParser;
import fr.marin.cyril.mapsapp.tool.Area;

public class DatabaseService extends Service {

    private IBinder binder = new DatabaseServiceBinder();

    private DatabaseHelper dbHelper;
    private Collection<InputStream> kmlfiles;

    public DatabaseService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();

        this.dbHelper = new DatabaseHelper(getApplicationContext());

        this.kmlfiles = new HashSet<>();
        // Ajouter ci dessous la liste des fichiers de ressource
        this.kmlfiles.add(this.getResources().openRawResource(R.raw.sommets_des_alpes_francaises));
        this.kmlfiles.add(this.getResources().openRawResource(R.raw.sommets_des_pyrenees));
        this.kmlfiles.add(this.getResources().openRawResource(R.raw.sommets_du_massif_central));
        this.kmlfiles.add(this.getResources().openRawResource(R.raw.sommets_du_massif_des_vosges));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    public boolean isInit() {
        return this.dbHelper.count() > 0;
    }

    public void initDataIfNeeded() {
        if (!this.isInit()) this.dbHelper.insertAll(new KmlParser().parseAll(this.kmlfiles));
    }

    public Placemark findByLatLng(LatLng latLng) {
        return dbHelper.findByLatLng(latLng);
    }

    public Collection<Placemark> findInArea(Area area) {
        return dbHelper.findInArea(area);
    }

    public static class DatabaseServiceConnection implements ServiceConnection {
        boolean bound = false;
        DatabaseService databaseService;

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            DatabaseService.DatabaseServiceBinder binder = (DatabaseService.DatabaseServiceBinder) service;
            databaseService = binder.getService();
            bound = databaseService != null;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bound = false;
        }

        public boolean isBound() {
            return bound;
        }

        public DatabaseService getDatabaseService() {
            return databaseService;
        }
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class DatabaseServiceBinder extends Binder {
        public DatabaseService getService() {
            // Return this instance of LocalService so clients can call public methods
            return DatabaseService.this;
        }
    }
}
