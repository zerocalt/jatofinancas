package com.example.app1;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app1.data.CartaoDAO;
import com.example.app1.utils.MenuHelper;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class CartaoAdapter extends RecyclerView.Adapter<CartaoAdapter.CartaoViewHolder> {

    private final List<TelaCadCartao.CartaoModel> cartoes;
    private final Context context;
    private final OnCartaoListener listener;

    public interface OnCartaoListener {
        void onEditClick(int idCartao);
        void onDeleteClick(int idCartao);
        void onAddDespesaClick(int idCartao);
        void onFaturaClick(int idCartao, int idUsuario);
    }

    public CartaoAdapter(Context context, List<TelaCadCartao.CartaoModel> cartoes, OnCartaoListener listener) {
        this.context = context;
        this.cartoes = cartoes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CartaoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cartao, parent, false);
        return new CartaoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartaoViewHolder holder, int position) {
        TelaCadCartao.CartaoModel cartao = cartoes.get(position);
        holder.bind(cartao, listener);
    }

    @Override
    public int getItemCount() {
        return cartoes.size();
    }

    static class CartaoViewHolder extends RecyclerView.ViewHolder {
        TextView nomeCartao, valorParcial, limite, diaVencimento, diaFechamento, porcentagem;
        ImageView icTipoCartao, icMenuCartao;
        ProgressBar barraLimite;
        LinearLayout linhaFatura, linhaBarra, linhaBotoes;
        View btnAddDespesa, btnFatura;

        public CartaoViewHolder(@NonNull View itemView) {
            super(itemView);
            nomeCartao = itemView.findViewById(R.id.NomeCartao);
            valorParcial = itemView.findViewById(R.id.txtValorParcial);
            limite = itemView.findViewById(R.id.txtLimite);
            diaVencimento = itemView.findViewById(R.id.txtDiaVencimento);
            diaFechamento = itemView.findViewById(R.id.txtDiaFechamento);
            porcentagem = itemView.findViewById(R.id.txtPorcentagem);
            icTipoCartao = itemView.findViewById(R.id.icTipoCartao);
            icMenuCartao = itemView.findViewById(R.id.icMenuCartao);
            barraLimite = itemView.findViewById(R.id.barraLimite);
            linhaFatura = itemView.findViewById(R.id.linhaFaturaParcial);
            linhaBarra = itemView.findViewById(R.id.linhaBarraLimite);
            linhaBotoes = itemView.findViewById(R.id.linhaBotoes);
            btnAddDespesa = itemView.findViewById(R.id.btnAdicionarDespesa);
            btnFatura = itemView.findViewById(R.id.btnFaturaCartao);
        }

        public void bind(final TelaCadCartao.CartaoModel cartao, final OnCartaoListener listener) {
            NumberFormat formatoBR = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
            double limiteComprometido = CartaoDAO.calcularLimiteUtilizado(itemView.getContext(), cartao.id);
            double limiteDisponivel = cartao.limite - limiteComprometido;

            nomeCartao.setText(cartao.nome);
            valorParcial.setText(formatoBR.format(cartao.valorFaturaParcial));
            limite.setText(formatoBR.format(limiteDisponivel < 0 ? 0 : limiteDisponivel));
            diaVencimento.setText("Venc: " + cartao.diaVencimento);
            diaFechamento.setText(" | Fech: " + cartao.diaFechamento);

            double pct = cartao.limite > 0 ? (limiteComprometido / cartao.limite) * 100 : 0;
            porcentagem.setText(String.format(Locale.getDefault(), "%.2f%%", pct));
            barraLimite.setProgress((int) pct);

            // Bandeira
            if (cartao.bandeira != null) {
                switch (cartao.bandeira.toLowerCase()) {
                    case "visa": icTipoCartao.setImageResource(R.drawable.visa); break;
                    case "mastercard": icTipoCartao.setImageResource(R.drawable.mastercard); break;
                    default: icTipoCartao.setImageResource(R.drawable.ic_credit_card_5px); break;
                }
            }

            // Cor do cartão
            if (itemView.getBackground() instanceof GradientDrawable) {
                GradientDrawable fundo = (GradientDrawable) itemView.getBackground().mutate();
                String cor = cartao.ativo == 1 ? cartao.cor : "#CCCCCC";
                if (TextUtils.isEmpty(cor)) {
                    cor = "#2F4F99"; // Cor padrão
                }
                try {
                    fundo.setColor(Color.parseColor(cor));
                } catch (IllegalArgumentException e) {
                    fundo.setColor(Color.parseColor("#2F4F99")); // Cor padrão em caso de erro
                }
            }

            // Visibilidade para cartões inativos
            int visibilidadeAtivo = cartao.ativo == 1 ? View.VISIBLE : View.GONE;
            linhaFatura.setVisibility(visibilidadeAtivo);
            linhaBarra.setVisibility(visibilidadeAtivo);
            linhaBotoes.setVisibility(visibilidadeAtivo);

            // Listeners
            icMenuCartao.setOnClickListener(v -> showPopupMenu(v, cartao, listener));
            if(cartao.ativo == 1) {
                btnAddDespesa.setOnClickListener(v -> listener.onAddDespesaClick(cartao.id));
                btnFatura.setOnClickListener(v -> listener.onFaturaClick(cartao.id, cartao.idUsuario));
            }
        }

        private void showPopupMenu(View view, final TelaCadCartao.CartaoModel cartao, final OnCartaoListener listener) {
            MenuHelper.MenuItemData[] menuItems = new MenuHelper.MenuItemData[]{
                    new MenuHelper.MenuItemData("Editar", R.drawable.ic_edit, () -> listener.onEditClick(cartao.id)),
                    new MenuHelper.MenuItemData("Excluir", R.drawable.ic_delete, () -> listener.onDeleteClick(cartao.id))
            };
            MenuHelper.showMenu(view.getContext(), view, menuItems);
        }
    }
}
