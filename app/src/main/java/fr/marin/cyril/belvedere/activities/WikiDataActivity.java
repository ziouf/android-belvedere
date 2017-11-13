package fr.marin.cyril.belvedere.activities;

import android.support.v7.app.AppCompatActivity;

import fr.marin.cyril.belvedere.async.DataGetterAsync;

/**
 * Created by cyril on 06/11/17.
 */

public class WikiDataActivity extends AppCompatActivity {

    @Override
    protected void onResume() {
        super.onResume();

        new DataGetterAsync().execute();

    }

}
