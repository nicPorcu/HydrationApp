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
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.persistence.DataQueryBuilder;
import com.backendless.persistence.LoadRelationsQueryBuilder;

import java.util.ArrayList;
import java.util.List;


public class WaterBottleAdapter extends RecyclerView.Adapter<WaterBottleAdapter.ViewHolder>{
    private RecyclerViewOnClick click;
    private final List<WaterBottle> waterBottles;
    int mSelectedItem=-1;
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
        holder.isCurrentBottleCheckbox.setChecked(position == mSelectedItem);

        //radioGroup.addView(holder.isCurrentBottleCheckbox);



//        LoadRelationsQueryBuilder<BackendlessUser> loadRelationsQueryBuilder;
//        loadRelationsQueryBuilder = LoadRelationsQueryBuilder.of( BackendlessUser.class );
//        loadRelationsQueryBuilder.setRelationName( "currentWaterBottle" );
//         Backendless.Data.of( BackendlessUser.class ).loadRelations( Backendless.UserService.CurrentUser().getObjectId(), loadRelationsQueryBuilder, new AsyncCallback<List<BackendlessUser>>() {
//
//             @Override
//             public void handleResponse(List<BackendlessUser> response) {
//                 if(response.size()!=0) {
//                     Log.d(TAG, "handleResponse: "+response.get(0));
//                    // Log.d(TAG, "onBindViewHolder: " + response.get(0).getProperty("currentWaterBottle"));
//                 }
//             }
//
//             @Override
//            public void handleFault(BackendlessFault fault) {
//                 Log.d(TAG, "handleFault: "+fault.getMessage());
//
//            }
//        });
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
        private RadioButton isCurrentBottleCheckbox;



        public ViewHolder(View view, RecyclerViewOnClick click) {
            super(view);
            mView = view;
            Log.d(TAG, "ViewHolder: hi");
            mNameView = (TextView) view.findViewById(R.id.water_bottle_name);
            mCapacityView= view.findViewById(R.id.bottle_capacity);
            isCurrentBottleCheckbox=view.findViewById(R.id.is_current_bottle_checkbox);
            recyclerViewOnClick=click;
            isCurrentBottleCheckbox.setOnClickListener(this);
            itemView.setOnClickListener(this);

        }

        @Override
        public String toString() {
            return super.toString() + " '" + mNameView.getText() + "'";
        }

        @Override
        public void onClick(View v) {

                if  (v.getId()==R.id.is_current_bottle_checkbox){
                         mSelectedItem=getAdapterPosition();
                        recyclerViewOnClick.setCurrentBottle(v, getAdapterPosition());
                        notifyDataSetChanged();

                }else{
                    recyclerViewOnClick.onClick(v, getAdapterPosition());

                }


        }
    }
}
