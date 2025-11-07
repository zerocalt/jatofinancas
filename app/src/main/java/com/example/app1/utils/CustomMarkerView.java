package com.example.app1.utils;

import android.content.Context;
import android.widget.TextView;

import com.example.app1.R;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import java.text.NumberFormat;
import java.util.Locale;

public class CustomMarkerView extends MarkerView {

    private TextView tvContent;

    public CustomMarkerView(Context context, int layoutResource) {
        super(context, layoutResource);
        tvContent = findViewById(R.id.tvContent);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        String text;
        if (e instanceof PieEntry) {
            PieEntry pieEntry = (PieEntry) e;
            text = pieEntry.getLabel() + ": " + formatCurrency(pieEntry.getValue());
        } else {
            text = formatCurrency(e.getY());
        }
        tvContent.setText(text);
        super.refreshContent(e, highlight);
    }

    private String formatCurrency(float value) {
        return NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(value);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -getHeight());
    }
}
