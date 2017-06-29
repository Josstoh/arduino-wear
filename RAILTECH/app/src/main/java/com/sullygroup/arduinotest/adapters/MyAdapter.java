package com.sullygroup.arduinotest.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.sullygroup.arduinotest.activities.MainActivity;
import com.sullygroup.arduinotest.R;
import com.sullygroup.arduinotest.models.Stats;
import com.sullygroup.arduinotest.services.BluetoothService;
import com.sullygroup.arduinotest.services.TempAndHumService;

import java.util.List;

import static com.sullygroup.arduinotest.services.TempAndHumService.*;

/**
 * Adapter pour le RecyclerView du MainActivity.
 * Created by jocelyn.caraman on 21/03/2017.
 */

public class MyAdapter extends RecyclerView.Adapter<ViewHolder> {
    public final static String TAG = "MAAdapter";
    private List<Object[]> mDataset;
    private MainActivity activity;
    private TempAndHumService tahService;
    public static final int MAIN_TITLE_VIEW_HOLDER = 0;
    public static final int TITLE_VIEW_HOLDER = 1;
    public static final int STATS_VIEW_HOLDER = 2;
    public static final int ROTATE_VIEW_HOLDER = 3;
    public static final int COLOR_SELECTOR_VIEW_HOLDER = 4;
    public static final int CONTROLS_VIEW_HOLDER = 5;

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
        // Appellé pour mettre à jour la vue avec les nouvelles données
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
            mTextView.post(new Runnable() {
                @Override
                public void run() {
                    mTextView.setHeight(mImageView.getWidth());
                }
            });
        }
    }

    /**
     * ViewHolder qui contient les vues pour la rotation (l'angle et un sélecteur).
     */
    private class RotateViewHolder extends RecyclerView.ViewHolder {
        SeekBar mSeekBar;
        TextView mTextView;

        RotateViewHolder(View v){
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
                        Bundle bundle = new Bundle(1);
                        bundle.putInt("angle",progress);
                        tahService.requestCommand(ROTATE_CMD,bundle);
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

    /**
     * ViewHolder qui contient les vues pour la couleur de la LED(3 composantes de couleurs RGB et le bouton).
     */
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
                            Bundle bundle = new Bundle(3);
                            bundle.putInt("red",Integer.valueOf(red));
                            bundle.putInt("green",Integer.valueOf(green));
                            bundle.putInt("blue",Integer.valueOf(blue));
                            tahService.requestCommand(COLOR_CMD,bundle);
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

    /**
     * ViewHolder qui contient les vues pour contrôler la connexion avec l'Arduino en BT.
     */
    private class ControlsViewHolder extends RecyclerView.ViewHolder {
        FloatingActionButton refresh;
        FloatingActionButton co_deco;
        Switch mode;
        ControlsViewHolder(View v){
            super(v);
            refresh = (FloatingActionButton) v.findViewById(R.id.fab_refresh);
            co_deco = (FloatingActionButton) v.findViewById(R.id.fab_connect_disconnect);
            mode = (Switch) v.findViewById(R.id.switch_mode) ;
            final LinearLayout row1 = (LinearLayout) v.findViewById(R.id.linear_layout_row1);

            refresh.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(tahService != null) {
                        tahService.requestCommand(TEMP_AND_HUM_CMD,null);
                        activity.onRefreshPending();
                    }

                    else
                        Log.e(TAG,"Can't request update : service is null");
                }
            });

            co_deco.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(activity != null) {
                        if(mode.isChecked()){
                            if(!BluetoothService.isRunning()) {
                                tahService.connectToDevice();
                                co_deco.setImageResource(R.drawable.ic_disconnect);
                            }
                            else {
                                tahService.disconnectToDevice();
                                co_deco.setImageResource(R.drawable.ic_connect);
                            }

                        }
                    }
                    else
                        Log.e(TAG,"Activity is null : can't create service");
                }
            });

            mode.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(activity != null) {
                        SharedPreferences sp = activity.getSharedPreferences(activity.getString(R.string.preference_connexion),MODE_PRIVATE);
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putBoolean(activity.getString(R.string.preference_key_bt_mode),isChecked);
                        editor.apply();
                        tahService.setConnexionMode(isChecked? ConnectionMode.BT: ConnectionMode.PHONE);
                    }
                    refresh.setVisibility(isChecked? View.VISIBLE:View.GONE);
                    co_deco.setVisibility(isChecked? View.VISIBLE:View.GONE);
                    int padding_bot;
                    if(isChecked)
                        padding_bot = 0;
                    else
                        padding_bot = 20;
                    row1.setPadding(row1.getPaddingLeft(),row1.getPaddingTop(),
                            row1.getPaddingRight(),padding_bot);

                }
            });

            SharedPreferences sp = activity.getSharedPreferences(activity.getString(R.string.preference_connexion),MODE_PRIVATE);
            boolean bt_mode = sp.getBoolean(activity.getString(R.string.preference_key_bt_mode),false);
            mode.setChecked(bt_mode);
            refresh.setVisibility(bt_mode? View.VISIBLE:View.GONE);
            co_deco.setVisibility(bt_mode? View.VISIBLE:View.GONE);
        }
        void bind(Object o) {
            //nada
        }
    }

    public MyAdapter(List<Object[]> myDataset, Context mContext, TempAndHumService service) {
        mDataset = myDataset;
        if(mContext instanceof MainActivity)
            activity = (MainActivity) mContext;
        else
            activity = null;
        tahService = service;
    }

    /**
     * Retourne l'ID du type de vue
     * @param position position dans la liste
     * @return un int qui représente le type de vue
     */
    @Override
    public int getItemViewType(int position) {
        Integer type = (Integer) mDataset.get(position)[0];
        if(type != null)
            return type;
        else return -1;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        switch (viewType) {
            case MAIN_TITLE_VIEW_HOLDER:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_main_title_item,parent,false);
                return new TitleViewHolder(view);
            case TITLE_VIEW_HOLDER:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_title_item,parent,false);
                return new TitleViewHolder(view);
            case STATS_VIEW_HOLDER:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_stats_item,parent,false);
                return new StatsViewHolder(view);
            case ROTATE_VIEW_HOLDER:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_seekbar_item,parent,false);
                return new RotateViewHolder(view);
            case COLOR_SELECTOR_VIEW_HOLDER:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_color_selector_item,parent,false);
                return new ColorSelectorViewHolder(view);
            case CONTROLS_VIEW_HOLDER:
                view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_controls_item,parent,false);
                return  new ControlsViewHolder(view);
            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        switch (holder.getItemViewType()) {
            case MAIN_TITLE_VIEW_HOLDER:
            case TITLE_VIEW_HOLDER:
                TitleViewHolder viewHolder0 = (TitleViewHolder) holder;
                viewHolder0.bind(mDataset.get(position)[1]);
                StaggeredGridLayoutManager.LayoutParams layoutParams = (StaggeredGridLayoutManager.LayoutParams) viewHolder0.itemView.getLayoutParams();
                layoutParams.setFullSpan(true);
                break;

            case STATS_VIEW_HOLDER:
                StatsViewHolder viewHolder1 = (StatsViewHolder) holder;
                viewHolder1.bind(mDataset.get(position)[1]);
                break;
            case ROTATE_VIEW_HOLDER:
                RotateViewHolder viewHolder2 = (RotateViewHolder) holder;
                viewHolder2.bind(mDataset.get(position)[1]);
                StaggeredGridLayoutManager.LayoutParams layoutParams2 = (StaggeredGridLayoutManager.LayoutParams) viewHolder2.itemView.getLayoutParams();
                layoutParams2.setFullSpan(true);
                break;
            case COLOR_SELECTOR_VIEW_HOLDER:
                ColorSelectorViewHolder viewHolder3 = (ColorSelectorViewHolder) holder;
                viewHolder3.bind(mDataset.get(position)[1]);
                StaggeredGridLayoutManager.LayoutParams layoutParams3 = (StaggeredGridLayoutManager.LayoutParams) viewHolder3.itemView.getLayoutParams();
                layoutParams3.setFullSpan(true);
                break;
            case CONTROLS_VIEW_HOLDER:
                ControlsViewHolder viewHolder4 = (ControlsViewHolder) holder;
                StaggeredGridLayoutManager.LayoutParams layoutParams4 = (StaggeredGridLayoutManager.LayoutParams) viewHolder4.itemView.getLayoutParams();
                layoutParams4.setFullSpan(true);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    public void setTahService(TempAndHumService service) {
        tahService = service;
    }
}
