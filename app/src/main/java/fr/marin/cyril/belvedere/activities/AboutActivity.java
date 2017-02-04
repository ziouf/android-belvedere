package fr.marin.cyril.belvedere.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import fr.marin.cyril.belvedere.R;
import fr.marin.cyril.belvedere.database.RealmDbHelper;
import fr.marin.cyril.belvedere.model.Placemark;
import fr.marin.cyril.belvedere.model.PlacemarkType;

public class AboutActivity extends AppCompatActivity {

    private RealmDbHelper realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        this.realm = RealmDbHelper.getInstance();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        LinearLayout about_list_layout = (LinearLayout) findViewById(R.id.activity_about_list);
        about_list_layout.addView(inflateAboutItem("Dernière mise à jour des data", sharedPreferences.getString("last_bdd_update", "jamais")));
        about_list_layout.addView(inflateAboutItem("Nombre total de sommet", realm.countByType(PlacemarkType.PEAK.name(), Placemark.class).toString()));
        about_list_layout.addView(inflateAboutItem("Nombre total de massif", realm.countByType(PlacemarkType.MOUNTAIN.name(), Placemark.class).toString()));

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
