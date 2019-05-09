package com.example.per6.hydrationapp;

import android.content.Context;
import android.os.ParcelUuid;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.per6.hydrationapp.ble.BlePeripheral;
import com.example.per6.hydrationapp.style.RssiUI;
import com.example.per6.hydrationapp.utils.KeyboardUtils;
import com.example.per6.hydrationapp.utils.LocalizationManager;

import java.lang.ref.WeakReference;
import java.util.List;

class BlePeripheralsAdapter extends RecyclerView.Adapter<BlePeripheralsAdapter.ViewHolder> {
    /*Shows all devices*/
    // Constants
    @SuppressWarnings("unused")
    private final static String TAG = BlePeripheralsAdapter.class.getSimpleName();

    interface Listener {
        void onAdvertisementData(@NonNull BlePeripheral blePeripheral);
    }

    // Data
    private List<BlePeripheral> mBlePeripherals;
    private Context mContext;
    private RecyclerView mRecyclerView;
    private Listener mListener;

    BlePeripheralsAdapter(@NonNull Context context, @NonNull Listener listener) {
        mContext = context.getApplicationContext();
        mListener = listener;
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        mRecyclerView = recyclerView;
    }

    void setBlePeripherals(final List<BlePeripheral> blePeripherals) {
        if (mBlePeripherals == null) {
            mBlePeripherals = blePeripherals;
            notifyItemRangeInserted(0, mBlePeripherals != null ? mBlePeripherals.size() : 0);
        } else {
            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return mBlePeripherals.size();
                }

                @Override
                public int getNewListSize() {
                    return blePeripherals.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    final String oldItemAddress = mBlePeripherals.get(oldItemPosition).getDevice().getAddress();
                    final String newItemAddress = blePeripherals.get(newItemPosition).getDevice().getAddress();

                    return oldItemAddress.equals(newItemAddress);
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    return mBlePeripherals.get(oldItemPosition).equals(blePeripherals.get(newItemPosition));

                }
            });
            mBlePeripherals = blePeripherals;

            // Save and restore state to preserve user scroll
            Parcelable recyclerViewState = mRecyclerView.getLayoutManager().onSaveInstanceState();
            result.dispatchUpdatesTo(this);
            mRecyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState);

        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_scan_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final BlePeripheral blePeripheral = mBlePeripherals.get(position);

        final String identifier = blePeripheral.getIdentifier();

        // Main view
        String name = blePeripheral.getName();
        if (name == null) {
            name = identifier;
        }

        holder.deviceAddress = identifier;
        holder.nameTextView.setText(name);

        final int rssiDrawableResource = RssiUI.getDrawableIdForRssi(blePeripheral.getRssi());
        holder.rssiImageView.setImageResource(rssiDrawableResource);

        holder.connectButton.setTag(position);
        final int connectionState = blePeripheral.getConnectionState();
        final String connectionAction = connectionState == BlePeripheral.STATE_DISCONNECTED ? "scanner_connect" : "scanner_disconnect";
        holder.connectButton.setText(LocalizationManager.getInstance().getString(mContext, connectionAction));

        final WeakReference<BlePeripheral> weakBlePeripheral = new WeakReference<>(blePeripheral);
        holder.connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                KeyboardUtils.dismissKeyboard(view);

                BlePeripheral selectedBlePeripheral = weakBlePeripheral.get();
                if (selectedBlePeripheral != null) {
                    if (connectionState == BlePeripheral.STATE_DISCONNECTED) {
                        selectedBlePeripheral.connect(mContext);
                    } else {
                        selectedBlePeripheral.disconnect();
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mBlePeripherals == null ? 0 : mBlePeripherals.size();
    }

    // region ViewHolder
    static class ViewHolder extends RecyclerView.ViewHolder {
        View view;
        TextView nameTextView;
        ImageView rssiImageView;
        Button connectButton;
        String deviceAddress;

        ViewHolder(View view) {
            super(view);
            this.view = view;
            nameTextView = view.findViewById(R.id.nameTextView);
            rssiImageView = view.findViewById(R.id.rssiImageView);
            connectButton = view.findViewById(R.id.connectButton);
            deviceAddress = null;
        }
    }
    // endregion

}
