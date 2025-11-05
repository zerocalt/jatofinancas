package com.example.app1;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.appcompat.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.example.app1.data.ContaDAO;
import com.example.app1.utils.MascaraMonetaria;
import com.github.dewinjm.monthyearpicker.MonthYearPickerDialogFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TelaConta extends AppCompatActivity {

    private TextView txtMes, txtAno;
    private int idUsuarioLogado;
    private FloatingActionButton fabCadConta;
    private RecyclerView recyclerContas;
    private ContasTelaContaAdapter contasAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tela_conta);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        idUsuarioLogado = getIdUsuarioLogado();
        txtMes = findViewById(R.id.txtMes);
        txtAno = findViewById(R.id.txtAno);
        fabCadConta = findViewById(R.id.fabCadConta);
        recyclerContas = findViewById(R.id.recyclerContas);

        setupRecyclerView();

        findViewById(R.id.btnMesAno).setOnClickListener(v -> showMonthYearPicker());

        fabCadConta.setOnClickListener(v -> {
            abrirFragmentoCadastro(null);
        });

        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentViewDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentViewDestroyed(fm, f);
                if (f instanceof MenuCadContaFragment) {
                    fabCadConta.setVisibility(View.VISIBLE);
                }
            }
        }, false);

        Calendar agora = Calendar.getInstance();
        String[] nomesMes = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
        txtMes.setText(nomesMes[agora.get(Calendar.MONTH)]);
        txtAno.setText(String.valueOf(agora.get(Calendar.YEAR)));

        carregarMenu();
        carregarContas();
    }

    private void setupRecyclerView() {
        recyclerContas.setLayoutManager(new LinearLayoutManager(this));
        contasAdapter = new ContasTelaContaAdapter(this, new ArrayList<>());
        recyclerContas.setAdapter(contasAdapter);
    }

    private void carregarContas() {
        List<Conta> contas = ContaDAO.carregarListaContas(this, idUsuarioLogado);
        contasAdapter.setContas(contas);
    }

    private void abrirFragmentoCadastro(Conta conta) {
        fabCadConta.setVisibility(View.GONE);
        MenuCadContaFragment fragment = (conta == null)
                ? MenuCadContaFragment.newInstance(idUsuarioLogado)
                : MenuCadContaFragment.newInstance(idUsuarioLogado, conta.getId());

        fragment.setOnContaSalvaListener(nomeConta -> {
            carregarContas();
        });

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerFragment, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void excluirConta(int contaId) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir Conta")
                .setMessage("Tem certeza que deseja excluir esta conta?")
                .setPositiveButton("Sim", (dialog, which) -> {
                    boolean sucesso = ContaDAO.excluirConta(this, contaId);
                    if (sucesso) {
                        Toast.makeText(this, "Conta excluída com sucesso!", Toast.LENGTH_SHORT).show();
                        carregarContas();
                    } else {
                        Toast.makeText(this, "Erro ao excluir conta.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Não", null)
                .show();
    }

    private void mostrarPopupEditarSaldo(Conta conta) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Editar Saldo");

        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int margin = (int) (20 * getResources().getDisplayMetrics().density);
        params.leftMargin = margin;
        params.rightMargin = margin;

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.addTextChangedListener(new MascaraMonetaria(input));

        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        input.setText(format.format(conta.getSaldo()));
        input.setLayoutParams(params);
        container.addView(input);

        builder.setView(container);

        builder.setPositiveButton("Salvar", (dialog, which) -> {
            String saldoStr = input.getText().toString();
            double novoSaldo = 0;
            try {
                if (!saldoStr.isEmpty()) {
                    String valorLimpo = saldoStr.replace("R$", "").replaceAll("[^0-9,.]", "").replace(".", "").replace(",", ".");
                    novoSaldo = Double.parseDouble(valorLimpo);
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Saldo inválido.", Toast.LENGTH_LONG).show();
                return;
            }

            boolean sucesso = ContaDAO.atualizarSaldoConta(this, conta.getId(), novoSaldo);
            if (sucesso) {
                Toast.makeText(this, "Saldo atualizado com sucesso!", Toast.LENGTH_SHORT).show();
                carregarContas(); // Recarrega a lista para mostrar o novo saldo
            } else {
                Toast.makeText(this, "Erro ao atualizar o saldo.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.cancel());

        builder.show();
    }


    private void carregarMenu() {
        BottomMenuFragment bottomMenuFragment = new BottomMenuFragment();
        Bundle bundle = new Bundle();
        bundle.putInt("botaoInativo", BottomMenuFragment.OPCOES);
        bottomMenuFragment.setArguments(bundle);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.menu_container, bottomMenuFragment)
                .commit();
    }

    private void showMonthYearPicker() {
        final String[] nomesMes = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
        int mesIndex = getMesIndex(txtMes.getText().toString());
        int anoSelecionado = Integer.parseInt(txtAno.getText().toString());

        MonthYearPickerDialogFragment dialogFragment = MonthYearPickerDialogFragment.getInstance(mesIndex, anoSelecionado);
        dialogFragment.setOnDateSetListener((year, monthOfYear) -> {
            txtMes.setText(nomesMes[monthOfYear]);
            txtAno.setText(String.valueOf(year));
            carregarContas();
        });
        dialogFragment.show(getSupportFragmentManager(), "MonthYearPickerDialog");
    }

    private int getMesIndex(String nomeMes) {
        String[] nomes = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
        for (int i = 0; i < nomes.length; i++) {
            if (nomes[i].equalsIgnoreCase(nomeMes)) return i;
        }
        return 0;
    }

    private int getIdUsuarioLogado() {
        try {
            MasterKey masterKey = new MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();
            SharedPreferences prefs = EncryptedSharedPreferences.create(this, "secure_login_prefs", masterKey, EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV, EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
            return prefs.getInt("saved_user_id", -1);
        } catch (Exception e) {
            return -1;
        }
    }

    public class ContasTelaContaAdapter extends RecyclerView.Adapter<ContasTelaContaAdapter.ContaViewHolder> {

        private List<Conta> contas;
        private Context context;

        public ContasTelaContaAdapter(Context context, List<Conta> contas) {
            this.context = context;
            this.contas = contas;
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

            holder.itemView.setBackgroundResource(R.drawable.background_lista_cartoes);

            int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, context.getResources().getDisplayMetrics());
            holder.itemView.setPadding(padding, padding, padding, padding);

            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
            layoutParams.setMargins(0, 0, 0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, context.getResources().getDisplayMetrics()));
            holder.itemView.setLayoutParams(layoutParams);

            holder.txtNomeConta.setText(conta.getNome());

            NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
            holder.txtSaldoConta.setText(format.format(conta.getSaldo()));

            if (conta.getSaldo() >= 0) {
                holder.txtSaldoConta.setTextColor(Color.parseColor("#339933")); // Verde
            } else {
                holder.txtSaldoConta.setTextColor(Color.parseColor("#E45757")); // Vermelho
            }

            holder.txtIniciaisConta.setText(getIniciais(conta.getNome()));
            GradientDrawable background = (GradientDrawable) holder.viewCorConta.getBackground().mutate();
            int corDeFundo;
            try {
                corDeFundo = Color.parseColor(conta.getCor());
            } catch (IllegalArgumentException e) {
                corDeFundo = Color.GRAY;
            }
            background.setColor(corDeFundo);

            if (isCorClara(corDeFundo)) {
                holder.txtIniciaisConta.setTextColor(Color.BLACK);
            } else {
                holder.txtIniciaisConta.setTextColor(Color.WHITE);
            }

            holder.menuConta.setVisibility(View.VISIBLE);
            holder.menuConta.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(context, v);

                try {
                    java.lang.reflect.Field popupField = popup.getClass().getDeclaredField("mPopup");
                    popupField.setAccessible(true);
                    Object menuPopupHelper = popupField.get(popup);
                    Class<?> helperClass = Class.forName(menuPopupHelper.getClass().getName());
                    java.lang.reflect.Method setForceIcons = helperClass.getMethod("setForceShowIcon", boolean.class);
                    setForceIcons.invoke(menuPopupHelper, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                popup.inflate(R.menu.menu_edit_delete);

                MenuItem excluirItem = popup.getMenu().findItem(R.id.menu_excluir);
                Drawable icon = excluirItem.getIcon();

                if (conta.isEmUso()) {
                    if (icon != null) {
                        icon.mutate().setAlpha(130);
                    }
                } else {
                    if (icon != null) {
                        icon.mutate().setAlpha(255);
                    }
                }

                popup.setOnMenuItemClickListener(item -> {
                    int itemId = item.getItemId();
                    if (itemId == R.id.menu_editar) {
                        abrirFragmentoCadastro(conta);
                        return true;
                    } else if (itemId == R.id.menu_alterar_saldo) {
                        mostrarPopupEditarSaldo(conta);
                        return true;
                    } else if (itemId == R.id.menu_excluir) {
                        if (conta.isEmUso()) {
                            Toast.makeText(context, "Esta conta não pode ser excluída pois possui transações ou cartões associados.", Toast.LENGTH_LONG).show();
                        } else {
                            excluirConta(conta.getId());
                        }
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
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

        class ContaViewHolder extends RecyclerView.ViewHolder {
            View viewCorConta;
            TextView txtIniciaisConta;
            TextView txtNomeConta;
            TextView txtSaldoConta;
            ImageView menuConta;

            public ContaViewHolder(@NonNull View itemView) {
                super(itemView);
                viewCorConta = itemView.findViewById(R.id.viewCorConta);
                txtIniciaisConta = itemView.findViewById(R.id.txtIniciaisConta);
                txtNomeConta = itemView.findViewById(R.id.txtNomeConta);
                txtSaldoConta = itemView.findViewById(R.id.txtSaldoConta);
                menuConta = itemView.findViewById(R.id.menu_conta);
            }
        }
    }
}
