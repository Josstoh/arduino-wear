package com.sullygroup.arduinotest.activities;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.sullygroup.arduinotest.R;
import com.sullygroup.arduinotest.adapters.MyAdapter;
import com.sullygroup.arduinotest.models.Stats;
import com.sullygroup.arduinotest.services.TempAndHumService;

import java.util.ArrayList;

/**
 * Ecran principal de l'application, il affiche les infos de la carte et des contrôles.
 */
public class MainActivity extends WearableActivity implements TempAndHumService.TempAndHumServiceListener{

    private static final String TAG = "MainActivity";

    private RecyclerView mRecyclerView;
    private MyAdapter mAdapter;
    private ArrayList<Object[]> list;
    private TextView connectedTextView;
    private boolean refreshPending = false;
    private TempAndHumService tahService;
    private Intent tahServiceIntent;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.d("bind","onServiceConnected called "+ className.getShortClassName());
            switch (className.getShortClassName())
            {
                case ".services."+TempAndHumService.TAG:
                    TempAndHumService.LocalBinder binder2 = (TempAndHumService.LocalBinder) service;
                    tahService = binder2.getServiceInstance();
                    tahService.registerClient(MainActivity.this);
                    mAdapter.setTahService(tahService);
                    if(tahService.isConnectedToDevice()) {
                        onConnected();
                    }
                    break;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "onServiceDisconnected called");

            switch (className.getShortClassName())
            {
                case "."+TempAndHumService.TAG:
                    tahService.unRegisterClient();
                    tahService = null;
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_main);

        //mResolvingError = savedInstanceState != null && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false);

        connectedTextView = (TextView) findViewById(R.id.connected_text_view);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        // liste des éléments qui composent la RecyclerView
        list = new ArrayList<>();
        list.add(new Object[] {MyAdapter.MAIN_TITLE_VIEW_HOLDER, "Temperature\nand humidity"});
        list.add(new Object[] {MyAdapter.STATS_VIEW_HOLDER, new Stats("Temp.",-1,R.drawable.ic_thermometer)});
        list.add(new Object[] {MyAdapter.STATS_VIEW_HOLDER,new Stats("Humi.",-1,R.drawable.ic_humidity)});
        list.add(new Object[] {MyAdapter.TITLE_VIEW_HOLDER, "Rotate"});
        list.add(new Object[] {MyAdapter.ROTATE_VIEW_HOLDER ,90});
        list.add(new Object[] {MyAdapter.TITLE_VIEW_HOLDER, "LED Color"});
        list.add(new Object[] {MyAdapter.COLOR_SELECTOR_VIEW_HOLDER, new int[] {0,255,100}});
        list.add(new Object[] {MyAdapter.TITLE_VIEW_HOLDER, "Controls"});
        list.add(new Object[] {MyAdapter.CONTROLS_VIEW_HOLDER});

        mAdapter = new MyAdapter(list,this,tahService);
        mRecyclerView.setAdapter(mAdapter);

        tahServiceIntent = new Intent(MainActivity.this, TempAndHumService.class);
        connectToTahService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Switch sw_mode = (Switch) findViewById(R.id.switch_mode);
        //if(sw_mode != null)
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(tahService != null) {
            tahService.unRegisterClient();
            stopService(tahServiceIntent);
        }

        unbindService(mConnection);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        /*if (requestCode == REQUEST_RESOLVE_ERROR) {
            mResolvingError = false;
            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!mGoogleApiClient.isConnecting() && !mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.connect();
                }
            }
        }*/
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //outState.putBoolean(STATE_RESOLVING_ERROR, mResolvingError);
    }

    /**
     * Met à jour l'affichage des champs de la couleur de la LED
     * @param red nouvelle valeur pour le rouge
     * @param green nouvelle valeur pour le vert
     * @param blue nouvelle valeur pour le bleu
     */
    public void updateColor(int red, int green, int blue) {
        Object[] obj = list.get(6);
        obj[1] = new int[] {red,green,blue};
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Met à jour l'affichage du champ Rotate
     * @param value nouvelle valeur
     */
    public void updateRotate(int value) {
        Object[] obj = list.get(4);
        obj[1] = value;
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Met à jour l'affichage du champ température
     * @param temp nouvelle valeur
     */
    public void updateTemp(int temp) {
        ((Stats)list.get(1)[1]).setValue(temp);
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Met à jour l'affichage du champ humidité
     * @param hum nouvelle valeur
     */
    public void updateHum(int hum) {
        ((Stats)list.get(2)[1]).setValue(hum);
        mAdapter.notifyDataSetChanged();
    }


    @Override
    public void onConnected() {
        Log.d(TAG,"connected to TempAndHumService");
        showConfirmationActivity("Connected to Arduino");
        connectedTextView.setVisibility(View.GONE);
    }

    @Override
    public void onTempResponse(float msg) {
        Log.d(TAG,"onTempResponse");
        updateTemp((int)msg);
        if(refreshPending) {
            showConfirmationActivity("Info refreshed");
            refreshPending = false;
        }
    }

    @Override
    public void onHumResponse(float msg) {
        Log.d(TAG,"onHUmResponse");
        updateHum((int)msg);
        if(refreshPending) {
            showConfirmationActivity("Info refreshed");
            refreshPending = false;
        }
    }

    @Override
    public void onRotateModified(int angle) {
        Log.d(TAG,"rotatemodified");
        updateRotate(angle);
    }

    @Override
    public void onServiceClosing() {
        Log.d(TAG,"TempAndHumService closing");
        connectedTextView.setVisibility(View.VISIBLE);
    }

    @Override
    public void updateTime() {
        Log.d(TAG,"updatetime");
    }

    /**
     * Crée le service @link TempAndHumService et le bind à cette activité
     */
    public void connectToTahService() {
        SharedPreferences sp = getSharedPreferences("connexion_preferences",MODE_PRIVATE);
        boolean bt_mode = sp.getBoolean(getString(R.string.preference_key_bt_mode),false);
        tahServiceIntent.putExtra(getString(R.string.preference_key_bt_mode),bt_mode);
        startService(tahServiceIntent);
        bindService(tahServiceIntent,mConnection,BIND_AUTO_CREATE);
    }

    /**
     * Quand une reqûete de rafraichissement des données est en attente. Permet plus tard d'afficher une ConfirmationActivity.
     */
    public void onRefreshPending() {
        refreshPending = true;
    }

    /**
     * Affiche une succes ConfirmationActivity avec le message passé en paramètre.
     * @param message message à affiché dans la ConfirmationActivity
     */
    private void showConfirmationActivity(String message) {
        Intent intent = new Intent(this, ConfirmationActivity.class);
        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE,
                ConfirmationActivity.SUCCESS_ANIMATION);
        intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, message);
        startActivity(intent);
    }
}
