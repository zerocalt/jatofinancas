package com.example.app1.utils;

import android.content.Context;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import androidx.appcompat.widget.PopupMenu;

public class MenuHelper {

    public interface MenuItemClickListener {
        void onClick();
    }

    public static class MenuItemData {
        public String title;
        public int iconResId;
        public MenuItemClickListener listener;

        public MenuItemData(String title, int iconResId, MenuItemClickListener listener) {
            this.title = title;
            this.iconResId = iconResId;
            this.listener = listener;
        }
    }

    public static void showMenu(Context context, View anchor, MenuItemData[] items) {
        Log.d("MENU_DEBUG", "5. Entrou no MenuHelper.showMenu.");
        PopupMenu popup = new PopupMenu(context, anchor);
        Log.d("MENU_DEBUG", "6. Objeto PopupMenu criado.");

        // Força a exibição de ícones, mesmo que o tema não o faça por padrão.
        try {
            java.lang.reflect.Field popupField = popup.getClass().getDeclaredField("mPopup");
            popupField.setAccessible(true);
            Object menuPopupHelper = popupField.get(popup);
            Class<?> helperClass = Class.forName(menuPopupHelper.getClass().getName());
            java.lang.reflect.Method setForceIcons = helperClass.getMethod("setForceShowIcon", boolean.class);
            setForceIcons.invoke(menuPopupHelper, true);
        } catch (Exception e) {
            Log.e("MENU_HELPER", "Erro ao forçar ícones", e);
        }

        for (int i = 0; i < items.length; i++) {
            MenuItem item = popup.getMenu().add(0, i, i, items[i].title);
            if (items[i].iconResId != 0) {
                item.setIcon(items[i].iconResId);
            }
        }

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id >= 0 && id < items.length && items[id].listener != null) {
                items[id].listener.onClick();
            }
            return true;
        });
        
        Log.d("MENU_DEBUG", "7. Chamando popup.show()...");
        popup.show();
    }
}
