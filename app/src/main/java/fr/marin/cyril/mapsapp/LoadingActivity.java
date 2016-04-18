package fr.marin.cyril.mapsapp;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import fr.marin.cyril.mapsapp.database.DatabaseHelper;

public class LoadingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // BugFix mode immersif pour api < 23
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            if (getSupportActionBar() != null) getSupportActionBar().hide();
        }

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Passage en mode plein ecran immerssif (api >= 23)
        getWindow().getDecorView()
                .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);
    }

    @Override
    protected void onResume() {
        super.onResume();

        new DbInitAsyncTask().execute();
    }

    private class DbInitAsyncTask extends AsyncTask {
        private DatabaseHelper db = new DatabaseHelper(LoadingActivity.this);
        private TextView loadingInfoTextView = (TextView) LoadingActivity.this.findViewById(R.id.loading_info);

        @Override
        protected void onPreExecute() {
            this.loadingInfoTextView.setText(R.string.loading_database_init);
        }

        @Override
        protected Object doInBackground(Object[] params) {
            this.db.initDataIfNeeded();
            return null;
        }

        @Override
        protected void onPostExecute(Object o) {
            this.loadingInfoTextView.setText(R.string.loading_application_openning);

            LoadingActivity.this.startActivity(new Intent(LoadingActivity.this, MapsActivity.class));
            LoadingActivity.this.finish();
        }
    }
}
