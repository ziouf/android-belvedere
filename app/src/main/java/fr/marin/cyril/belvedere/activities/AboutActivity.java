package fr.marin.cyril.belvedere.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import fr.marin.cyril.belvedere.R;
import fr.marin.cyril.belvedere.model.Placemark;
import fr.marin.cyril.belvedere.model.PlacemarkType;
import io.realm.Realm;

public class AboutActivity extends AppCompatActivity {

    private Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        this.realm = Realm.getDefaultInstance();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        TextView last_update = (TextView) findViewById(R.id.about_last_update_value);
        last_update.setText(sharedPreferences.getString("last_bdd_update", "jamais"));
        TextView peak_count = (TextView) findViewById(R.id.about_peak_count_value);
        peak_count.setText(Long.toString(realm.where(Placemark.class).equalTo("type", PlacemarkType.PEAK.name()).count()));
        TextView mount_count = (TextView) findViewById(R.id.about_mount_count_value);
        mount_count.setText(Long.toString(realm.where(Placemark.class).equalTo("type", PlacemarkType.MOUNTAIN.name()).count()));
    }

    @Override
    protected void onDestroy() {
        this.realm.close();
        super.onDestroy();
    }
}
