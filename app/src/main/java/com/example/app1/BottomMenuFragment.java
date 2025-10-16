package com.example.app1;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class BottomMenuFragment extends Fragment {
    public static final int PRINCIPAL = 0;
    public static final int TRANSACOES = 1;
    public static final int CARTOES = 2;
    public static final int OPCOES = 3;

    private int botaoInativo = PRINCIPAL;
    private boolean menuAberto = false;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (getArguments() != null) {
            botaoInativo = getArguments().getInt("botaoInativo", PRINCIPAL);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_bottom_menu, container, false);

        // Menu inferior e botões
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

        // Estado do menu inferior
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
        }

        btnPrincipal.setOnClickListener(vi -> {
            if (botaoInativo != PRINCIPAL) { /* sua navegação */ }
        });
        btnTransacoes.setOnClickListener(vi -> {
            if (botaoInativo != TRANSACOES) { /* navegação */ }
        });
        btnCartoes.setOnClickListener(vi -> {
            if (botaoInativo != CARTOES) { /* navegação */
                Intent intent = new Intent(getActivity(), TelaCadCartao.class);
                startActivity(intent);
            }
        });
        btnOpcoes.setOnClickListener(vi -> {
            if (botaoInativo != OPCOES) { /* navegação */ }
        });

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

        fabReceita.setOnClickListener(btn -> { /* Ação Receita */ });
        fabDespesa.setOnClickListener(btn -> { /* Ação Despesa */ });
        fabTransferencia.setOnClickListener(btn -> { /* Ação Transferência */ });
        fabDespesaCartao.setOnClickListener(btn -> { /* Ação Despesa Cartão */ });

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
}