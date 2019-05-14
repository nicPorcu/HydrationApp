package com.example.per6.hydrationapp;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.backendless.BackendlessUser;

import java.util.List;

/**
 * TODO: Replace the implementation with code for your data type.
 */
public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private final List<BackendlessUser> users;
    private Context context;

    public UserAdapter(List<BackendlessUser> items, Context context) {
        users = items;
        this.context=context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.user_info_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.usernameView.setText((String)users.get(position).getProperty("name"));
        holder.daysLoggedView.setText((Integer)users.get(position).getProperty("daysLogged")+" day streak");


    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView usernameView;

        public final TextView daysLoggedView;

        public ViewHolder(View view) {
            super(view);
            usernameView = view.findViewById(R.id.username_view);
            daysLoggedView = (TextView) view.findViewById(R.id.days_logged_view);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + daysLoggedView.getText() + "'";
        }
    }
}
