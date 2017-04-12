package com.sullygroup.arduinotest;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * Created by jocelyn.caraman on 07/04/2017.
 */

public class ConfigAdapter extends RecyclerView.Adapter {
    public static final String TAG = "ConfigAdapter";
    private List<String> mDataset;
    private ComplicationConfigActivity activity;
    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        TextView mTextView;
        ViewHolder(View v) {
            super(v);
            itemView.setOnClickListener(this);
            mTextView = (TextView) v.findViewById(R.id.title);
        }
        void bind(Object o) {
            String s = (String) o;
            mTextView.setText(s);
        }
        @Override
        public void onClick(View view) {
            Log.d(TAG, "onClick " + getAdapterPosition() + " " + mDataset.get(getAdapterPosition()));
            activity.handleItemClick(getAdapterPosition());
        }
    }

    ConfigAdapter(List<String> myDataset,ComplicationConfigActivity mActivity) {
        mDataset = myDataset;
        activity = mActivity;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_main_title_item,parent,false);
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
