package com.example.app1.utils;

import android.app.Activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentActivity;

import com.example.app1.MenuCadDespesaCartaoFragment;

import com.example.app1.MenuCadDespesaFragment;
import com.example.app1.R;
public class MenuBottomUtils {

    // Método estático para abrir o fragment do menu
    public static void abrirMenuCadDespesaCartao(AppCompatActivity activity, int idUsuario, int idCartao) {
        MenuCadDespesaCartaoFragment fragment = MenuCadDespesaCartaoFragment.newInstance(idUsuario, idCartao);
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_abrirmenubottom, fragment)
                .addToBackStack(null)
                .commit();
    }

    public static void abrirMenuCadDespesa(AppCompatActivity activity, int idUsuario) {
        MenuCadDespesaFragment fragment = MenuCadDespesaFragment.newInstance(idUsuario);
        activity.getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_abrirmenubottom, fragment)
                .addToBackStack(null)
                .commit();
    }

}