package fr.marin.cyril.belvedere.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ListViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import fr.marin.cyril.belvedere.Preferences;
import fr.marin.cyril.belvedere.R;
import fr.marin.cyril.belvedere.model.Placemark;
import io.realm.Realm;

public class AboutActivity extends AppCompatActivity {
    private static final String TAG = AboutActivity.class.getSimpleName();
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        this.realm = Realm.getDefaultInstance();

        this.changeListener(realm);
        realm.addChangeListener(this::changeListener);
    }

    private void changeListener(Realm realm) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        long last_update = sharedPreferences.getLong(Preferences.LAST_UPDATE_DATE, 0);
        long update_frequency_days = sharedPreferences.getLong(Preferences.UPDATE_FREQUENCY_DAYS, Preferences.UPDATE_FREQUENCY_DAYS_DEFAULT);

        ArrayList<Pair<String, String>> about_items = new ArrayList<>();
        about_items.add(Pair.create(getString(R.string.last_data_update_date),
                last_update != 0 ? SIMPLE_DATE_FORMAT.format(new Date(last_update)) : getString(R.string.never)));
        about_items.add(Pair.create(getString(R.string.data_update_frequency),
                Long.toString(update_frequency_days)));
        about_items.add(Pair.create(getString(R.string.total_peak_count),
                Long.toString(realm.where(Placemark.class).count())));

        AboutActivity.this.updateData(about_items);
    }

    private void updateData(ArrayList<Pair<String, String>> about_items) {
        final AboutItemAdapter arrayAdapter = new AboutItemAdapter(this, R.layout.about_item, about_items);
        final ListViewCompat about_list_layout = findViewById(R.id.activity_about_list);
        about_list_layout.setAdapter(arrayAdapter);
    }

    @Override
    protected void onDestroy() {
        if (Objects.nonNull(this.realm))
            this.realm.close();
        super.onDestroy();
    }

    private class AboutItemAdapter extends ArrayAdapter<Pair<String, String>> {
        private AboutItemAdapter(Context context, int viewResourceId, Collection<Pair<String, String>> items) {
            super(context, viewResourceId, new ArrayList<>(items));
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                view = inflater.inflate(R.layout.about_item, null);
            }

            Pair<String, String> item = getItem(position);

            if (item == null) return view;

            TextView about_label = view.findViewById(R.id.about_label);
            TextView about_value = view.findViewById(R.id.about_value);

            about_label.setText(item.first);
            about_value.setText(item.second);

            return view;
        }
    }
}
