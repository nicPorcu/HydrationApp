package com.example.per6.hydrationapp;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.backendless.Backendless;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.persistence.DataQueryBuilder;


import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;
import static com.example.per6.hydrationapp.ConnectedPeripheralFragment.createFragmentArgs;


public class MyWaterBottlesFragment extends Fragment {

    private static final String TAG = "WaterBottlesFragment";
    private static String singlePeripheralIdentifierMaster;
    private WaterBottleAdapter adapter;
    private LinearLayoutManager layoutManager;

    private List<WaterBottle> waterBottleList;
    private RecyclerView recyclerView;
    private View rootView;
    private RecyclerViewOnClick click;
    private FloatingActionButton addButton;
    private int requestCode;


    public MyWaterBottlesFragment() {
        // Required empty public constructor
    }

    public static MyWaterBottlesFragment newInstance(@Nullable String singlePeripheralIdentifier) {
        MyWaterBottlesFragment fragment = new MyWaterBottlesFragment();
        fragment.setArguments(createFragmentArgs(singlePeripheralIdentifier));
        singlePeripheralIdentifierMaster = singlePeripheralIdentifier;
        return fragment;
    }


    public void areYaSure(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.dialog_message);

        builder.setPositiveButton(R.string.yes, (dialog, id) -> deleteItem(position));
        builder.setNegativeButton(R.string.no, (dialog, which) -> { getBottles();});
        AlertDialog dialog = builder.create();
        dialog.show();

    }

    public void deleteItem(int position) {
        WaterBottle b= waterBottleList.remove(position);
        adapter.notifyDataSetChanged();
        Backendless.Persistence.of( WaterBottle.class ).remove( b,
                new AsyncCallback<Long>()
                {
                    public void handleResponse( Long response )
                    {
                        // Contact has been deleted. The response is the
                        // time in milliseconds when the object was deleted
                    }
                    public void handleFault( BackendlessFault fault )
                    {
                        // an error has occurred, the error code can be
                        // retrieved with fault.getCode()
                    }
                } );        Log.d(TAG, "deleteItem: ");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_user_info, container, false);

        wireWidgets();
        getBottles();
        // Inflate the layout for this fragment
        return rootView;
    }

    private void getBottles() {
        waterBottleList.clear();
        String query= "ownerId= '"+ Backendless.UserService.CurrentUser().getUserId()+"'";
        StringBuilder whereClause = new StringBuilder();
        whereClause.append( query);
        DataQueryBuilder queryBuilder = DataQueryBuilder.create();
        queryBuilder.setWhereClause(whereClause.toString());
        Backendless.Data.of(WaterBottle.class).find( queryBuilder, new AsyncCallback<List<WaterBottle>>(){

            @Override
            public void handleResponse(List<WaterBottle> response) {
                waterBottleList.addAll(response);
                Log.d(TAG, "handleResponse: "+ waterBottleList.toString());
                adapter.notifyDataSetChanged();
                Log.d(TAG, "handleResponse: stuff up");
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                Log.d(TAG, "handleFault: " + fault.getMessage());

                Toast.makeText(getContext(), fault.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void wireWidgets() {
        waterBottleList = new ArrayList<>();
        requestCode=0;
        recyclerView = rootView.findViewById(R.id.water_bottle_recycler_view);
        addButton=rootView.findViewById(R.id.add_button);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), BottleEditorActivity.class);
                intent.putExtra("editMode", true);
                intent.putExtra("waterBottle", new WaterBottle());
                intent.putExtra("singlePeripheralIdentifier", singlePeripheralIdentifierMaster);
                startActivityForResult(intent, requestCode);

            }
        });

        click = new RecyclerViewOnClick() {
            @Override
            public void onClick(View v, int pos) {
                Log.d(TAG, "onClick: made it to 1");
                Intent intent=new Intent(getContext(), BottleEditorActivity.class);
                intent.putExtra("editMode", false);
                intent.putExtra("waterBottle", waterBottleList.get(pos));
                intent.putExtra("singlePeripheralIdentifier", singlePeripheralIdentifierMaster);
                startActivityForResult(intent,requestCode);
                Log.d(TAG, "onClick: made it to 2");

            }

        };
        layoutManager = new LinearLayoutManager(getContext());
        adapter = new WaterBottleAdapter( waterBottleList, getContext(), click);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new SwipeToDeleteCallback(adapter, this));
        itemTouchHelper.attachToRecyclerView(recyclerView);
        registerForContextMenu(recyclerView);
    }
    //work plz



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: "+"activity result");
        if(this.requestCode == requestCode && resultCode == Activity.RESULT_OK){
            getBottles();

        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
