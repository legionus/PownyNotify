package com.yandex.pownynotify;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class EventAdapter<T> extends ArrayAdapter<T> {
    private Context mContext;
    private int mResource;
    private List<T> mObjects;

    public EventAdapter(Context context, int resource,  List<T> objects) {
        super(context, resource, objects);

        mContext  = context;
        mResource = resource;
        mObjects  = objects;

        System.out.println("!!! EventAdapter");
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //System.out.println("!!! getView");

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View vi = inflater.inflate(mResource, null);

        final Event event = (Event) mObjects.get(position);

        TextView subject   = (TextView) vi.findViewById(R.id.EventSubject);
        TextView severity  = (TextView) vi.findViewById(R.id.EventSeverity);
        TextView eventTime = (TextView) vi.findViewById(R.id.EventTime);
        TextView evCount   = (TextView) vi.findViewById(R.id.EventCount);

        subject.setText(event.getSubject());
        eventTime.setText(event.getTime().toString());
        evCount.setText("");

        if (event.getCount() > 1) {
            evCount.setText("(" + event.getCount() + ")");
            evCount.setTextColor(mContext.getResources().getColor(R.color.info));
        }

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
