package com.sullygroup.arduinotest.complications;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.view.CurvedChildLayoutManager;
import android.support.wearable.view.WearableRecyclerView;
import android.view.View;

import com.sullygroup.arduinotest.R;

import java.util.ArrayList;

import static android.support.wearable.complications.ComplicationProviderService.EXTRA_CONFIG_COMPLICATION_ID;
import static android.support.wearable.complications.ComplicationProviderService.EXTRA_CONFIG_COMPLICATION_TYPE;

/**
 * Activité qui permet de configurer les complications de l'applications.
 * Created by jocelyn.caraman on 07/04/2017.
 */

public class ComplicationConfigActivity extends WearableActivity {
    public static final String TAG = "ComplicationConfigAct";
    private WearableRecyclerView mRecyclerView;
    private ArrayList<String[]> list;
    private String complicationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_complication_config);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if(bundle != null){
            String id = bundle.get(EXTRA_CONFIG_COMPLICATION_ID).toString();
            if(id != null && !id.isEmpty()) {
                complicationId = String.valueOf(id);
                /*SharedPreferences sharedPref = getSharedPreferences(
                        getString(R.string.preference_complications), Context.MODE_PRIVATE);
                String sharedPref.getString(id, "none");*/

            }
            int type = bundle.getInt(EXTRA_CONFIG_COMPLICATION_TYPE);
            switch (type){
                case ComplicationData.TYPE_LONG_TEXT:
                    setResult(RESULT_OK);
                    finish();
                    break;
                case ComplicationData.TYPE_SHORT_TEXT:
                    break;
                default:
                    setResult(RESULT_CANCELED);
                    finish();
                    break;
            }

        }

        mRecyclerView = (WearableRecyclerView) findViewById(R.id.recycler);
        mRecyclerView.setCenterEdgeItems(true);
        CurvedChildLayoutManager mChildLayoutManager = new MyLauncherChildLayoutManager(this);
        mRecyclerView.setLayoutManager(mChildLayoutManager);
        list = new ArrayList<>();
        list.add(new String[] {"temp","Temperature"});
        list.add(new String[] {"hum","Humidity"});
        mRecyclerView.setAdapter(new ConfigAdapter(list,this));
    }

    /**
     * Méthode pour gérer les clics sur les items du RecyclerView.
     * @param adapterPosition position de l'item dans le RecyclerView
     */
    public void handleItemClick(int adapterPosition) {
        SharedPreferences sharedPref = getSharedPreferences(
                getString(R.string.preference_complications), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(complicationId,list.get(adapterPosition)[0]);
        editor.apply();
        setResult(RESULT_OK);
        finish();
    }

    /**
     * Classe permettant l'affichage du RecyclerView en mode "curvé".
     */
    private class MyLauncherChildLayoutManager extends CurvedChildLayoutManager {
        /** How much should we scale the icon at most. */
        private static final float MAX_ICON_PROGRESS = 0.65f;

        private float mProgressToCenter;

        MyLauncherChildLayoutManager(Context context) {
            super(context);
        }

        @Override
        public void updateChild(View child, WearableRecyclerView parent) {
            super.updateChild(child, parent);

            // Figure out % progress from top to bottom
            float centerOffset = ((float) child.getHeight() / 2.0f) / (float) parent.getHeight();
            float yRelativeToCenterOffset = (child.getY() / parent.getHeight()) + centerOffset;

            // Normalize for center
            mProgressToCenter = Math.abs(0.5f - yRelativeToCenterOffset);
            // Adjust to the maximum scale
            mProgressToCenter = Math.min(mProgressToCenter, MAX_ICON_PROGRESS);

            child.setScaleX(1 - mProgressToCenter);
            child.setScaleY(1 - mProgressToCenter);
        }
    }
}
