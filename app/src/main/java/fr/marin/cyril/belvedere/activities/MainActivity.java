package fr.marin.cyril.belvedere.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import fr.marin.cyril.belvedere.R;
import fr.marin.cyril.belvedere.fragments.MapsFragment;
import fr.marin.cyril.belvedere.tools.Objects;

/**
 * Created by cyril on 31/05/16.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static FragmentManager fragmentManager;

    private ActionBarDrawerToggle toggle;
    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Init Menu Drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, findViewById(R.id.toolbar), R.string.open, R.string.close);
        drawerLayout.addDrawerListener(toggle);

        // Init
        final NavigationView navigationView = findViewById(R.id.navigation_view);
        if (Objects.nonNull(navigationView)) {
            navigationView.setNavigationItemSelectedListener(
                    this::selectDrawerItem
            );
        }

        // Fragment manager
        fragmentManager = getSupportFragmentManager();

        // Fragment par défaut
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_placeholder, MapsFragment.getInstance(), MapsFragment.class.getSimpleName())
                .commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.menu, menu);
//        // Retrieve the SearchView and plug it into SearchManager
//        final SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
//        final SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
//        if (Objects.nonNull(searchManager))
//            searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        return true;
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
                fragmentManager.popBackStack();
                break;
            case R.id.menu_info:
                startActivity(new Intent(this, AboutActivity.class));
                break;
            case R.id.menu_settings:
                break;
        }

        // Close the navigation drawer
        drawerLayout.closeDrawers();

        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return toggle.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }
}
