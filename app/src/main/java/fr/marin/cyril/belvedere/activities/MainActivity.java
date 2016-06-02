package fr.marin.cyril.belvedere.activities;

import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import fr.marin.cyril.belvedere.R;
import fr.marin.cyril.belvedere.fragments.MapsFragment;
import fr.marin.cyril.belvedere.fragments.SettingsFragment;

/**
 * Created by cyril on 31/05/16.
 */
public class MainActivity extends AppCompatActivity {

    public static android.support.v4.app.FragmentManager fragmentManager;

    private Toolbar toolbar;
    private ActionBarDrawerToggle toggle;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set a Toolbar to replace the ActionBar.
        // Configuration de l'Actionbar
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_USE_LOGO);
            actionBar.setHomeButtonEnabled(true);
        }

        // Init Menu Drawer
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navigationView = (NavigationView) findViewById(R.id.navigation_view);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

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
                .add(R.id.main_activity_fragment_placeholder, new MapsFragment())
                .commit();

    }

    private boolean selectDrawerItem(MenuItem item) {
        // Create a new fragment and specify the fragment to show based on nav item clicked
        switch (item.getItemId()) {
            case R.id.menu_maps:
                popFragment(currentFragment);
                break;
            case R.id.menu_settings:
                currentFragment = addFragment(SettingsFragment.class);
                break;
        }

        // Close the navigation drawer
        drawerLayout.closeDrawers();

        return false;
    }

    private Fragment addFragment(Class fragmentClass) {
        Fragment fragment = null;
        try {
            fragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception ignore) {
        }

        fragmentManager.beginTransaction()
                .add(R.id.main_activity_fragment_placeholder, fragment)
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
