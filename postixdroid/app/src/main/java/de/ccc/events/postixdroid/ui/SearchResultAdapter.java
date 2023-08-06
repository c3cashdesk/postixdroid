package de.ccc.events.postixdroid.ui;


import android.content.Context;
import androidx.core.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import de.ccc.events.postixdroid.R;
import de.ccc.events.postixdroid.check.TicketCheckProvider;

public class SearchResultAdapter extends ArrayAdapter<TicketCheckProvider.SearchResult> {

    private Context context;
    private int resource;

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(resource, parent, false);
        } else {
            view = convertView;
        }
        TicketCheckProvider.SearchResult item = getItem(position);

        TextView tvSecret = (TextView) view.findViewById(R.id.tvSecret);
        TextView tvOrderCode = (TextView) view.findViewById(R.id.tvOrderCode);
        TextView tvStatus = (TextView) view.findViewById(R.id.tvStatus);
        TextView tvAttendeeName = (TextView) view.findViewById(R.id.tvAttendeeName);
        TextView tvTicketName = (TextView) view.findViewById(R.id.tvTicketName);
        View rlResult = view.findViewById(R.id.rlResult);

        tvSecret.setText(item.getSecret());
        tvOrderCode.setText(item.getOrderCode());
        tvTicketName.setText(item.getTicket());
        if (item.isRedeemed()) {
            tvStatus.setText(R.string.status_redeemed);
            rlResult.setBackgroundColor(ContextCompat.getColor(context, R.color.scan_result_warn));
        } else if (!item.isPaid()) {
            tvStatus.setText(R.string.status_unpaid);
            rlResult.setBackgroundColor(ContextCompat.getColor(context, R.color.scan_result_err));
        } else {
            tvStatus.setText(R.string.status_valid);
            rlResult.setBackgroundColor(ContextCompat.getColor(context, R.color.scan_result_ok));
        }

        return view;
    }

    public SearchResultAdapter(Context context, int resource,
                               int textViewResourceId, List<TicketCheckProvider.SearchResult> objects) {
        super(context, resource, textViewResourceId, objects);
        this.context = context;
        this.resource = resource;
    }

}
