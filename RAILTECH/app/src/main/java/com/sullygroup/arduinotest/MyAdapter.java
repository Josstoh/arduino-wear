package com.sullygroup.arduinotest;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.List;

/**
 * Created by jocelyn.caraman on 21/03/2017.
 */

public class MyAdapter extends RecyclerView.Adapter<ViewHolder> {
    private List<Object> mDataset;
    protected MainActivity activity;

    public static class TitleViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView mTextView;
        public TitleViewHolder(View v) {
            super(v);
            mTextView = (TextView) v.findViewById(R.id.title);
        }
        public void bind(Object o) {
            String s = (String) o;
            mTextView.setText(s);
        }
    }

    class StatsViewHolder extends RecyclerView.ViewHolder {
        public TextView mTextView;
        public ImageView mImageView;
        public TextView name;
        public StatsViewHolder(View v){
            super(v);
            mTextView = (TextView) v.findViewById(R.id.value);
            mImageView = (ImageView) v.findViewById(R.id.icon);
            name = (TextView) v.findViewById(R.id.name);
        }
        public void bind(Object o) {
            Stats s = (Stats) o;
            mTextView.setText(s.getValue()+"");
            name.setText(s.getName());
            mImageView.setImageResource(s.getIcon());
        }
    }

    class SeekBarViewHolder extends RecyclerView.ViewHolder {
        public SeekBar mSeekBar;
        public TextView mTextView;
        public SeekBarViewHolder(View v){
            super(v);
            mSeekBar = (SeekBar) v.findViewById(R.id.seekBar);
            mTextView = (TextView) v.findViewById(R.id.textView);
            mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                private int progress;
                @Override
                public void onProgressChanged(SeekBar seekBar, int mProgress, boolean b) {
                    progress = mProgress;
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    mTextView.setText(activity.getString(R.string.rotate,progress));
                    if(activity != null){
                        String command = "c";
                        command += Integer.toString(progress);
                        Log.d("Adapter",command);
                        activity.requestConnectToArduino(command.getBytes());
                    }

                }
            });
        }
        public void bind(Object o) {
            int v = (int) o;
            mSeekBar.setProgress(v);
        }
    }

    public MyAdapter(List<Object> myDataset, Context mContext) {
        mDataset = myDataset;
        if(mContext instanceof MainActivity)
            activity = (MainActivity) mContext;
        else
            activity = null;
    }

    @Override
    public int getItemViewType(int position) {
        Object o = mDataset.get(position);
        if(o instanceof String) {
            return 0;
        }
        else if (o instanceof Stats){
            return 1;
        }
        return 2;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case 0:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_title_item,parent,false);
                return new TitleViewHolder(view);
            case 1:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_stats_item,parent,false);
                return new StatsViewHolder(view);
            case 2:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_seekbar_item,parent,false);
                return new SeekBarViewHolder(view);
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        switch (holder.getItemViewType()) {
            case 0:
                TitleViewHolder viewHolder0 = (TitleViewHolder) holder;
                viewHolder0.bind(mDataset.get(position));
                StaggeredGridLayoutManager.LayoutParams layoutParams = (StaggeredGridLayoutManager.LayoutParams) viewHolder0.itemView.getLayoutParams();
                layoutParams.setFullSpan(true);
                break;

            case 1:
                StatsViewHolder viewHolder1 = (StatsViewHolder) holder;
                viewHolder1.bind(mDataset.get(position));
                break;
            case 2:
                SeekBarViewHolder viewHolder2 = (SeekBarViewHolder) holder;
                viewHolder2.bind(mDataset.get(position));
                StaggeredGridLayoutManager.LayoutParams layoutParams2 = (StaggeredGridLayoutManager.LayoutParams) viewHolder2.itemView.getLayoutParams();
                layoutParams2.setFullSpan(true);
                break;
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}
