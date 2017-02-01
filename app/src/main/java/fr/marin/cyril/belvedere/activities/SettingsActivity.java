package fr.marin.cyril.belvedere.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import fr.marin.cyril.belvedere.R;
import fr.marin.cyril.belvedere.model.Placemark;
import io.realm.Realm;

public class SettingsActivity extends AppCompatActivity {

    private Realm realm;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        this.realm = Realm.getDefaultInstance();
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        Long peak_count = realm.where(Placemark.class).count();
        TextView peak_count_tv = (TextView) findViewById(R.id.peak_count_value);
        peak_count_tv.setText(peak_count.toString());



    }

    @Override
    protected void onDestroy() {
        this.realm.close();
        super.onDestroy();
    }
}
