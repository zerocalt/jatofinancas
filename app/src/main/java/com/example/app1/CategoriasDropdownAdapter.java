package com.example.app1;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class CategoriasDropdownAdapter extends ArrayAdapter<Categoria> {
    public CategoriasDropdownAdapter(@NonNull Context context, @NonNull List<Categoria> objects) {
        super(context, 0, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }
    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        return getCustomView(position, convertView, parent);
    }

    private View getCustomView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_categoria_dropdown, parent, false);

        Categoria categoria = getItem(position);
        TextView tvCircle = convertView.findViewById(R.id.tvCircle);
        TextView tvNome = convertView.findViewById(R.id.tvNomeCategoria);

        // Iniciais da categoria:
        String iniciais = "";
        if (categoria.nome != null && !categoria.nome.isEmpty()) {
            String[] partes = categoria.nome.trim().split("\\s+");
            for (String p : partes)
                if (!p.isEmpty())
                    iniciais += Character.toUpperCase(p.charAt(0));
        }
        tvCircle.setText(iniciais.length() > 2 ? iniciais.substring(0,2) : iniciais);

        // Cor:
        GradientDrawable bg = (GradientDrawable) tvCircle.getBackground();
        try {
            bg.setColor(android.graphics.Color.parseColor(categoria.cor));
        } catch(Exception e) {
            bg.setColor(0xFF888888); // cinza padrão
        }

        tvNome.setText(categoria.nome);

        return convertView;
    }
}