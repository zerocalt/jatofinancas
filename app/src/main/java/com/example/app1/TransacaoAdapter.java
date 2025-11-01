package com.example.app1;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class TransacaoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;

    private final List<Object> items;
    private final OnTransacaoListener listener;

    public interface OnTransacaoListener {
        void onTransacaoClick(TelaTransacoes.TransacaoItem item);
    }

    public static class HeaderData {
        public String title;
        public Double total;

        public HeaderData(String title, double total) {
            this.title = title;
            this.total = total;
        }

        public HeaderData(String title) {
            this.title = title;
            this.total = null;
        }
    }

    public TransacaoAdapter(Context context, List<Object> items, OnTransacaoListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof HeaderData) {
            return TYPE_HEADER;
        }
        return TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            return new HeaderViewHolder(inflater.inflate(R.layout.item_transacao_header, parent, false));
        }
        return new ItemViewHolder(inflater.inflate(R.layout.item_transacao, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == TYPE_HEADER) {
            ((HeaderViewHolder) holder).bind((HeaderData) items.get(position));
        } else {
            ((ItemViewHolder) holder).bind((TelaTransacoes.TransacaoItem) items.get(position), listener);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView title, total;
        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.header_title);
            total = itemView.findViewById(R.id.header_total);
        }

        public void bind(HeaderData header) {
            title.setText(header.title);
            if (header.total != null) { // Section Header
                total.setVisibility(View.VISIBLE);
                total.setText(NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(header.total));
                if (header.title.equals("Receitas")) {
                    total.setTextColor(Color.parseColor("#006400"));
                } else {
                    total.setTextColor(Color.parseColor("#E45757"));
                }
            } else { // Date Header
                total.setVisibility(View.GONE);
            }
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        TextView titulo, categoria, valor, inicial, tipoLabel;
        ImageView statusIndicator;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            titulo = itemView.findViewById(R.id.tituloTransacao);
            categoria = itemView.findViewById(R.id.tituloCategoria);
            valor = itemView.findViewById(R.id.valorTransacao);
            inicial = itemView.findViewById(R.id.txtIconCategoria);
            statusIndicator = itemView.findViewById(R.id.statusIndicator);
            tipoLabel = itemView.findViewById(R.id.labelTipoTransacao);
        }

        public void bind(final TelaTransacoes.TransacaoItem item, final OnTransacaoListener listener) {
            titulo.setText(item.descricao);
            categoria.setText(item.categoriaNome != null ? item.categoriaNome : "Sem Categoria");
            valor.setText(NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(item.valor));

            if (item.categoriaNome != null && !item.categoriaNome.isEmpty()) {
                inicial.setText(item.categoriaNome.substring(0, 1));
            } else {
                inicial.setText("?");
            }

            if (inicial.getBackground() instanceof GradientDrawable) {
                GradientDrawable circle = (GradientDrawable) inicial.getBackground().mutate();
                try {
                    circle.setColor(Color.parseColor(item.categoriaCor));
                } catch (Exception e) {
                    circle.setColor(Color.GRAY);
                }
            }

            boolean isReceita = "receita".equals(item.tipoTransacao);
            valor.setTextColor(isReceita ? Color.parseColor("#006400") : Color.parseColor("#E45757"));

            boolean isPaga = (isReceita && item.recebido == 1) || (!isReceita && item.pago == 1);
            ((GradientDrawable) statusIndicator.getDrawable()).setColor(isPaga ? Color.parseColor("#006400") : Color.parseColor("#E45757"));
            
            String label = "";
            if (item.isProjecao) {
                label = "(fixa)";
            } else if (item.parcelas > 1) {
                label = String.format(Locale.getDefault(), "(%d/%d)", item.numeroParcela, item.parcelas);
            }
            tipoLabel.setText(label);
            tipoLabel.setVisibility(label.isEmpty() ? View.GONE : View.VISIBLE);

            itemView.setOnClickListener(v -> listener.onTransacaoClick(item));
        }
    }
}
