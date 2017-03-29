package com.sullygroup.arduinotest;

import android.content.Context;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.List;

/**
 * Adapter pour le RecyclerView du MainActivity.
 * Created by jocelyn.caraman on 21/03/2017.
 */

public class MyAdapter extends RecyclerView.Adapter<ViewHolder> {
    private List<Object> mDataset;
    private MainActivity activity;

    /**
     * ViewHolder qui contient la vue pour le titre d'une section.
     */
    private class TitleViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        TextView mTextView;
        TitleViewHolder(View v) {
            super(v);
            mTextView = (TextView) v.findViewById(R.id.title);
        }
        void bind(Object o) {
            String s = (String) o;
            mTextView.setText(s);
        }
    }

    /**
     * ViewHolder qui contient les vues pour les stats(température et l'humidité).
     */
    private class StatsViewHolder extends RecyclerView.ViewHolder {
        TextView mTextView;
        ImageView mImageView;
        TextView name;
        StatsViewHolder(View v){
            super(v);
            mTextView = (TextView) v.findViewById(R.id.value);
            mImageView = (ImageView) v.findViewById(R.id.icon);
            name = (TextView) v.findViewById(R.id.name);
        }
        void bind(Object o) {
            Stats s = (Stats) o;
            mTextView.setText(s.getValue()+"");
            name.setText(s.getName());
            mImageView.setImageResource(s.getIcon());
        }
    }
    /**
     * ViewHolder qui contient les vues pour les stats(température et l'humidité).
     */
    private class SeekBarViewHolder extends RecyclerView.ViewHolder {
        SeekBar mSeekBar;
        TextView mTextView;

        SeekBarViewHolder(View v){
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
        void bind(Object o) {
            int v = (int) o;
            mSeekBar.setProgress(v);
            mTextView.setText(activity.getString(R.string.rotate,v));
        }
    }

    private class ColorSelectorViewHolder extends RecyclerView.ViewHolder {
        EditText redEditText;
        EditText greenEditText;
        EditText blueEditText;
        FloatingActionButton fab;
        int[] colors;
        ColorSelectorViewHolder(View v){
            super(v);
            redEditText = (EditText) v.findViewById(R.id.red_edit_text);
            redEditText.post(new Runnable() {
                @Override
                public void run() {
                    redEditText.setHeight(redEditText.getWidth());
                }
            });
            redEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        colors[0] = Integer.valueOf(redEditText.getText().toString());
                        InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                        return true;
                    }
                    return false;
                }
            });
            greenEditText = (EditText) v.findViewById(R.id.green_edit_text);
            greenEditText.post(new Runnable() {
                @Override
                public void run() {
                    greenEditText.setHeight(greenEditText.getWidth());
                }
            });
            greenEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        colors[1] = Integer.valueOf(greenEditText.getText().toString());
                        InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                        return true;
                    }
                    return false;
                }
            });
            blueEditText = (EditText) v.findViewById(R.id.blue_edit_text);
            blueEditText.post(new Runnable() {
                @Override
                public void run() {
                    blueEditText.setHeight(blueEditText.getWidth());
                }
            });
            blueEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        colors[2] = Integer.valueOf(blueEditText.getText().toString());
                        InputMethodManager imm = (InputMethodManager)v.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                        return true;
                    }
                    return false;
                }
            });
            fab = (FloatingActionButton) v.findViewById(R.id.fab) ;
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String red = redEditText.getText().toString();
                    String green = greenEditText.getText().toString();
                    String blue = blueEditText.getText().toString();
                    if(!red.isEmpty() && !green.isEmpty() && !blue.isEmpty()) {
                        if(activity != null){
                            String command = "d" + red + ";" + green + ";" + blue;
                            Log.d("Adapter",command);
                            activity.requestConnectToArduino(command.getBytes());
                        }
                    }

                }
            });
        }
        void bind(Object o) {
            colors = (int[]) o;
            redEditText.setText(colors[0]+"");
            greenEditText.setText(colors[1]+"");
            blueEditText.setText(colors[2]+"");
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
        if(position == 0)
            return 0;
        Object o = mDataset.get(position);
        if(o instanceof String) {
            return 1;
        }
        else if (o instanceof Stats){
            return 2;
        }
        else if(o instanceof int[]){
            return 4;
        }
        return 3;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case 0:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_main_title_item,parent,false);
                return new TitleViewHolder(view);
            case 1:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_title_item,parent,false);
                return new TitleViewHolder(view);
            case 2:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_stats_item,parent,false);
                return new StatsViewHolder(view);
            case 3:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_seekbar_item,parent,false);
                return new SeekBarViewHolder(view);
            case 4:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_color_selector_item,parent,false);
                return new ColorSelectorViewHolder(view);
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        switch (holder.getItemViewType()) {
            case 0:
            case 1:
                TitleViewHolder viewHolder0 = (TitleViewHolder) holder;
                viewHolder0.bind(mDataset.get(position));
                StaggeredGridLayoutManager.LayoutParams layoutParams = (StaggeredGridLayoutManager.LayoutParams) viewHolder0.itemView.getLayoutParams();
                layoutParams.setFullSpan(true);
                break;

            case 2:
                StatsViewHolder viewHolder1 = (StatsViewHolder) holder;
                viewHolder1.bind(mDataset.get(position));
                break;
            case 3:
                SeekBarViewHolder viewHolder2 = (SeekBarViewHolder) holder;
                viewHolder2.bind(mDataset.get(position));
                StaggeredGridLayoutManager.LayoutParams layoutParams2 = (StaggeredGridLayoutManager.LayoutParams) viewHolder2.itemView.getLayoutParams();
                layoutParams2.setFullSpan(true);
                break;
            case 4:
                ColorSelectorViewHolder viewHolder3 = (ColorSelectorViewHolder) holder;
                viewHolder3.bind(mDataset.get(position));
                StaggeredGridLayoutManager.LayoutParams layoutParams3 = (StaggeredGridLayoutManager.LayoutParams) viewHolder3.itemView.getLayoutParams();
                layoutParams3.setFullSpan(true);
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }
}
