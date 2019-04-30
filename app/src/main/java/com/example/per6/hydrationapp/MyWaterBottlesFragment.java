package com.example.per6.hydrationapp;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
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

import weborb.util.ObjectProperty;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MyWaterBottlesFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MyWaterBottlesFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MyWaterBottlesFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = "WaterBottlesFragment";
    private WaterBottleAdapter adapter;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private List<WaterBottle> waterBottleList;
    private RecyclerView recyclerView;
    private View rootView;

    private OnFragmentInteractionListener mListener;

    public MyWaterBottlesFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MyWaterBottlesFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MyWaterBottlesFragment newInstance(String param1, String param2) {
        MyWaterBottlesFragment fragment = new MyWaterBottlesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_user_info, container, false);

        wireWidgets();
        displayBottles();
        // Inflate the layout for this fragment
        return rootView;
    }

    private void displayBottles() {
        String query= "h's water bottle";
        StringBuilder whereClause = new StringBuilder();
        whereClause.append("waterBottle like '%" + query + "%'");
        DataQueryBuilder queryBuilder = DataQueryBuilder.create();
        queryBuilder.setWhereClause(whereClause.toString());
        Backendless.Data.of(WaterBottle.class).find( queryBuilder, new AsyncCallback<List<WaterBottle>>(){

            @Override
            public void handleResponse(List<WaterBottle> response) {
                waterBottleList.addAll(response);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                Log.d(TAG, "handleFault: " + fault.getMessage());

                Toast.makeText(getContext(), fault.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void wireWidgets() {
        waterBottleList=new ArrayList<>();
        recyclerView=rootView.findViewById(R.id.water_bottle_recycler_view);

    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
//        if (context instanceof OnFragmentInteractionListener) {
//            mListener = (OnFragmentInteractionListener) context;
//        } else {
//            throw new RuntimeException(context.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
