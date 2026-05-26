package com.example.guiterapp_1;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.guiterapp_1.databinding.ItemLibraryMenuBinding;

import java.util.List;

public class LibraryMenuAdapter extends RecyclerView.Adapter<LibraryMenuAdapter.ViewHolder> {

    private final List<LibraryMenuItem> items;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(LibraryMenuItem item);
    }

    public LibraryMenuAdapter(List<LibraryMenuItem> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemLibraryMenuBinding binding = ItemLibraryMenuBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LibraryMenuItem item = items.get(position);
        holder.binding.tvMenuTitle.setText(item.getTitle());
        holder.binding.ivMenuIcon.setImageResource(item.getIconResId());
        holder.itemView.setOnClickListener(v -> listener.onItemClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemLibraryMenuBinding binding;

        ViewHolder(ItemLibraryMenuBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
