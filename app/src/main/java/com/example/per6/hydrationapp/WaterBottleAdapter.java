package com.example.per6.hydrationapp;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class WaterBottleAdapter extends RecyclerView.Adapter<WaterBottleAdapter.ViewHolder>{
    private RecyclerViewOnClick click;
    private final List<WaterBottle> waterBottles;
    private Context context;



    public WaterBottleAdapter(List<WaterBottle> items, Context context, RecyclerViewOnClick click) {
        waterBottles = items;
        this.click=click;
        this.context=context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.fragment_user_info, viewGroup, false);
        return new ViewHolder(view, click);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WaterBottle bottle= waterBottles.get(position);
        holder.mIdView.setText(position);
        holder.mContentView.setText(waterBottles.get(position).getBottleName());


        }



    @Override
    public int getItemCount() {
        return waterBottles.size();
    }


    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public final View mView;
        private RecyclerViewOnClick recyclerViewOnClick;
        private final TextView mIdView;
        private final TextView mContentView;



        public ViewHolder(View view, RecyclerViewOnClick click) {
            super(view);
            mView = view;
            mIdView = (TextView) view.findViewById(R.id.item_number);
            mContentView = (TextView) view.findViewById(R.id.content);
            recyclerViewOnClick=click;
            itemView.setOnClickListener(this);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }

        @Override
        public void onClick(View v) {
                recyclerViewOnClick.onClick(v, getAdapterPosition());
        }
    }
}
