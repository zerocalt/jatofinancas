package com.example.app1;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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

import com.example.app1.data.CategoriaDAO;
import com.github.dewinjm.monthyearpicker.MonthYearPickerDialogFragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TelaCategorias extends AppCompatActivity {

    private TextView txtMes, txtAno;
    private int idUsuarioLogado;
    private FloatingActionButton fabCadCategoria;
    private RecyclerView recyclerCategorias;
    private CategoriasAdapter categoriasAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tela_categorias);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        idUsuarioLogado = getIdUsuarioLogado();
        txtMes = findViewById(R.id.txtMes);
        txtAno = findViewById(R.id.txtAno);
        fabCadCategoria = findViewById(R.id.fabCadCategoria);
        recyclerCategorias = findViewById(R.id.recyclerCategorias);

        setupRecyclerView();

        findViewById(R.id.btnMesAno).setOnClickListener(v -> showMonthYearPicker());

        fabCadCategoria.setOnClickListener(v -> {
            abrirFragmentoCadastro(null);
        });

        getSupportFragmentManager().registerFragmentLifecycleCallbacks(new FragmentManager.FragmentLifecycleCallbacks() {
            @Override
            public void onFragmentViewDestroyed(@NonNull FragmentManager fm, @NonNull Fragment f) {
                super.onFragmentViewDestroyed(fm, f);
                if (f instanceof MenuCadCategoriaFragment) {
                    fabCadCategoria.setVisibility(View.VISIBLE);
                }
            }
        }, false);

        Calendar agora = Calendar.getInstance();
        String[] nomesMes = {"Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho", "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"};
        txtMes.setText(nomesMes[agora.get(Calendar.MONTH)]);
        txtAno.setText(String.valueOf(agora.get(Calendar.YEAR)));

        carregarMenu();
        carregarCategorias();
    }

    private void setupRecyclerView() {
        recyclerCategorias.setLayoutManager(new LinearLayoutManager(this));
        categoriasAdapter = new CategoriasAdapter(this, new ArrayList<>());
        recyclerCategorias.setAdapter(categoriasAdapter);
    }

    private void carregarCategorias() {
        String mesAno = String.format(Locale.ROOT, "%04d-%02d", Integer.parseInt(txtAno.getText().toString()), getMesIndex(txtMes.getText().toString()) + 1);
        List<Categoria> categorias = CategoriaDAO.listarCategoriasComDespesas(this, idUsuarioLogado, mesAno);
        categoriasAdapter.setCategorias(categorias);
    }

    private void abrirFragmentoCadastro(Categoria categoria) {
        fabCadCategoria.setVisibility(View.GONE);
        MenuCadCategoriaFragment fragment = (categoria == null)
                ? MenuCadCategoriaFragment.newInstance(idUsuarioLogado)
                : MenuCadCategoriaFragment.newInstance(idUsuarioLogado, categoria.getId());

        fragment.setOnCategoriaSalvaListener(nomeCategoria -> {
            carregarCategorias();
        });

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.containerFragment, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void excluirCategoria(int categoriaId) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir Categoria")
                .setMessage("Tem certeza que deseja excluir esta categoria?")
                .setPositiveButton("Sim", (dialog, which) -> {
                    boolean sucesso = CategoriaDAO.excluirCategoria(this, categoriaId);
                    if (sucesso) {
                        Toast.makeText(this, "Categoria excluída com sucesso!", Toast.LENGTH_SHORT).show();
                        carregarCategorias();
                    } else {
                        Toast.makeText(this, "Erro ao excluir categoria. Verifique se ela não está em uso.", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Não", null)
                .show();
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
            carregarCategorias();
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

    public class CategoriasAdapter extends RecyclerView.Adapter<CategoriasAdapter.CategoriaViewHolder> {

        private List<Categoria> categorias;
        private Context context;

        public CategoriasAdapter(Context context, List<Categoria> categorias) {
            this.context = context;
            this.categorias = categorias;
        }

        @NonNull
        @Override
        public CategoriaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_categoria, parent, false);
            return new CategoriaViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CategoriaViewHolder holder, int position) {
            Categoria categoria = categorias.get(position);

            holder.itemView.setBackgroundResource(R.drawable.background_lista_cartoes);

            int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16, context.getResources().getDisplayMetrics());
            holder.itemView.setPadding(padding, padding, padding, padding);

            ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
            layoutParams.setMargins(0, 0, 0, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, context.getResources().getDisplayMetrics()));
            holder.itemView.setLayoutParams(layoutParams);

            holder.txtNomeCategoria.setText(categoria.getNome());

            NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
            holder.txtTotalDespesa.setText(format.format(categoria.getTotalDespesa()));

            holder.txtIniciaisCategoria.setText(getIniciais(categoria.getNome()));
            GradientDrawable background = (GradientDrawable) holder.viewCorCategoria.getBackground().mutate();
            int corDeFundo;
            try {
                corDeFundo = Color.parseColor(categoria.getCor());
            } catch (IllegalArgumentException e) {
                corDeFundo = Color.GRAY;
            }
            background.setColor(corDeFundo);

            if (isCorClara(corDeFundo)) {
                holder.txtIniciaisCategoria.setTextColor(Color.BLACK);
            } else {
                holder.txtIniciaisCategoria.setTextColor(Color.WHITE);
            }

            holder.menuCategoria.setVisibility(View.VISIBLE);
            holder.menuCategoria.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(context, v);

                // Final and correct fix: This reflection code is necessary to force PopupMenu to show icons.
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

                if (categoria.isEmUso()) {
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
                        abrirFragmentoCadastro(categoria);
                        return true;
                    } else if (itemId == R.id.menu_excluir) {
                        if (categoria.isEmUso()) {
                            Toast.makeText(context, "Esta categoria não pode ser excluída pois está em uso.", Toast.LENGTH_LONG).show();
                        } else {
                            excluirCategoria(categoria.getId());
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
            return categorias.size();
        }

        public void setCategorias(List<Categoria> categorias) {
            this.categorias = categorias;
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

        class CategoriaViewHolder extends RecyclerView.ViewHolder {
            View viewCorCategoria;
            TextView txtIniciaisCategoria;
            TextView txtNomeCategoria;
            TextView txtTotalDespesa;
            ImageView menuCategoria;

            public CategoriaViewHolder(@NonNull View itemView) {
                super(itemView);
                viewCorCategoria = itemView.findViewById(R.id.viewCorCategoria);
                txtIniciaisCategoria = itemView.findViewById(R.id.txtIniciaisCategoria);
                txtNomeCategoria = itemView.findViewById(R.id.txtNomeCategoria);
                txtTotalDespesa = itemView.findViewById(R.id.txtTotalDespesa);
                menuCategoria = itemView.findViewById(R.id.menu_categoria);
            }
        }
    }
}
