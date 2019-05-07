package com.group12.pickup.Model;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.group12.pickup.R;

public class WindowInfoAdapter implements GoogleMap.InfoWindowAdapter {

    private final View window;
    private Context context;

    public WindowInfoAdapter(Context context) {

        this.context = context;
        window = LayoutInflater.from(context).inflate(R.layout.info_window, null);
    }


    private void renderWindow(Marker marker) {

        String title = marker.getTitle();
        TextView textView = window.findViewById(R.id.title);

        if(!title.equals(""))
            textView.setText(title);

        String info = marker.getSnippet();
        TextView textViewInfo = window.findViewById(R.id.info);

        if(!info.equals(""))
            textViewInfo.setText(info);
    }


    @Override
    public View getInfoWindow(Marker marker) {

        renderWindow(marker);
        return window;
    }

    @Override
    public View getInfoContents(Marker marker) {

        renderWindow(marker);
        return window;
    }
}
