package com.vulsoft.vulsoftos;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class SmartWidgetManager {

    public static View createAtAGlanceWidget(Context context) {
        View v = LayoutInflater.from(context).inflate(R.layout.layout_widget_at_a_glance, null);
        updateWidget(v);
        return v;
    }

    public static String getSmartGreeting() {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour < 12)
            return "Bonjour";
        if (hour < 18)
            return "Bon après-midi";
        return "Bonsoir";
    }

    public static void updateWidget(View root) {
        TextView txtGreeting = root.findViewById(R.id.txtWidgetGreeting);
        
        if (txtGreeting != null) {
            txtGreeting.setText(getSmartGreeting());
        }
    }
}
