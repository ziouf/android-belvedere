package fr.marin.cyril.belvedere.activities;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.MenuItem;

import fr.marin.cyril.belvedere.R;
import fr.marin.cyril.belvedere.activities.old.CompassActivity;
import fr.marin.cyril.belvedere.fragments.MapsFragment;

/**
 * Created by cyril on 31/05/16.
 */
public class MainActivity extends CompassActivity {

    public static FragmentManager fragmentManager;

    private ActionBarDrawerToggle toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Init Menu Drawer
        DrawerLayout drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Fragment manager
        fragmentManager = getFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        MapsFragment mapsFragment = new MapsFragment();
        transaction.add(R.id.main_activity_fragment_placeholder, mapsFragment);
        transaction.commit();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (toggle.onOptionsItemSelected(item))
            return true;

        return super.onOptionsItemSelected(item);
    }
}
