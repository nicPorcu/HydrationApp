package com.example.per6.hydrationapp.leaderboard;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.per6.hydrationapp.leaderboard.LeaderboardFragment.OnListFragmentInteractionListener;
import com.example.per6.hydrationapp.leaderboard.LeaderboardDisplayerContent.leaderboardDisplayerItem;
import com.example.per6.hydrationapp.R;
import com.example.per6.hydrationapp.User;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link leaderboardDisplayerItem} and makes a call to the
 * specified {@link OnListFragmentInteractionListener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class MyFragmentLeaderboardRecyclerViewAdapter extends RecyclerView.Adapter<MyFragmentLeaderboardRecyclerViewAdapter.ViewHolder> {

    private final List<User> users;
    private final OnListFragmentInteractionListener mListener;

    public MyFragmentLeaderboardRecyclerViewAdapter(List<User> items, OnListFragmentInteractionListener listener) {
        users = items;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_fragmentleaderboard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        User user= users.get(position);
        holder.mIdView.setText(users.get(position).getName());
        holder.mContentView.setText(users.get(position).getCurrentStreak());

        holder.mView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mListener) {
                    // Notify the active callbacks interface (the activity, if the
                    // fragment is attached to one) that an item has been selected.
                    //mListener.onListFragmentInteraction(holder.mItem);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mIdView;
        public final TextView mContentView;
        public leaderboardDisplayerItem mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mIdView = (TextView) view.findViewById(R.id.item_number);
            mContentView = (TextView) view.findViewById(R.id.content);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }
}
