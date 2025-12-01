package com.example.app1;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.app1.data.CartaoDAO;
import com.example.app1.data.ContaDAO;
import com.example.app1.data.TransacoesCartaoDAO;
import com.example.app1.utils.MascaraMonetaria;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.skydoves.colorpickerview.ColorPickerDialog;
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class fragment_cad_cartao extends Fragment {

    private static final String ARG_ID_USUARIO = "id_usuario";
    private static final String ARG_ID_CARTAO  = "id_cartao";

    private int idUsuarioLogado;
    private Integer idCartaoEdicao;
    private List<Conta> listaDeContas = new ArrayList<>();

    private View overlay;
    private LinearLayout slidingMenu;
    private FloatingActionButton btnAddConta;

    public static fragment_cad_cartao newInstance(int idUsuario, @Nullable Integer idCartao) {
        Bundle args = new Bundle();
        args.putInt(ARG_ID_USUARIO, idUsuario);
        if (idCartao != null) args.putInt(ARG_ID_CARTAO, idCartao);
        fragment_cad_cartao f = new fragment_cad_cartao();
        f.setArguments(args);
        return f;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cad_cartao, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            idUsuarioLogado = args.getInt(ARG_ID_USUARIO, -1);
            idCartaoEdicao  = args.containsKey(ARG_ID_CARTAO)
                    ? args.getInt(ARG_ID_CARTAO) : null;
        }

        overlay     = view.findViewById(R.id.overlay);
        slidingMenu = view.findViewById(R.id.slidingMenu);
        btnAddConta = view.findViewById(R.id.btnAddConta);

        setupListeners(view);
        carregarDropdowns(view);

        if (idCartaoEdicao != null) {
            preencherCamposEdicao(view, idCartaoEdicao);
        } else {
            limparCampos(view);
        }
    }

    private void setupListeners(View view) {
        // Fechar ao clicar no overlay
        overlay.setOnClickListener(v -> fecharFragment());

        // Botão adicionar conta abre MenuCadContaFragment na Activity
        btnAddConta.setOnClickListener(v -> {
            if (getActivity() == null) return;
            FrameLayout container = getActivity().findViewById(R.id.containerFragment);
            if (container != null) container.setVisibility(View.VISIBLE);

            MenuCadContaFragment fragment = MenuCadContaFragment.newInstance(idUsuarioLogado);
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.containerFragment, fragment)
                    .addToBackStack(null)
                    .commit();
        });

        TextInputEditText inputCor = view.findViewById(R.id.inputCor);
        inputCor.setOnClickListener(v -> {
            new ColorPickerDialog.Builder(requireContext())
                    .setTitle("Escolha uma cor")
                    .setPositiveButton("Selecionar",
                            (ColorEnvelopeListener) (envelope, fromUser) -> {
                                String hex = "#" + envelope.getHexCode();
                                inputCor.setText(hex);
                                inputCor.setTextColor(envelope.getColor());
                            })
                    .setNegativeButton("Cancelar",
                            (dialogInterface, i) -> dialogInterface.dismiss())
                    .attachAlphaSlideBar(true)
                    .attachBrightnessSlideBar(true)
                    .setBottomSpace(12)
                    .show();
        });

        TextInputEditText inputLimite = view.findViewById(R.id.inputLimite);
        inputLimite.addTextChangedListener(new MascaraMonetaria(inputLimite));

        Button btnSalvar = view.findViewById(R.id.btnSalvarCartao);
        btnSalvar.setOnClickListener(v -> salvarCartao(view));
    }

    private void carregarDropdowns(View view) {
        // Bandeiras
        MaterialAutoCompleteTextView autoCompleteBandeira =
                view.findViewById(R.id.autoCompleteBandeira);
        String[] bandeiras = {"Visa", "Mastercard", "Elo", "American Express", "Hipercard", "Outros"};
        ArrayAdapter<String> bandeirasAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, bandeiras);
        autoCompleteBandeira.setAdapter(bandeirasAdapter);

        // Contas
        MaterialAutoCompleteTextView autoCompleteConta =
                view.findViewById(R.id.autoCompleteConta);
        listaDeContas = ContaDAO.carregarListaContas(requireContext(), idUsuarioLogado);
        ArrayAdapter<Conta> contasAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_dropdown_item_1line, listaDeContas);
        autoCompleteConta.setAdapter(contasAdapter);
    }

    private void preencherCamposEdicao(View view, int idCartao) {
        ((TextView) view.findViewById(R.id.tituloSlidingMenu))
                .setText("Editar Cartão de Crédito");
        ((Button) view.findViewById(R.id.btnSalvarCartao))
                .setText("Salvar Alterações");

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            TelaCadCartao.CartaoModel cartao = CartaoDAO.buscarCartaoPorId(requireContext(), idCartao);
            handler.post(() -> {
                if (cartao == null) return;

                ((TextInputEditText) view.findViewById(R.id.inputNomeCartao))
                        .setText(cartao.nome);
                ((MaterialAutoCompleteTextView) view.findViewById(R.id.autoCompleteBandeira))
                        .setText(cartao.bandeira, false);
                ((TextInputEditText) view.findViewById(R.id.inputLimite))
                        .setText(String.format(Locale.GERMAN, "%.2f", cartao.limite));
                ((TextInputEditText) view.findViewById(R.id.inputDiaVencimento))
                        .setText(String.valueOf(cartao.diaVencimento));
                ((TextInputEditText) view.findViewById(R.id.inputDiaFechamento))
                        .setText(String.valueOf(cartao.diaFechamento));
                ((TextInputEditText) view.findViewById(R.id.inputCor))
                        .setText(cartao.cor);

                RadioButton radioAtivo = view.findViewById(R.id.radioAtivo);
                RadioButton radioDesativado = view.findViewById(R.id.radioDesativado);
                if (cartao.ativo == 1) radioAtivo.setChecked(true);
                else radioDesativado.setChecked(true);

                MaterialAutoCompleteTextView autoCompleteConta =
                        view.findViewById(R.id.autoCompleteConta);
                for (int i = 0; i < listaDeContas.size(); i++) {
                    if (listaDeContas.get(i).getId() == cartao.idConta) {
                        autoCompleteConta.setText(listaDeContas.get(i).getNome(), false);
                        break;
                    }
                }
            });
        });
    }

    private void limparCampos(View view) {
        ((TextInputEditText) view.findViewById(R.id.inputNomeCartao)).setText("");
        ((MaterialAutoCompleteTextView) view.findViewById(R.id.autoCompleteBandeira))
                .setText("", false);
        ((MaterialAutoCompleteTextView) view.findViewById(R.id.autoCompleteConta))
                .setText("", false);
        ((TextInputEditText) view.findViewById(R.id.inputLimite)).setText("");
        ((TextInputEditText) view.findViewById(R.id.inputDiaVencimento)).setText("");
        ((TextInputEditText) view.findViewById(R.id.inputDiaFechamento)).setText("");
        ((TextInputEditText) view.findViewById(R.id.inputCor)).setText("");
        ((RadioButton) view.findViewById(R.id.radioAtivo)).setChecked(true);

        ((TextView) view.findViewById(R.id.tituloSlidingMenu))
                .setText("Adicionar Cartão de Crédito");
        ((Button) view.findViewById(R.id.btnSalvarCartao))
                .setText("Salvar Cartão");
    }

    private void salvarCartao(View view) {
        Context ctx = requireContext();

        String nome = ((TextInputEditText) view.findViewById(R.id.inputNomeCartao))
                .getText().toString().trim();
        String bandeira = ((MaterialAutoCompleteTextView) view.findViewById(R.id.autoCompleteBandeira))
                .getText().toString().trim();
        String limiteStr = ((TextInputEditText) view.findViewById(R.id.inputLimite))
                .getText().toString().replaceAll("[^0-9,.]", "").replace(".", "").replace(",", ".");
        String vencimentoStr = ((TextInputEditText) view.findViewById(R.id.inputDiaVencimento))
                .getText().toString().trim();
        String fechamentoStr = ((TextInputEditText) view.findViewById(R.id.inputDiaFechamento))
                .getText().toString().trim();
        String cor = ((TextInputEditText) view.findViewById(R.id.inputCor))
                .getText().toString().trim();
        String nomeConta = ((MaterialAutoCompleteTextView) view.findViewById(R.id.autoCompleteConta))
                .getText().toString().trim();

        RadioGroup radioGroupAtivo = view.findViewById(R.id.radioGroupAtivo);
        boolean ativo = radioGroupAtivo.getCheckedRadioButtonId() == R.id.radioAtivo;

        if (TextUtils.isEmpty(nome) || TextUtils.isEmpty(vencimentoStr)
                || TextUtils.isEmpty(fechamentoStr) || TextUtils.isEmpty(nomeConta)) {
            Toast.makeText(ctx, "Preencha todos os campos obrigatórios", Toast.LENGTH_SHORT).show();
            return;
        }

        int idConta = -1;
        for (Conta conta : listaDeContas) {
            if (conta.getNome().equals(nomeConta)) {
                idConta = conta.getId();
                break;
            }
        }
        if (idConta == -1) {
            Toast.makeText(ctx, "Conta inválida", Toast.LENGTH_SHORT).show();
            return;
        }

        double limite = TextUtils.isEmpty(limiteStr) ? 0.0 : Double.parseDouble(limiteStr);
        int vencimento = Integer.parseInt(vencimentoStr);
        int fechamento = Integer.parseInt(fechamentoStr);

        MeuDbHelper dbHelper = new MeuDbHelper(ctx);
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Integer fechamentoAntigo = null;
        Integer vencimentoAntigo = null;

        if (idCartaoEdicao != null) {
            Cursor cursor = db.query("cartoes",
                    new String[]{"data_fechamento_fatura", "data_vencimento_fatura"},
                    "id = ?", new String[]{String.valueOf(idCartaoEdicao)},
                    null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    fechamentoAntigo = cursor.getInt(cursor.getColumnIndexOrThrow("data_fechamento_fatura"));
                    vencimentoAntigo = cursor.getInt(cursor.getColumnIndexOrThrow("data_vencimento_fatura"));
                }
                cursor.close();
            }
        }

        ContentValues values = new ContentValues();
        values.put("nome", nome);
        values.put("bandeira", bandeira);
        values.put("limite", limite);
        values.put("data_vencimento_fatura", vencimento);
        values.put("data_fechamento_fatura", fechamento);
        values.put("cor", cor);
        values.put("ativo", ativo ? 1 : 0);
        values.put("id_usuario", idUsuarioLogado);
        values.put("id_conta", idConta);

        boolean sucesso;
        if (idCartaoEdicao == null) {
            long newRowId = db.insert("cartoes", null, values);
            sucesso = newRowId != -1;
        } else {
            int rowsAffected = db.update("cartoes", values,
                    "id = ?", new String[]{String.valueOf(idCartaoEdicao)});
            sucesso = rowsAffected > 0;

            if (sucesso && fechamentoAntigo != null && fechamentoAntigo != fechamento) {
                TransacoesCartaoDAO dao = new TransacoesCartaoDAO();
                dao.reprocessarFaturasAbertasPorFechamento(db,
                        idCartaoEdicao, fechamentoAntigo, fechamento, vencimento);
            }
        }

        if (sucesso) {
            Toast.makeText(ctx, "Cartão salvo com sucesso!", Toast.LENGTH_SHORT).show();
            // avisa Activity para recarregar lista
            if (getParentFragmentManager() != null) {
                Bundle result = new Bundle();
                result.putBoolean("atualizar", true);
                getParentFragmentManager().setFragmentResult("despesaSalvaRequest", result);
            }
            fecharFragment();
        } else {
            Toast.makeText(ctx, "Erro ao salvar o cartão.", Toast.LENGTH_SHORT).show();
        }
    }

    private void fecharFragment() {
        if (getActivity() == null) return;
        // esconder container na Activity
        FrameLayout container = getActivity().findViewById(R.id.containerFragment);
        if (container != null) container.setVisibility(View.GONE);
        requireActivity().getSupportFragmentManager().popBackStack();
    }
}
