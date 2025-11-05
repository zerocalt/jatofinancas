package com.example.app1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.example.app1.utils.MenuHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class BottomMenuFragment extends Fragment {
    public static final int PRINCIPAL = 0;
    public static final int TRANSACOES = 1;
    public static final int CARTOES = 2;
    public static final int OPCOES = 3;

    private int botaoInativo = -1;
    private boolean menuAberto = false;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (getArguments() != null) {
            botaoInativo = getArguments().getInt("botaoInativo", -1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_bottom_menu, container, false);

        getParentFragmentManager().setFragmentResultListener("despesaSalvaRequest", this, (requestKey, bundle) -> {
            if (bundle.getBoolean("atualizar")) {
                refreshCurrentActivity();
            }
        });

        setupNavigation(v);
        setupFabMenu(v);

        return v;
    }

    private void setupNavigation(View v) {
        LinearLayout btnPrincipal = v.findViewById(R.id.btnPrincipal);
        LinearLayout btnTransacoes = v.findViewById(R.id.btnTransacoes);
        LinearLayout btnCartoes = v.findViewById(R.id.btnCartoes);
        LinearLayout btnOpcoes = v.findViewById(R.id.btnOpcoes);

        if (botaoInativo != -1) {
            deactivateButton(v, botaoInativo);
        }

        btnPrincipal.setOnClickListener(vi -> navigateTo(TelaPrincipal.class));
        btnTransacoes.setOnClickListener(vi -> navigateTo(TelaTransacoes.class));
        btnCartoes.setOnClickListener(vi -> navigateTo(TelaCadCartao.class));
        btnOpcoes.setOnClickListener(this::mostrarMenuOpcoes);
    }

    private void mostrarMenuOpcoes(View view) {
        MenuHelper.MenuItemData[] menuItems = new MenuHelper.MenuItemData[]{
                new MenuHelper.MenuItemData("Categorias", R.drawable.ic_category, () -> {
                    // Ação para Categorias
                }),
                new MenuHelper.MenuItemData("Contas", R.drawable.ic_account_balance, () -> navigateTo(TelaConta.class)),
                new MenuHelper.MenuItemData("Gráficos", R.drawable.ic_credit_card, () -> {
                    // Ação para Gráficos
                }),
                new MenuHelper.MenuItemData("Relatórios", R.drawable.ic_credit_card, () -> {
                    // Ação para Relatórios
                }),
                new MenuHelper.MenuItemData("Sobre", R.drawable.ic_credit_card, () -> {
                    // Ação para Sobre
                }),
                new MenuHelper.MenuItemData("Encerrar Sessão", R.drawable.ic_credit_card, this::fazerLogout)
        };

        MenuHelper.showMenu(getContext(), view, menuItems);
    }

    private void fazerLogout() {
        if (getContext() == null) return;

        try {
            MasterKey masterKey = new MasterKey.Builder(getContext().getApplicationContext())
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            SharedPreferences prefs = EncryptedSharedPreferences.create(
                    getContext().getApplicationContext(),
                    "secure_login_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );

            SharedPreferences.Editor editor = prefs.edit();
            editor.remove("saved_email");
            editor.commit();

            Toast.makeText(getContext(), "Sessão encerrada", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("from_logout", true);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Erro ao limpar sessão", Toast.LENGTH_LONG).show();
        }
    }

    private void deactivateButton(View v, int buttonId) {
        @ColorInt int corDesativado = ContextCompat.getColor(requireContext(), R.color.botaoDesativado);
        int icId = 0, txtId = 0, btnId = 0;

        switch (buttonId) {
            case PRINCIPAL: icId = R.id.icPrincipal; txtId = R.id.txtPrincipal; btnId = R.id.btnPrincipal; break;
            case TRANSACOES: icId = R.id.icTransacoes; txtId = R.id.txtTransacoes; btnId = R.id.btnTransacoes; break;
            case CARTOES: icId = R.id.icCartoes; txtId = R.id.txtCartoes; btnId = R.id.btnCartoes; break;
            case OPCOES: icId = R.id.icOpcoes; txtId = R.id.txtOpcoes; btnId = R.id.btnOpcoes; break;
        }

        if (icId != 0) {
            ((ImageView) v.findViewById(icId)).setColorFilter(corDesativado);
            ((TextView) v.findViewById(txtId)).setTextColor(corDesativado);
            v.findViewById(btnId).setClickable(false);
        }
    }

    private void navigateTo(Class<?> cls) {
        Intent intent = new Intent(getActivity(), cls);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        if (cls == TelaTransacoes.class) { 
            intent.putExtra("id_usuario", getIdUsuarioLogado());
        }
        startActivity(intent);
    }

    private void setupFabMenu(View v) {
        FloatingActionButton fab = v.findViewById(R.id.addmovimentacao);
        if (getActivity() instanceof TelaCadCartao) {
            fab.setVisibility(View.GONE);
            return;
        }
        
        View overlay = v.findViewById(R.id.overlay);
        LinearLayout quickActions = v.findViewById(R.id.quickActions);
        FloatingActionButton fabReceita = v.findViewById(R.id.fabReceita);
        FloatingActionButton fabDespesa = v.findViewById(R.id.fabDespesa);
        FloatingActionButton fabTransferencia = v.findViewById(R.id.fabTransferencia);
        FloatingActionButton fabDespesaCartao = v.findViewById(R.id.fabDespesaCartao);

        fab.setOnClickListener(view -> {
            if (!menuAberto) {
                openMenu(overlay, quickActions, fabReceita, fabDespesa, fabTransferencia, fabDespesaCartao);
            } else {
                closeMenu(overlay, quickActions, fabReceita, fabDespesa, fabTransferencia, fabDespesaCartao);
            }
        });

        overlay.setOnClickListener(view -> closeMenu(overlay, quickActions, fabReceita, fabDespesa, fabTransferencia, fabDespesaCartao));

        fabReceita.setOnClickListener(btn -> openCadFragment("receita"));
        fabDespesa.setOnClickListener(btn -> openCadFragment("despesa"));
        fabDespesaCartao.setOnClickListener(btn -> openCadFragment("despesa_cartao"));
        fabTransferencia.setOnClickListener(btn -> { /* Ação para Transferência */ });
    }

    private void openCadFragment(String tipo) {
        if (getActivity() == null) return;
        int idUsuario = getIdUsuarioLogado();
        if (idUsuario == -1) return;

        View v = getView();
        closeMenu(v.findViewById(R.id.overlay), v.findViewById(R.id.quickActions),
                v.findViewById(R.id.fabReceita), v.findViewById(R.id.fabDespesa), v.findViewById(R.id.fabTransferencia), v.findViewById(R.id.fabDespesaCartao));

        v.postDelayed(() -> {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            int containerId = R.id.containerFragment; // Usa o ID padrão

            if (tipo.equals("despesa_cartao")) {
                MenuCadDespesaCartaoFragment fragment = MenuCadDespesaCartaoFragment.newInstance(idUsuario, -1);
                activity.getSupportFragmentManager().beginTransaction()
                        .replace(containerId, fragment).addToBackStack(null).commit();
            } else {
                MenuCadDespesaFragment fragment = MenuCadDespesaFragment.newInstance(idUsuario, tipo);
                fragment.setOnTransacaoSalvaListener(new MenuCadDespesaFragment.OnTransacaoSalvaListener() {
                    @Override
                    public void onTransacaoSalva() {
                        refreshCurrentActivity();
                    }
                });
                activity.getSupportFragmentManager().beginTransaction()
                        .replace(containerId, fragment).addToBackStack(null).commit();
            }
        }, 250);
    }

    private void refreshCurrentActivity() {
        if (getActivity() instanceof TelaTransacoes) {
            ((TelaTransacoes) getActivity()).carregarTransacoesAsync();

        } else if (getActivity() instanceof TelaPrincipal) {
            ((TelaPrincipal) getActivity()).carregarDadosAsync();

        } else if (getActivity() instanceof TelaFaturaCartao) {
            TelaFaturaCartao tela = (TelaFaturaCartao) getActivity();
            int idCartao = tela.getIdCartao();
            int idUsuario = tela.getIdUsuario(); // 👈 adiciona este getter na TelaFaturaCartao se ainda não tiver

            if (idCartao == -1 || idUsuario == -1) {
                Toast.makeText(getContext(), "Erro: id do cartão ou usuário inválido", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(getContext(), TelaFaturaCartao.class);
            intent.putExtra("id_cartao", idCartao);
            intent.putExtra("id_usuario", idUsuario); // 👈 garante envio dos dois dados
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);

            getActivity().finish();
            startActivity(intent);
        }
    }




    private void openMenu(View overlay, View quickActions, View... fabs) {
        overlay.setVisibility(View.VISIBLE);
        quickActions.setVisibility(View.VISIBLE);
        menuAberto = true;
        for (int i = 0; i < fabs.length; i++) {
            animateMenuOpen(fabs[i], i * 40);
        }
    }

    private void closeMenu(View overlay, View quickActions, View... fabs) {
        menuAberto = false;
        for (int i = 0; i < fabs.length; i++) {
            animateMenuClose(fabs[i], (fabs.length - 1 - i) * 40);
        }
        overlay.postDelayed(() -> {
            overlay.setVisibility(View.GONE);
            quickActions.setVisibility(View.GONE);
        }, 200);
    }

    private void animateMenuOpen(View v, int delay) {
        v.setVisibility(View.VISIBLE);
        v.setAlpha(0f);
        v.setScaleX(0f);
        v.setScaleY(0f);
        v.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(150).setStartDelay(delay).start();
    }

    private void animateMenuClose(View v, int delay) {
        v.animate().alpha(0f).scaleX(0f).scaleY(0f).setDuration(150).setStartDelay(delay)
            .withEndAction(() -> v.setVisibility(View.INVISIBLE)).start();
    }

    private int getIdUsuarioLogado() {
        try {
            MasterKey masterKey = new MasterKey.Builder(requireContext()).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();
            SharedPreferences prefs = EncryptedSharedPreferences.create(requireContext(), "secure_login_prefs", masterKey, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
            return prefs.getInt("saved_user_id", -1);
        } catch (Exception e) {
            return -1;
        }
    }
}
