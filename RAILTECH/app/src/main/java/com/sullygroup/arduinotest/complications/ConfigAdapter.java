package com.sullygroup.arduinotest.complications;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.sullygroup.arduinotest.R;

import java.util.List;

/**
 * Adapter pour le RecyclerView de la ComplicationConfigActivity.
 * Created by jocelyn.caraman on 07/04/2017.
 */
class ConfigAdapter extends RecyclerView.Adapter {
    private static final String TAG = "ConfigAdapter";
    private List<String[]> mDataset;
    private ComplicationConfigActivity activity;

    private class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        TextView mTextView;
        ViewHolder(View v) {
            super(v);
            itemView.setOnClickListener(this);
            mTextView = (TextView) v.findViewById(R.id.title);
        }
        void bind(Object o) {
            String[] s = (String[]) o;
            mTextView.setText(s[1]);
        }
        @Override
        public void onClick(View view) {
            activity.handleItemClick(getAdapterPosition());
        }
    }

    ConfigAdapter(List<String[]> myDataset,ComplicationConfigActivity mActivity) {
        mDataset = myDataset;
        activity = mActivity;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_complication_config,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ViewHolder viewHolder0 = (ViewHolder) holder;
        viewHolder0.bind(mDataset.get(position));
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}
