package com.example.app1;
import com.example.app1.interfaces.BottomMenuListener;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.app1.utils.MenuBottomUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public class BottomMenuFragment extends Fragment {
    public static final int PRINCIPAL = 0;
    public static final int TRANSACOES = 1;
    public static final int CARTOES = 2;
    public static final int OPCOES = 3;

    private int botaoInativo = -1; // Mude para -1 padrão, indicando "todos ativos"
    private boolean menuAberto = false;
    private BottomMenuListener fabListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        // Tenta obter o listener da Activity
        if (context instanceof BottomMenuListener) {
            fabListener = (BottomMenuListener) context;
        } else {
            // Se a Activity Pai NÃO implementa a interface, o listener permanece null.
            fabListener = null;
            // Não lance exceção, apenas defina como null para que o menu funcione no modo padrão.
        }

        // Recebe o argumento, se existir. Se não, mantém botaoInativo = -1
        if (getArguments() != null) {
            botaoInativo = getArguments().getInt("botaoInativo", -1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_bottom_menu, container, false);

        LinearLayout btnPrincipal = v.findViewById(R.id.btnPrincipal);
        ImageView icPrincipal = v.findViewById(R.id.icPrincipal);
        TextView txtPrincipal = v.findViewById(R.id.txtPrincipal);

        LinearLayout btnTransacoes = v.findViewById(R.id.btnTransacoes);
        ImageView icTransacoes = v.findViewById(R.id.icTransacoes);
        TextView txtTransacoes = v.findViewById(R.id.txtTransacoes);

        LinearLayout btnCartoes = v.findViewById(R.id.btnCartoes);
        ImageView icCartoes = v.findViewById(R.id.icCartoes);
        TextView txtCartoes = v.findViewById(R.id.txtCartoes);

        LinearLayout btnOpcoes = v.findViewById(R.id.btnOpcoes);
        ImageView icOpcoes = v.findViewById(R.id.icOpcoes);
        TextView txtOpcoes = v.findViewById(R.id.txtOpcoes);

        FloatingActionButton fab = v.findViewById(R.id.addmovimentacao);

        @ColorInt int corDesativado = ContextCompat.getColor(requireContext(), R.color.botaoDesativado);
        @ColorInt int corNormal = ContextCompat.getColor(requireContext(), R.color.white);

        // Habilita todos os botões como padrão
        icPrincipal.setColorFilter(corNormal);
        txtPrincipal.setTextColor(corNormal);
        btnPrincipal.setClickable(true);
        icTransacoes.setColorFilter(corNormal);
        txtTransacoes.setTextColor(corNormal);
        btnTransacoes.setClickable(true);
        icCartoes.setColorFilter(corNormal);
        txtCartoes.setTextColor(corNormal);
        btnCartoes.setClickable(true);
        icOpcoes.setColorFilter(corNormal);
        txtOpcoes.setTextColor(corNormal);
        btnOpcoes.setClickable(true);

        // Só deixa algum botão inativo se o argumento for diferente de -1
        switch (botaoInativo) {
            case PRINCIPAL:
                icPrincipal.setColorFilter(corDesativado);
                txtPrincipal.setTextColor(corDesativado);
                btnPrincipal.setClickable(false);
                break;
            case TRANSACOES:
                icTransacoes.setColorFilter(corDesativado);
                txtTransacoes.setTextColor(corDesativado);
                btnTransacoes.setClickable(false);
                break;
            case CARTOES:
                icCartoes.setColorFilter(corDesativado);
                txtCartoes.setTextColor(corDesativado);
                btnCartoes.setClickable(false);
                break;
            case OPCOES:
                icOpcoes.setColorFilter(corDesativado);
                txtOpcoes.setTextColor(corDesativado);
                btnOpcoes.setClickable(false);
                break;
            case -1:
                // Nenhum botão inativo, todos ativos
                break;
        }

        btnPrincipal.setOnClickListener(vi -> {
            Intent intent = new Intent(getActivity(), TelaPrincipal.class);
            startActivity(intent);
        });
        btnTransacoes.setOnClickListener(vi -> { /* navegação transações */ });
        btnCartoes.setOnClickListener(vi -> {
            Intent intent = new Intent(getActivity(), TelaCadCartao.class);
            startActivity(intent);
        });
        btnOpcoes.setOnClickListener(vi -> { /* navegação opções */ });

        // ANIMAÇÃO E LÓGICA DO FAB E DO MENU FLUTUANTE
        View overlay = v.findViewById(R.id.overlay);
        LinearLayout quickActions = v.findViewById(R.id.quickActions);

        FloatingActionButton fabReceita = v.findViewById(R.id.fabReceita);
        FloatingActionButton fabDespesa = v.findViewById(R.id.fabDespesa);
        FloatingActionButton fabTransferencia = v.findViewById(R.id.fabTransferencia);
        FloatingActionButton fabDespesaCartao = v.findViewById(R.id.fabDespesaCartao);

        fab.setOnClickListener(view -> {
            if (!menuAberto) {
                overlay.setVisibility(View.VISIBLE);
                quickActions.setVisibility(View.VISIBLE);
                animateMenuOpen(fabReceita,  0);
                animateMenuOpen(fabDespesa,  50);
                animateMenuOpen(fabTransferencia, 100);
                animateMenuOpen(fabDespesaCartao, 150);
                menuAberto = true;
            } else {
                closeMenu(overlay, quickActions, fabReceita, fabDespesa, fabTransferencia, fabDespesaCartao);
            }
        });

        overlay.setOnClickListener(view -> {
            closeMenu(overlay, quickActions, fabReceita, fabDespesa, fabTransferencia, fabDespesaCartao);
        });

        int idUsuario = getIdUsuarioLogado();

        fabReceita.setOnClickListener(btn -> {
            if (getActivity() != null && idUsuario != -1) {
                // Fecha o menu atual antes de abrir outro
                closeMenu(v.findViewById(R.id.overlay),
                        v.findViewById(R.id.quickActions),
                        fabReceita, fabDespesa, fabTransferencia, fabDespesaCartao);

                // Abre o menu de despesas colocando dados de Receita
                // Usa um pequeno atraso para dar tempo da animação terminar
                v.postDelayed(() -> {
                    MenuBottomUtils.abrirMenuCadDespesa((AppCompatActivity) getActivity(), idUsuario, "receita");
                }, 200);
            }
        });

        // abrir menu Despesa
        fabDespesa.setOnClickListener(btn -> {
            if (getActivity() != null && idUsuario != -1) {
                // Fecha o menu atual antes de abrir outro
                closeMenu(v.findViewById(R.id.overlay),
                        v.findViewById(R.id.quickActions),
                        fabReceita, fabDespesa, fabTransferencia, fabDespesaCartao);

                // Abre o menu de despesas
                // Usa um pequeno atraso para dar tempo da animação terminar
                v.postDelayed(() -> {
                    MenuBottomUtils.abrirMenuCadDespesa((AppCompatActivity) getActivity(), idUsuario, "despesa");
                }, 200);
            }
        });
        fabTransferencia.setOnClickListener(btn -> { /* ação transferência */ });

        // abrir menu Despesa Cartão
        fabDespesaCartao.setOnClickListener(btn -> {
            if (getActivity() != null && idUsuario != -1) {
                // Fecha o menu atual antes de abrir outro
                closeMenu(v.findViewById(R.id.overlay),
                        v.findViewById(R.id.quickActions),
                        fabReceita, fabDespesa, fabTransferencia, fabDespesaCartao);

                // Abre o menu de despesas
                // Se a Activity PAI implementa o Listener (ex: TelaFaturaCartao)
                if (fabListener != null) {
                    // Usa a nova lógica (Activity PAI abre o fragmento e adiciona o Listener de atualização)
                    v.postDelayed(() -> {
                        fabListener.onFabDespesaCartaoClick(idUsuario);
                    }, 200);
                } else {
                    // Se a Activity PAI NÃO implementa o Listener (ex: TelaPrincipal)
                    // Usa a lógica ANTIGA, que abre o Fragmento DIRETAMENTE.
                    v.postDelayed(() -> {
                        MenuBottomUtils.abrirMenuCadDespesaCartao((AppCompatActivity) getActivity(), idUsuario, -1);
                    }, 200);
                }
            }
        });

        return v;
    }

    private void animateMenuOpen(View v, int delay) {
        v.setScaleX(0f);
        v.setScaleY(0f);
        v.setAlpha(0f);
        v.animate()
                .scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(220)
                .setStartDelay(delay)
                .start();
    }

    private void animateMenuClose(View v, int delay) {
        v.animate()
                .scaleX(0f).scaleY(0f).alpha(0f)
                .setDuration(180)
                .setStartDelay(delay)
                .withEndAction(() -> v.setScaleX(1f))
                .start();
    }

    private void closeMenu(View overlay, LinearLayout quickActions, View... fabs) {
        for (int i = 0; i < fabs.length; i++)
            animateMenuClose(fabs[i], i * 50);
        overlay.postDelayed(() -> {
            overlay.setVisibility(View.GONE);
            quickActions.setVisibility(View.GONE);
            menuAberto = false;
        }, 250);
    }

    //pegar o id_usuario
    private int getIdUsuarioLogado() {
        try {
            MasterKey masterKey = new MasterKey.Builder(requireContext())
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            SharedPreferences prefs = EncryptedSharedPreferences.create(
                    requireContext(),
                    "secure_login_prefs", // mesmo nome do MainActivity
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            return prefs.getInt("saved_user_id", -1); // mesma chave que salvamos
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

}