package com.example.app1.utils;

import android.content.Context;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class MenuHelper {

    public interface MenuItemClickListener {
        void onClick();
    }

    public static class MenuItemData {
        public String title;
        public int iconResId; // drawable
        public MenuItemClickListener listener;

        public MenuItemData(String title, int iconResId, MenuItemClickListener listener) {
            this.title = title;
            this.iconResId = iconResId;
            this.listener = listener;
        }
    }

    public static void showMenu(Context context, View anchor, MenuItemData[] items) {
        PopupMenu popup = new PopupMenu(context, anchor);

        // Adiciona os itens dinamicamente
        for (int i = 0; i < items.length; i++) {
            MenuItem item = popup.getMenu().add(0, i, i, items[i].title);
            if (items[i].iconResId != 0) {
                item.setIcon(items[i].iconResId);
            }
        }

        // Forçar exibir ícones
        try {
            Field[] fields = popup.getClass().getDeclaredFields();
            for (Field field : fields) {
                if ("mPopup".equals(field.getName())) {
                    field.setAccessible(true);
                    Object menuPopupHelper = field.get(popup);
                    Class<?> classPopupHelper = Class.forName(menuPopupHelper.getClass().getName());
                    Method setForceIcons = classPopupHelper.getMethod("setForceShowIcon", boolean.class);
                    setForceIcons.invoke(menuPopupHelper, true);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id >= 0 && id < items.length && items[id].listener != null) {
                items[id].listener.onClick();
            }
            return true;
        });

        popup.show();
    }
}