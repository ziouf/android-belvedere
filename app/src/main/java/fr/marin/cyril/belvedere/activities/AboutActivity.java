package fr.marin.cyril.belvedere.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import fr.marin.cyril.belvedere.Preferences;
import fr.marin.cyril.belvedere.R;
import fr.marin.cyril.belvedere.database.RealmDbHelper;
import fr.marin.cyril.belvedere.model.Placemark;
import fr.marin.cyril.belvedere.model.PlacemarkType;

public class AboutActivity extends AppCompatActivity {
    private static final String TAG = AboutActivity.class.getSimpleName();
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private RealmDbHelper realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        this.realm = RealmDbHelper.getInstance();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        long last_update = sharedPreferences.getLong(Preferences.LAST_UPDATE_DATE.name(), Preferences.LAST_UPDATE_DATE.defaultValue());
        long update_frequency_days = sharedPreferences.getLong(Preferences.UPDATE_FREQUENCY_DAYS.name(), Preferences.UPDATE_FREQUENCY_DAYS.defaultValue());

        LinearLayout about_list_layout = (LinearLayout) findViewById(R.id.activity_about_list);
        about_list_layout.addView(inflateAboutItem(getString(R.string.last_data_update_date), last_update != Preferences.LAST_UPDATE_DATE.defaultValue() ? SIMPLE_DATE_FORMAT.format(new Date(last_update)) : getString(R.string.never)));
        about_list_layout.addView(inflateAboutItem(getString(R.string.data_update_frequency), Long.valueOf(update_frequency_days).toString()));
        about_list_layout.addView(inflateAboutItem(getString(R.string.total_peak_count), realm.countByType(PlacemarkType.PEAK.name(), Placemark.class).toString()));
        about_list_layout.addView(inflateAboutItem(getString(R.string.total_mount_count), realm.countByType(PlacemarkType.MOUNTAIN.name(), Placemark.class).toString()));

    }


    private View inflateAboutItem(String label, String value) {
        View about_item = getLayoutInflater().inflate(R.layout.about_item, null);

        TextView label_tv = (TextView) about_item.findViewById(R.id.about_label);
        TextView value_tv = (TextView) about_item.findViewById(R.id.about_value);

        label_tv.setText(label);
        value_tv.setText(value);

        return about_item;
    }

    @Override
    protected void onDestroy() {
        this.realm.close();
        super.onDestroy();
    }
}
