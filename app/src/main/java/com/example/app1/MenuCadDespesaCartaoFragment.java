package com.example.app1;

import android.content.Context;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.example.app1.utils.MascaraMonetaria;

import java.util.ArrayList;
import java.util.List;

public class MenuCadDespesaCartaoFragment extends Fragment {

    private View overlayDespesaCartao;
    private View slidingMenuDespesaCartao;
    private TextInputLayout tilNomeDespesaCartao, tilCategoriaDespesa, tilDataDespesaCartao, tilValorDespesa;
    private TextInputEditText inputNomeDespesaCartao, inputDataDespesaCartao, inputValorDespesa;
    private MaterialAutoCompleteTextView autoCompleteCategoria, autoCompleteCartao;
    private int idUsuarioLogado = -1;
    private int idCartao = -1;

    private android.widget.Button btnSalvarDespesaCartao;
    private OnBackPressedCallback backCallback;

    public static MenuCadDespesaCartaoFragment newInstance(int idUsuario, int idCartao) {
        MenuCadDespesaCartaoFragment fragment = new MenuCadDespesaCartaoFragment();
        Bundle args = new Bundle();
        args.putInt("id_usuario", idUsuario);
        args.putInt("id_cartao", idCartao);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            idUsuarioLogado = getArguments().getInt("id_usuario", -1);
            idCartao = getArguments().getInt("id_cartao", -1);
        }

        backCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                fecharMenu();
                if (getActivity() != null) {
                    View container = getActivity().findViewById(R.id.rootMenuConta);
                    if (container != null) container.setVisibility(View.GONE);
                }
                setEnabled(false);
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, backCallback);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,  @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_menu_cad_despesa_cartao, container, false);

        overlayDespesaCartao = root.findViewById(R.id.overlayDespesaCartao);
        slidingMenuDespesaCartao = root.findViewById(R.id.slidingMenuDespesaCartao);

        tilNomeDespesaCartao = root.findViewById(R.id.textInputNomeDespesaCartao);
        tilCategoriaDespesa = root.findViewById(R.id.textInputCategoriaDespesa);
        tilDataDespesaCartao = root.findViewById(R.id.textInputDataDespesaCartao);
        tilValorDespesa = root.findViewById(R.id.textInputValorDespesa);

        inputNomeDespesaCartao = root.findViewById(R.id.inputNomeDespesaCartao);
        inputDataDespesaCartao = root.findViewById(R.id.inputDataDespesaCartao);
        inputValorDespesa = root.findViewById(R.id.inputValorDespesa);

        autoCompleteCategoria = root.findViewById(R.id.autoCompleteTipoConta);
        autoCompleteCartao = root.findViewById(R.id.autoCompleteCartao);

        inputNomeDespesaCartao.addTextChangedListener(new SimpleTextWatcher(() -> tilNomeDespesaCartao.setError(null)));
        inputDataDespesaCartao.setOnClickListener(v -> openDatePicker());
        inputDataDespesaCartao.setFocusable(false);
        inputDataDespesaCartao.setClickable(true);

        // Carrega e aplica o adapter customizado nas categorias
        List<Categoria> categorias = carregarCategoriasComoCategoria(requireContext(), idUsuarioLogado);
        CategoriasDropdownAdapter adapter = new CategoriasDropdownAdapter(requireContext(), categorias);
        autoCompleteCategoria.setAdapter(adapter);

        autoCompleteCategoria.setOnItemClickListener((parent, view, position, id) -> {
            Categoria c = (Categoria) parent.getItemAtPosition(position);
            autoCompleteCategoria.setText(c.nome, false); // forçar só o nome, caso necessário
            tilCategoriaDespesa.setError(null);
        });

        // Preenche o nome do cartão selecionado e bloqueia edição
        if(idCartao != -1){
            try(SQLiteDatabase dbRead = new MeuDbHelper(requireContext()).getReadableDatabase()){
                try(Cursor cursor = dbRead.rawQuery("SELECT nome FROM cartoes WHERE id = ?", new String[]{String.valueOf(idCartao)})){
                    if(cursor.moveToFirst()){
                        autoCompleteCartao.setText(cursor.getString(cursor.getColumnIndexOrThrow("nome")), false);
                    }
                }
            }
        }
        autoCompleteCartao.setEnabled(false);

        inputValorDespesa.addTextChangedListener(new MascaraMonetaria(inputValorDespesa));

        overlayDespesaCartao.setOnClickListener(v -> fecharMenu());

        btnSalvarDespesaCartao = root.findViewById(R.id.btnSalvarDespesaCartao);
        btnSalvarDespesaCartao.setOnClickListener(v -> salvarDespesa());

        // Define a data atual formatada em dd/MM/yyyy
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
        String dataAtual = sdf.format(new java.util.Date());
        inputDataDespesaCartao.setText(dataAtual);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        abrirMenu();
    }

    public void abrirMenu() {
        overlayDespesaCartao.setVisibility(View.VISIBLE);
        slidingMenuDespesaCartao.setVisibility(View.VISIBLE);
        slidingMenuDespesaCartao.post(() -> {
            slidingMenuDespesaCartao.setTranslationY(slidingMenuDespesaCartao.getHeight());
            slidingMenuDespesaCartao.animate().translationY(0).setDuration(300).start();

            // Solicita foco no campo de valor da despesa e exibe teclado
            inputValorDespesa.requestFocus();
            InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(inputValorDespesa, InputMethodManager.SHOW_IMPLICIT);
            }
        });
        backCallback.setEnabled(true);
    }

    public void fecharMenu() {
        slidingMenuDespesaCartao.animate().translationY(slidingMenuDespesaCartao.getHeight()).setDuration(300)
                .withEndAction(() -> {
                    slidingMenuDespesaCartao.setVisibility(View.GONE);
                    overlayDespesaCartao.setVisibility(View.GONE);
                    backCallback.setEnabled(false);
                }).start();
    }

    private void salvarDespesa() {
        String nome = inputNomeDespesaCartao.getText().toString().trim();
        String categoria = autoCompleteCategoria.getText().toString().trim();
        String data = inputDataDespesaCartao.getText().toString().trim();
        String valorStr = inputValorDespesa.getText().toString().trim();
        valorStr = valorStr.replace("R$", "").replaceAll("[^0-9,]", "").trim();

        if (nome.isEmpty()) {
            tilNomeDespesaCartao.setError("Informe o nome da despesa");
            return;
        }

        if (categoria.isEmpty()) {
            tilCategoriaDespesa.setError("Informe a categoria");
            return;
        }

        if (data.isEmpty()) {
            tilDataDespesaCartao.setError("Informe a data da despesa");
            return;
        }

        double valor = 0;
        try {
            if (!valorStr.isEmpty()) {
                valor = Double.parseDouble(valorStr.replace(",", "."));
            }
        } catch (NumberFormatException e) {
            Snackbar.make(requireView(), "Valor inválido", Snackbar.LENGTH_LONG).show();
            return;
        }

        SQLiteDatabase db = new MeuDbHelper(requireContext()).getWritableDatabase();
        ContentValues valores = new ContentValues();
        valores.put("nome_despesa", nome);
        valores.put("categoria", categoria);
        valores.put("data_despesa", data);
        valores.put("valor", valor);
        valores.put("id_usuario", idUsuarioLogado);
        valores.put("id_cartao", idCartao);

        long res = db.insert("despesas_cartao", null, valores);
        db.close();

        if (res != -1) {
            Snackbar.make(requireView(), "Despesa salva com sucesso!", Snackbar.LENGTH_LONG).show();
            fecharMenu();
            limparCampos();
        } else {
            Snackbar.make(requireView(), "Erro ao salvar despesa.", Snackbar.LENGTH_LONG).show();
        }
    }

    // Carrega lista de categorias (nome + cor), globais e do usuário, sem duplicados e ordenadas
    private List<Categoria> carregarCategoriasComoCategoria(Context ctx, int idUsuario) {
        List<Categoria> lista = new ArrayList<>();
        String sql = "SELECT nome, cor FROM categorias WHERE id_usuario IS NULL OR id_usuario = ? ORDER BY nome COLLATE NOCASE ASC";
        try (SQLiteDatabase db = new MeuDbHelper(ctx).getReadableDatabase();
             Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(idUsuario)})) {
            if (cursor != null && cursor.moveToFirst()) {
                ArrayList<String> nomesUnicos = new ArrayList<>();
                do {
                    String nome = cursor.getString(cursor.getColumnIndexOrThrow("nome"));
                    String cor = cursor.getString(cursor.getColumnIndexOrThrow("cor"));
                    if (!nomesUnicos.contains(nome)) {
                        nomesUnicos.add(nome);
                        lista.add(new Categoria(nome, cor != null ? cor : "#888888"));
                    }
                } while (cursor.moveToNext());
            }
        }
        return lista;
    }

    private void limparCampos() {
        inputNomeDespesaCartao.setText("");
        inputDataDespesaCartao.setText("");
        inputValorDespesa.setText("");
        autoCompleteCategoria.setText("");
    }

    private void openDatePicker() {
        final java.util.Calendar calendar = java.util.Calendar.getInstance();

        int year = calendar.get(java.util.Calendar.YEAR);
        int month = calendar.get(java.util.Calendar.MONTH);
        int day = calendar.get(java.util.Calendar.DAY_OF_MONTH);

        android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(requireContext(),
                (view, selectedYear, selectedMonth, selectedDayOfMonth) -> {
                    String dataSelecionada = String.format("%02d/%02d/%04d", selectedDayOfMonth, selectedMonth + 1, selectedYear);
                    inputDataDespesaCartao.setText(dataSelecionada);
                }, year, month, day);

        datePickerDialog.show();
    }

    private static class SimpleTextWatcher implements android.text.TextWatcher {
        private final Runnable callback;
        public SimpleTextWatcher(Runnable callback) { this.callback = callback; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (callback != null) callback.run();
        }
        @Override public void afterTextChanged(android.text.Editable s) {}
    }

    // Classe modelo Categoria
    public static class Categoria {
        public String nome;
        public String cor;

        public Categoria(String nome, String cor) {
            this.nome = nome;
            this.cor = cor;
        }
    }

    // Adapter customizado
    public class CategoriasDropdownAdapter extends android.widget.ArrayAdapter<Categoria> {
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
}