package com.example.per6.hydrationapp;

import android.content.Context;
import android.content.DialogInterface;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;


public class WaterBottleAdapter extends RecyclerView.Adapter<WaterBottleAdapter.ViewHolder>{
    private RecyclerViewOnClick click;
    private final List<WaterBottle> waterBottles;
    private Context context;
    private View rootview;
    public static final String TAG= "WaterBottleAdapter";
    private WaterBottle mRecentlyDeletedItem;
    private int mRecentlyDeletedItemPosition;



    public WaterBottleAdapter(List<WaterBottle> items, Context context, RecyclerViewOnClick click) {
        waterBottles = items;
        this.click=click;
        this.context=context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        rootview = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.water_bottle_info_item, viewGroup, false);
        return new ViewHolder(rootview, click);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder: "+position);
        Log.d(TAG, "onBindViewHolder: "+ holder.getAdapterPosition());

            holder.mNameView.setText(waterBottles.get(position).getBottleName());
            holder.mCapacityView.setText(waterBottles.get(position).getCapacity()+"oz");




        }



    @Override
    public int getItemCount() {
        return waterBottles.size();
    }

    public List<WaterBottle> getWaterBottles() {
        return waterBottles;
    }

    public Context getContext(){
        return context;
    }







    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public  View mView;
        private RecyclerViewOnClick recyclerViewOnClick;

        private TextView mNameView;
        private TextView mCapacityView;



        public ViewHolder(View view, RecyclerViewOnClick click) {
            super(view);
            mView = view;
            Log.d(TAG, "ViewHolder: hi");
            mNameView = (TextView) view.findViewById(R.id.water_bottle_name);
            mCapacityView= view.findViewById(R.id.bottle_capacity);
            recyclerViewOnClick=click;
            itemView.setOnClickListener(this);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mNameView.getText() + "'";
        }

        @Override
        public void onClick(View v) {
                recyclerViewOnClick.onClick(v, getAdapterPosition());
        }
    }
}
