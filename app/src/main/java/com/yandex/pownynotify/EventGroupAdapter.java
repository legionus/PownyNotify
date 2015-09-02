package com.yandex.pownynotify;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class EventGroupAdapter<T> extends ArrayAdapter<T> {
    private Context mContext;
    private int mResource;
    private List<T> mObjects;

    public EventGroupAdapter(Context context, int resource, List<T> objects) {
        super(context, resource, objects);

        mContext  = context;
        mResource = resource;
        mObjects  = objects;

        System.out.println("!!! EventGroupAdapter");
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View vi = inflater.inflate(mResource, null);

        final Event event = (Event) mObjects.get(position);

        TextView sample    = (TextView) vi.findViewById(R.id.EventGroupSample);
        TextView severity  = (TextView) vi.findViewById(R.id.EventGroupSeverity);
        TextView eventTime = (TextView) vi.findViewById(R.id.EventGroupTime);

        if (event.getBody().isEmpty()) {
            sample.setText(mContext.getString(R.string.no_message));
            sample.setTextColor(mContext.getResources().getColor(R.color.disable));
        } else {
            sample.setText(event.getBody());
        }

        eventTime.setText(event.getTime().toString());

        switch (event.getSeverity()) {
            case "CRIT":
                severity.setBackgroundResource(R.color.danger);
                break;
            case "WARN":
                severity.setBackgroundResource(R.color.warning);
                break;
            case "OK":
                severity.setBackgroundResource(R.color.success);
                break;
        }
        return vi;
    }
}
