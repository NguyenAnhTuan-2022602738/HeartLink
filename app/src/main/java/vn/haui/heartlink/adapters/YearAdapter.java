package vn.haui.heartlink.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import vn.haui.heartlink.R;

public class YearAdapter extends RecyclerView.Adapter<YearAdapter.YearViewHolder> {

    private final List<Integer> years;
    private final OnYearSelectedListener listener;

    public interface OnYearSelectedListener {
        void onYearSelected(int year);
    }

    public YearAdapter(List<Integer> years, OnYearSelectedListener listener) {
        this.years = years;
        this.listener = listener;
    }

    @NonNull
    @Override
    public YearViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_year, parent, false);
        return new YearViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull YearViewHolder holder, int position) {
        int year = years.get(position);
        holder.yearText.setText(String.valueOf(year));
        holder.itemView.setOnClickListener(v -> listener.onYearSelected(year));
    }

    @Override
    public int getItemCount() {
        return years.size();
    }

    static class YearViewHolder extends RecyclerView.ViewHolder {
        TextView yearText;

        public YearViewHolder(@NonNull View itemView) {
            super(itemView);
            yearText = itemView.findViewById(R.id.year_text);
        }
    }
}