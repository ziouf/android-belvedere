package fr.marin.cyril.belvedere.tools;

import android.widget.Filter;

import fr.marin.cyril.belvedere.model.Placemark;
import io.realm.Case;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Created by cyril on 09/12/17.
 */

public abstract class PlacemarkTitleFilter extends Filter {
    @Override
    protected FilterResults performFiltering(CharSequence constraint) {
        final FilterResults results = new FilterResults();

        if (Objects.isNull(constraint) || constraint.length() < 2) {
            results.count = 0;
            results.values = null;
        } else {
            try (Realm realm = Realm.getDefaultInstance()) {
                final RealmResults<Placemark> queryResults = realm.where(Placemark.class)
                        .contains("title", constraint.toString(), Case.INSENSITIVE)
                        .findAllSorted("title", Sort.ASCENDING);

                results.values = realm.copyFromRealm(queryResults);
                results.count = queryResults.size();
            }
        }

        return results;
    }
}
