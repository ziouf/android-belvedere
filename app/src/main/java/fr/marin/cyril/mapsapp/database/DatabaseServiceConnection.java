package fr.marin.cyril.mapsapp.database;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;

/**
 * Created by cscm6014 on 04/04/2016.
 */
public class DatabaseServiceConnection implements ServiceConnection {
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
