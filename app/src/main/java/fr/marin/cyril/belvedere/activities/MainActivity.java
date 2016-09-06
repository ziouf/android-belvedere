package fr.marin.cyril.belvedere.activities;

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

import fr.marin.cyril.belvedere.R;
import fr.marin.cyril.belvedere.fragments.MapsFragment;
import fr.marin.cyril.belvedere.fragments.SettingsFragment;
import fr.marin.cyril.belvedere.sparql.DbPediaQueryManager;

/**
 * Created by cyril on 31/05/16.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    public static FragmentManager fragmentManager;

    private ActionBarDrawerToggle toggle;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Init Menu Drawer
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationView = (NavigationView) findViewById(R.id.navigation_view);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(toggle);

        // Init
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
                popFragment(currentFragment);
                break;
            case R.id.menu_refresh:
                // TODO : Add call to refresh data from dbpedia.org
                AsyncTask<Void, Void, Void> at = new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void[] params) {
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
                                Log.i(TAG, "TOTO !! -- br create");
                                StringBuilder sb = new StringBuilder();
                                String buffer;
                                while ((buffer = reader.readLine()) != null)
                                    sb.append(buffer);

                                Log.i(TAG, sb.toString());
                            } finally {
                                conn.disconnect();
                            }

                        } catch (IOException e) {
                            Log.e(TAG, e.getMessage());
                        }
                        return null;
                    }
                };
                at.execute();

                break;
            case R.id.menu_settings:
                popFragment(currentFragment);
                currentFragment = addFragment(SettingsFragment.class);
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

        if (toggle.onOptionsItemSelected(item))
            return true;

        return super.onOptionsItemSelected(item);
    }


}
