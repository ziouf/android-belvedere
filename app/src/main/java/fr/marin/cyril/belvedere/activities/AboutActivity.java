package fr.marin.cyril.belvedere.activities;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import fr.marin.cyril.belvedere.R;
import fr.marin.cyril.belvedere.model.Placemark;
import io.realm.Realm;

public class AboutActivity extends AppCompatActivity {

    private Realm realm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        this.realm = Realm.getDefaultInstance();

        TextView peak_count = (TextView) findViewById(R.id.about_peak_count_value);
        peak_count.setText(Long.toString(realm.where(Placemark.class).count()));
    }

    @Override
    protected void onDestroy() {
        this.realm.close();
        super.onDestroy();
    }
}
