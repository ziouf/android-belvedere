package fr.marin.cyril.belvedere.tools;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import java.util.List;

import fr.marin.cyril.belvedere.model.Placemark;

/**
 * Created by cyril on 09/12/17.
 */

public class PlacemarkSearchAdapter extends ArrayAdapter<Placemark> implements Filterable {

    private final int mFieldId;
    private final LayoutInflater mInflater;

    public PlacemarkSearchAdapter(Context ctx) {
        super(ctx, android.R.layout.simple_dropdown_item_1line);
        this.mFieldId = android.R.layout.simple_dropdown_item_1line;
        this.mInflater = LayoutInflater.from(ctx);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View view = Objects.isNull(convertView) ? mInflater.inflate(mFieldId, parent, false) : convertView;
        final TextView textView = (TextView) view;

        final Placemark placemark = this.getItem(position);
        textView.setText(placemark.getTitle());

        return view;
    }

    @Override
    public Filter getFilter() {
        return new PlacemarkTitleFilter() {
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if (results.count > 0) {
                    PlacemarkSearchAdapter.this.clear();
                    PlacemarkSearchAdapter.this.addAll((List<Placemark>) results.values);
                }
            }
        };
    }
}
