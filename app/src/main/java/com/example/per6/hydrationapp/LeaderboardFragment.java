package com.example.per6.hydrationapp;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.backendless.Backendless;
import com.backendless.BackendlessUser;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.persistence.DataQueryBuilder;

import java.util.ArrayList;
import java.util.List;

import static com.example.per6.hydrationapp.ConnectedPeripheralFragment.createFragmentArgs;


public class LeaderboardFragment extends Fragment {

    private static final String TAG = "LeaderboardFragment";

    private static final String ARG_COLUMN_COUNT = "column-count";
    private static String singlePeripheralIdentifierMaster;
    private int mColumnCount = 1;

    private UserAdapter adapter;
    private RecyclerView recyclerView;
    private List<BackendlessUser> userList;
    private View rootView;


    public LeaderboardFragment() {
    }

    public static LeaderboardFragment newInstance(@Nullable String singlePeripheralIdentifier) {
        LeaderboardFragment fragment = new LeaderboardFragment();
        fragment.setArguments(createFragmentArgs(singlePeripheralIdentifier));
        singlePeripheralIdentifierMaster = singlePeripheralIdentifier;
        return fragment;
    }

    // TODO: Customize parameter initialization
    @SuppressWarnings("unused")
    public static LeaderboardFragment newInstance(int columnCount) {
        LeaderboardFragment fragment = new LeaderboardFragment();
        Bundle args = new Bundle();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mColumnCount = getArguments().getInt(ARG_COLUMN_COUNT);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_leaderboard, container, false);
        wireWidgets();

        getUsers();
        // Set the adapter
        return rootView;

    }

    private void getUsers() {


        DataQueryBuilder queryBuilder = DataQueryBuilder.create();
        queryBuilder.setSortBy(  "daysLogged DESC" );
        Backendless.Data.of( BackendlessUser.class ).find( queryBuilder,
                new AsyncCallback<List<BackendlessUser>>()
                {
                    @Override
                    public void handleResponse( List<BackendlessUser> response )
                    {
                        userList.addAll(response);
                        adapter.notifyDataSetChanged();
                        // the "response" object is a collection of Person objects.
                        // each item in the collection represents an object from the "Person" table
                    }

                    @Override
                    public void handleFault( BackendlessFault fault )
                    {
                        Log.d(TAG, "handleFault: " + fault.getMessage());

                        // use the getCode(), getMessage() or getDetail() on the fault object
                        // to see the details of the error
                    }
                });
    }

    private void wireWidgets () {
        userList=new ArrayList<>();
        recyclerView = rootView.findViewById(R.id.leaderboard_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new UserAdapter( userList, getContext());
        recyclerView.setAdapter(adapter);




    }
}
