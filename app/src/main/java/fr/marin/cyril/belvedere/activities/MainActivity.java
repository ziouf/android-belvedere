package fr.marin.cyril.belvedere.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import fr.marin.cyril.belvedere.R;
import fr.marin.cyril.belvedere.activities.settings.SettingsActivity;
import fr.marin.cyril.belvedere.database.DatabaseHelper;
import fr.marin.cyril.belvedere.datasources.DbPediaQueryManager;
import fr.marin.cyril.belvedere.fragments.MapsFragment;
import fr.marin.cyril.belvedere.model.Placemark;
import fr.marin.cyril.belvedere.parser.DbPediaJsonResponseParser;

/**
 * Created by cyril on 31/05/16.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public static FragmentManager fragmentManager;

    private ActionBarDrawerToggle toggle;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Init Menu Drawer
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        final NavigationView navigationView = (NavigationView) findViewById(R.id.navigation_view);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(toggle);

        // Init
        assert navigationView != null;
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem item) {
                        return selectDrawerItem(item);
                    }
                }
        );

        // Fragment manager
        fragmentManager = getSupportFragmentManager();

        // Fragment par défaut
        fragmentManager.beginTransaction()
                .add(R.id.fragment_placeholder, new MapsFragment())
                .commit();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        toggle.syncState();
    }

    private boolean selectDrawerItem(MenuItem item) {
        // Create a new fragment and specify the fragment to show based on nav item clicked
        switch (item.getItemId()) {
            case R.id.menu_maps:
                break;
            case R.id.menu_refresh:
                // TODO : Add call to refresh data from dbpedia.org
                final AsyncTask<Void, Void, String> at = new AsyncTask<Void, Void, String>() {
                    @Override
                    protected String doInBackground(Void[] params) {
                        Log.i(TAG, "TOTO !! -- doInBackground");
                        try {
                            String url = DbPediaQueryManager.buildApiUrl(DbPediaQueryManager.FRENCH_PEAKS_QUERY);
                            Log.i(TAG, "TOTO !! -- url : " + url);
                            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                            conn.setRequestMethod("GET");
                            conn.setDoInput(true);
                            conn.connect();

                            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                                Log.i(TAG, "TOTO !! -- " + conn.getResponseCode());
                                final StringBuilder sb = new StringBuilder();
                                String buffer;
                                while ((buffer = reader.readLine()) != null)
                                    sb.append(buffer);

                                return sb.toString();
                            } finally {
                                conn.disconnect();
                            }

                        } catch (IOException e) {
                            Log.e(TAG, e.getMessage());
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(String result) {
                        Log.i(TAG, result);
                        final DbPediaJsonResponseParser parser = new DbPediaJsonResponseParser();
                        final List<Placemark> placemarks = parser.readJsonString(result);

                        Log.i(TAG, "Downloaded " + placemarks.size() + " placemarks");

                        for (Placemark p : placemarks) {
                            DatabaseHelper.getInstance(getApplicationContext()).insertOrUpdatePlacemark(p);
                        }

                        Log.i(TAG, "Finish");
                    }
                };
                at.execute();

                break;
            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
        }

        // Close the navigation drawer
        drawerLayout.closeDrawers();

        return false;
    }

    private Fragment addFragment(Class fragmentClass) {
        Fragment fragment;
        try {
            fragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            Log.e(TAG, "Erreur lors de l'instanciation du fragment", e);
            return null;
        }

        if (fragment != null)
            fragmentManager.beginTransaction()
                    .add(R.id.fragment_placeholder, fragment)
                .commit();

        return fragment;
    }

    private void popFragment(Fragment fragment) {
        if (fragment != null)
            fragmentManager.beginTransaction()
                    .detach(fragment).remove(fragment)
                    .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return toggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }


}
