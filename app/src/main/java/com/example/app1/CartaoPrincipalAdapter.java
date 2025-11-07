package com.example.app1;

import android.content.Context;
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

public class CartaoPrincipalAdapter extends RecyclerView.Adapter<CartaoPrincipalAdapter.CartaoViewHolder> {

    private List<CartaoFatura> cartoes;
    private Context context;
    private OnCartaoClickListener listener;

    public interface OnCartaoClickListener {
        void onCartaoClick(int idCartao);
    }

    public CartaoPrincipalAdapter(Context context, List<CartaoFatura> cartoes, OnCartaoClickListener listener) {
        this.context = context;
        this.cartoes = cartoes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartaoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cartao_principal, parent, false);
        return new CartaoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartaoViewHolder holder, int position) {
        CartaoFatura cartaoFatura = cartoes.get(position);
        Cartao cartao = cartaoFatura.getCartao();

        holder.txtNomeCartao.setText(cartao.getNome());

        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        holder.txtValorFatura.setText(format.format(cartaoFatura.getValorFatura()));

        int bandeiraResId = 0;
        String bandeira = cartao.getBandeira();
        if (bandeira != null && !bandeira.isEmpty()) {
            String resourceName = bandeira.toLowerCase();
            bandeiraResId = context.getResources().getIdentifier(resourceName, "drawable", context.getPackageName());
        }

        if (bandeiraResId != 0) {
            holder.imgBandeiraCartao.setImageResource(bandeiraResId);
        } else {
            holder.imgBandeiraCartao.setImageResource(R.drawable.ic_credit_card); 
        }

        holder.itemView.setOnClickListener(v -> {
            if(listener != null) {
                listener.onCartaoClick(cartao.getId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartoes.size();
    }

    public void setCartoes(List<CartaoFatura> cartoes) {
        this.cartoes = cartoes;
        notifyDataSetChanged();
    }

    static class CartaoViewHolder extends RecyclerView.ViewHolder {
        ImageView imgBandeiraCartao;
        TextView txtNomeCartao;
        TextView txtValorFatura;

        public CartaoViewHolder(@NonNull View itemView) {
            super(itemView);
            imgBandeiraCartao = itemView.findViewById(R.id.imgBandeiraCartao);
            txtNomeCartao = itemView.findViewById(R.id.txtNomeCartao);
            txtValorFatura = itemView.findViewById(R.id.txtValorFatura);
        }
    }
}
