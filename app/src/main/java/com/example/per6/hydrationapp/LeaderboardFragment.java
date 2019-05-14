package com.example.per6.hydrationapp;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import static com.example.per6.hydrationapp.ConnectedPeripheralFragment.createFragmentArgs;

public class LeaderboardFragment extends Fragment {

    private static final String ARG_COLUMN_COUNT = "column-count";
    private static String singlePeripheralIdentifierMaster;

    public LeaderboardFragment() {
    }

    public static LeaderboardFragment newInstance(@Nullable String peripheralIdentifier) {
        LeaderboardFragment fragment = new LeaderboardFragment();
        fragment.setArguments(createFragmentArgs(peripheralIdentifier));
        singlePeripheralIdentifierMaster = peripheralIdentifier;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fragment_leaderboard_list, container, false);
        return view;
    }
}
