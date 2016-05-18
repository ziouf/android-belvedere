package fr.marin.cyril.belvedere.tools;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by CSCM6014 on 18/05/2016.
 */
public class ConnectivityStatus {
    private final ConnectivityManager cm;

    public ConnectivityStatus(Context context) {
        this.cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public boolean canDownloadFromWeb() {
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }
}
