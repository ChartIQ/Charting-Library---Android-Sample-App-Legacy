package com.chartiq.chartiqsample;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ColorAdapter extends RecyclerView.Adapter {

    Context context;
    String[] colors;
    View.OnClickListener onClickListener;

    public ColorAdapter(Context context, int arrayId, View.OnClickListener listener) {
        this.context = context;
        colors = context.getResources().getStringArray(arrayId);
        onClickListener = listener;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.color, parent, false);
        view.setOnClickListener(onClickListener);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        TextView textView = (TextView) holder.itemView.findViewById(R.id.textview);
        textView.setBackgroundColor(Color.parseColor(colors[position]));
        textView.setTag(colors[position]);
    }

    @Override
    public int getItemCount() {
        return colors.length;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView textView;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView;
        }
    }
}
