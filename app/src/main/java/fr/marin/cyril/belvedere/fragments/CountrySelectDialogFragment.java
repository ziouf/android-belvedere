package fr.marin.cyril.belvedere.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.marin.cyril.belvedere.Preferences;
import fr.marin.cyril.belvedere.R;
import fr.marin.cyril.belvedere.model.Country;

/**
 * Created by cyril on 16/11/17.
 */

public class CountrySelectDialogFragment extends DialogFragment {

    private final Set<Country> selectedCountries = new HashSet<>();
    private List<Country> countries = new ArrayList<>();
    private DialogInterface.OnClickListener onClickOkListener;
    private DialogInterface.OnClickListener onClickKoListener;
    private SharedPreferences sharedPreferences;

    public static CountrySelectDialogFragment newInstance() {
        return new CountrySelectDialogFragment();
    }

    public void setSharedPreferences(SharedPreferences pref) {
        this.sharedPreferences = pref;
    }

    @Override
    public void setArguments(Bundle args) {
        if (args.containsKey(Preferences.COUNTRIES))
            countries = args.getParcelableArrayList(Preferences.COUNTRIES);
    }

    public void setOnClickOkListener(DialogInterface.OnClickListener onClickOkListener) {
        this.onClickOkListener = onClickOkListener;
    }

    public void setOnClickKoListener(DialogInterface.OnClickListener onClickKoListener) {
        this.onClickKoListener = onClickKoListener;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.AppTheme)
                .setTitle("Please choose your country")
                .setPositiveButton(R.string.ok, this::onPositiveButtonClick)
                .setNegativeButton(R.string.cancel, this::onNegativeButtonClick)
                .setNeutralButton(R.string.select_all, this::onNeutralButtonClick)
                .setMultiChoiceItems(Stream.of(countries).map(Country::getLabel).collect(Collectors.toList()).toArray(new String[0]),
                        null, this::onMultiChoiceClickListener);

        return builder.create();
    }

    private void onMultiChoiceClickListener(DialogInterface dialogInterface, int i, boolean checked) {
        if (checked)
            selectedCountries.add(countries.get(i));
        else
            selectedCountries.remove(countries.get(i));
    }

    private void onNeutralButtonClick(DialogInterface dialogInterface, int i) {
        selectedCountries.addAll(countries);

        final Activity a = getActivity();

        new AlertDialog.Builder(a, R.style.AppTheme)
                .setTitle("Warning")
                .setMessage("You have selected all countries. Loading data may run for quite a long time and consume data. Do you want to proceed anyway ?")
                .setNegativeButton(R.string.cancel, (dialog, i1) -> {
                    a.startActivity(new Intent(a, a.getClass()));
                    a.finish();
                })
                .setPositiveButton(R.string.ok, this::onPositiveButtonClick)
                .create()
                .show();
    }

    private void onPositiveButtonClick(DialogInterface dialogInterface, int i) {
        sharedPreferences.edit().putStringSet(
                Preferences.COUNTRIES,
                Stream.of(selectedCountries).map(Country::getId).collect(Collectors.toSet())
        ).apply();

        this.onClickOkListener.onClick(dialogInterface, i);
    }

    private void onNegativeButtonClick(DialogInterface dialogInterface, int i) {

        this.onClickKoListener.onClick(dialogInterface, i);
    }
}
