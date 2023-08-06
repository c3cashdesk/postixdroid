package de.ccc.events.postixdroid.ui;

import android.content.Context;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import de.ccc.events.postixdroid.AppConfig;
import de.ccc.events.postixdroid.R;
import de.ccc.events.postixdroid.net.api.PostixApi;

public class AutoCompleteAdapter extends ArrayAdapter<ListConstraintEntry> implements Filterable {
    private ArrayList<ListConstraintEntry> mData;
    private int listId;

    public AutoCompleteAdapter(Context context, int listId) {
        super(context, R.layout.item_listentry);
        mData = new ArrayList<>();
        this.listId = listId;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public ListConstraintEntry getItem(int index) {
        return mData.get(index);
    }

    @NonNull
    @Override
    public View getView(int position, View view, @NonNull ViewGroup parent) {

        if (view == null) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_listentry, parent, false);
        }

        ListConstraintEntry e = getItem(position);
        ((TextView) view.findViewById(R.id.tvName)).setText(e.getName());
        ((TextView) view.findViewById(R.id.tvIdentifier)).setText(e.getId());
        return view;
    }

    @Override
    public Filter getFilter() {
        Filter myFilter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults = new FilterResults();
                if(constraint != null) {
                    PostixApi api = PostixApi.fromConfig(new AppConfig(getContext()));
                    try {
                        JSONObject res = api.listEntries(listId, constraint.toString());
                        JSONArray arr = res.getJSONArray("results");
                        mData.clear();
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject r = arr.getJSONObject(i);
                            mData.add(new ListConstraintEntry(
                                    r.getString("identifier"),
                                    r.getString("name")
                            ));
                        }
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                    // Now assign the values and count to the FilterResults object
                    filterResults.values = mData;
                    filterResults.count = mData.size();
                }
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence contraint, FilterResults results) {
                if(results != null && results.count > 0) {
                    notifyDataSetChanged();
                }
                else {
                    notifyDataSetInvalidated();
                }
            }
        };
        return myFilter;
    }
}