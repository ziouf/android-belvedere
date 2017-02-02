package fr.marin.cyril.belvedere.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import fr.marin.cyril.belvedere.R;
import fr.marin.cyril.belvedere.model.Placemark;
import io.realm.Realm;

/**
 * A simple {@link Fragment} subclass.
 * create an instance of this fragment.
 */
public class AboutFragment extends Fragment {

    private static AboutFragment singleton;
    private Realm realm;
    private View rootView;

    public AboutFragment() {
        // Required empty public constructor
    }

    public static AboutFragment getInstance() {
        if (singleton == null) singleton = new AboutFragment();
        return singleton;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.realm = Realm.getDefaultInstance();
    }

    @Override
    public void onDestroy() {
        this.realm.close();
        super.onDestroy();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        if (this.rootView == null)
            this.rootView = inflater.inflate(R.layout.fragment_about, container, false);

        TextView peak_count_value = (TextView) rootView.findViewById(R.id.about_peak_count_value);
        peak_count_value.setText(Long.toString(realm.where(Placemark.class).count()));

        this.initActionBar();

        return rootView;
    }

    private void initActionBar() {
        if (rootView == null) return;

        AppCompatActivity activity = (AppCompatActivity) getActivity();
        // Set a Toolbar to replace the ActionBar.
        activity.setSupportActionBar((Toolbar) rootView.findViewById(R.id.toolbar));

        // Configuration de l'Actionbar
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP | ActionBar.DISPLAY_USE_LOGO);
        }
    }


}
