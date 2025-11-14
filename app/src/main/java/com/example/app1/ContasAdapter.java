package com.example.app1;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class ContasAdapter extends RecyclerView.Adapter<ContasAdapter.ContaViewHolder> {

    private List<Conta> contas;
    private Context context;
    private OnContaClickListener listener;

    public ContasAdapter(Context context, List<Conta> contas, OnContaClickListener listener) {
        this.context = context;
        this.contas = contas;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ContaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_conta, parent, false);
        return new ContaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContaViewHolder holder, int position) {
        Conta conta = contas.get(position);

        holder.txtNomeConta.setText(conta.getNome());

        // Formatar saldo como moeda
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        holder.txtSaldoConta.setText(format.format(conta.getSaldo()));
        holder.txtSaldoContaPrevisao.setText("saldo previsto: " + format.format(conta.getSaldoPrevisto()));

        // Definir a cor do saldo
        if (conta.getSaldo() >= 0) {
            holder.txtSaldoConta.setTextColor(Color.parseColor("#339933")); // Verde
        } else {
            holder.txtSaldoConta.setTextColor(Color.parseColor("#E45757")); // Vermelho
        }

        // Definir iniciais e cor do círculo
        holder.txtIniciaisConta.setText(getIniciais(conta.getNome()));
        GradientDrawable background = (GradientDrawable) holder.viewCorConta.getBackground().mutate();
        int corDeFundo;
        try {
            corDeFundo = Color.parseColor(conta.getCor());
        } catch (IllegalArgumentException e) {
            corDeFundo = Color.GRAY; // Cor padrão em caso de erro
        }
        background.setColor(corDeFundo);

        // Ajustar a cor do texto com base na luminosidade do fundo
        if (isCorClara(corDeFundo)) {
            holder.txtIniciaisConta.setTextColor(Color.BLACK);
        } else {
            holder.txtIniciaisConta.setTextColor(Color.WHITE);
        }

        holder.itemView.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(context, v);
            popupMenu.getMenuInflater().inflate(R.menu.menu_contas_telaprincipal, popupMenu.getMenu());

            // Forçar exibição dos ícones
            try {
                Field[] fields = popupMenu.getClass().getDeclaredFields();
                for (Field field : fields) {
                    if ("mPopup".equals(field.getName())) {
                        field.setAccessible(true);
                        Object menuPopupHelper = field.get(popupMenu);
                        Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                        Method setForceIcons = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
                        setForceIcons.invoke(menuPopupHelper, true);
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            popupMenu.setOnMenuItemClickListener(menuItem -> {
                int id = menuItem.getItemId();
                if (id == R.id.menu_alterar_saldo) {
                    listener.onEditarSaldo(conta);
                    return true;
                } else if (id == R.id.menu_exibir_transacoes) {
                    listener.onExibirTransacoes(conta);
                    return true;
                }
                return false;
            });

            popupMenu.show();
        });

        holder.menuConta.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return contas.size();
    }

    public void setContas(List<Conta> contas) {
        this.contas = contas;
        notifyDataSetChanged();
    }

    private boolean isCorClara(int color) {
        return ColorUtils.calculateLuminance(color) > 0.5;
    }

    private String getIniciais(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            return "";
        }
        String nomeLimpo = nome.trim();
        String[] palavras = nomeLimpo.split("\\s+");
        StringBuilder iniciais = new StringBuilder();

        if (palavras.length >= 2) {
            iniciais.append(palavras[0].charAt(0));
            iniciais.append(palavras[1].charAt(0));
        } else if (palavras.length == 1 && !palavras[0].isEmpty()) {
            if (palavras[0].length() >= 2) {
                iniciais.append(palavras[0].substring(0, 2));
            } else {
                iniciais.append(palavras[0].charAt(0));
            }
        }
        return iniciais.toString().toUpperCase();
    }

    static class ContaViewHolder extends RecyclerView.ViewHolder {
        View viewCorConta;
        TextView txtIniciaisConta;
        TextView txtNomeConta;
        TextView txtSaldoConta;
        TextView txtSaldoContaPrevisao;
        View menuConta;

        public ContaViewHolder(@NonNull View itemView) {
            super(itemView);
            viewCorConta = itemView.findViewById(R.id.viewCorConta);
            txtIniciaisConta = itemView.findViewById(R.id.txtIniciaisConta);
            txtNomeConta = itemView.findViewById(R.id.txtNomeConta);
            txtSaldoConta = itemView.findViewById(R.id.txtSaldoConta);
            txtSaldoContaPrevisao = itemView.findViewById(R.id.txtSaldoContaPrevisao);
            menuConta = itemView.findViewById(R.id.menu_conta);
        }
    }

    public interface OnContaClickListener {
        void onEditarSaldo(Conta conta);
        void onExibirTransacoes(Conta conta);
    }

}
