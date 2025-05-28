package com.example.app7;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ContactoAdapter extends RecyclerView.Adapter<ContactoAdapter.ViewHolder> {
    private final List<Contacto> contactos;
    private final OnContactoClickListener listener;

    public interface OnContactoClickListener {
        void onContactoClick(Contacto contacto);
    }

    public ContactoAdapter(List<Contacto> contactos, OnContactoClickListener listener) {
        this.contactos = contactos;
        this.listener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Contacto contacto = contactos.get(position);
        holder.text.setText(contacto.getNombre());
        holder.itemView.setOnClickListener(v -> listener.onContactoClick(contacto));
    }

    @Override
    public int getItemCount() {
        return contactos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView text;
        public ViewHolder(View itemView) {
            super(itemView);
            text = itemView.findViewById(android.R.id.text1);
        }
    }
}