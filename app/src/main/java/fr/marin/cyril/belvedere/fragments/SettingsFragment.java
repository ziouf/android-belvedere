package fr.marin.cyril.belvedere.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import fr.marin.cyril.belvedere.R;

/**
 * Created by cyril on 31/05/16.
 */
public class SettingsFragment extends Fragment {
    private static final String TAG = "SettingsFragment";
    private static View view;

    private AppCompatActivity activity;
    private ActionBar actionBar;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null)
                parent.removeView(view);
        }

        try {
            view = inflater.inflate(R.layout.fragment_settings, container, false);
        } catch (InflateException e) {
            Log.w(TAG, "Error when inflating UI", e);
        }

        activity = (AppCompatActivity) getActivity();
        // Set a Toolbar to replace the ActionBar.
        activity.setSupportActionBar((Toolbar) view.findViewById(R.id.toolbar));

        // Configuration de l'Actionbar
        actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getString(R.string.menu_settings));
            actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_USE_LOGO | ActionBar.DISPLAY_SHOW_TITLE);
        }

        return view;
    }

}
