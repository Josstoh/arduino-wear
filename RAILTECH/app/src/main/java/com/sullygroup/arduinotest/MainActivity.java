package com.sullygroup.arduinotest;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.Set;

/**
 * Ecran principal de l'application, il affiche les infos de la carte et des contrôles.
 */
public class MainActivity extends WearableActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, DataApi.DataListener {

    private static final String TAG = "MainActivity";

    public static final String CONNECT_TO_ARDUINO_CAPABILITY = "connect_to_arduino";
    public static final String CONNECT_TO_ARDUINO_MESSAGE_PATH = "/connect_to_arduino";

    GoogleApiClient mGoogleApiClient;
    CapabilityApi.CapabilityListener capabilityListener;
    RecyclerView mRecyclerView;
    MyAdapter mAdapter;
    ArrayList<Object> list;
    // Node qui est connecté à la carte Arduino (le téléphone)
    private String ConnectedToArduinoNodeId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_main);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        // liste des éléments qui composent la RecyclerView
        list = new ArrayList<>();
        list.add("Temperature\nand humidity");
        list.add(new Stats("Temp.",-1,R.drawable.ic_thermometer));
        list.add(new Stats("Humi.",-1,R.drawable.ic_humidity));
        list.add("Rotate");
        list.add(90);
        list.add("LED Color");
        list.add(new int[] {0,255,100});

        mAdapter = new MyAdapter(list,this);
        mRecyclerView.setAdapter(mAdapter);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
    }

    /**
     * Met à jour le noeud connecté à la carte.
     * @param capabilityInfo
     */
    private void updateConnectToArduinoCapability(CapabilityInfo capabilityInfo) {
        Set<Node> connectedNodes = capabilityInfo.getNodes();
        ConnectedToArduinoNodeId = pickBestNodeId(connectedNodes);
    }

    /**
     * Choisit le meilleur noeud à utiliser (le téléphone).
     * @param nodes le set de noeud parmi lequelle choisir
     * @return le meilleur noeud ou null s'il n'y en a pas.
     */
    private String pickBestNodeId(Set<Node> nodes) {
        String bestNodeId = null;
        for (Node node : nodes) {
            if (node.isNearby()) {
                return node.getId();
            }
            bestNodeId = node.getId();
        }
        return bestNodeId;
    }

    /**
     * Pour envoyer un message à l'Arduino. Passe par le téléphone.
     * @param string le message en Bytes à envoyé.
     */
    public void requestConnectToArduino(byte[] string) {
        Log.d(TAG,"request");
        if (ConnectedToArduinoNodeId != null) {
            Wearable.MessageApi.sendMessage(mGoogleApiClient, ConnectedToArduinoNodeId,
                CONNECT_TO_ARDUINO_MESSAGE_PATH, string).setResultCallback(
                new ResultCallback<MessageApi.SendMessageResult>(){
                    @Override
                    public void onResult(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                        if (!sendMessageResult.getStatus().isSuccess()) {
                            Log.e(TAG,"Sending message failed");
                        }
                    }
                }
            );
        } else {
            Log.e(TAG,"Unable to retrieve node which is connected to arduino");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (null != mGoogleApiClient && mGoogleApiClient.isConnected()) {
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            Wearable.CapabilityApi.removeCapabilityListener(mGoogleApiClient,capabilityListener,CONNECT_TO_ARDUINO_CAPABILITY);
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_DELETED) {
                Log.d(TAG, "DataItem deleted: " + event.getDataItem().getUri());
            } else if (event.getType() == DataEvent.TYPE_CHANGED) {
                Log.d(TAG, "DataItem changed: " + event.getDataItem().getUri());
                DataItem item = event.getDataItem();
                if (item.getUri().getPath().compareTo("/stats/temp") == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    updateTemp(dataMap.getInt("value"));
                }
                else if(item.getUri().getPath().compareTo("/stats/hum") == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    updateHum(dataMap.getInt("value"));
                }
                else if(item.getUri().getPath().compareTo("/rotate") == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    updateRotate(dataMap.getInt("value"));
                }
                else if(item.getUri().getPath().compareTo("/color") == 0) {
                    DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                    //updateColor(dataMap.getInt("red"),dataMap.getInt("red"),dataMap.getInt("blue"));
                }
            }
        }
        dataEventBuffer.release();
    }
    /**
     * Met à jour l'affichage des champs de la couleur de la LED
     * @param red nouvelle valeur pour le rouge
     * @param green nouvelle valeur pour le vert
     * @param blue nouvelle valeur pour le bleu
     */
    private void updateColor(int red, int green, int blue) {
        list.set(6,new int[] {red,green,blue});
        mAdapter.notifyDataSetChanged();
    }

    /**
     * Met à jour l'affichage du champ Rotate
     * @param value nouvelle valeur
     */
    private void updateRotate(int value) {
        list.set(4,value);
        mAdapter.notifyDataSetChanged();
    }
    /**
     * Met à jour l'affichage du champ température
     * @param temp nouvelle valeur
     */
    private void updateTemp(int temp) {
        ((Stats)list.get(1)).setValue(temp);
        mAdapter.notifyDataSetChanged();
    }
    /**
     * Met à jour l'affichage du champ humidité
     * @param hum nouvelle valeur
     */
    private void updateHum(int hum) {
        ((Stats)list.get(2)).setValue(hum);
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "onConnected: " + connectionHint);
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Connected to Google Api Service");
        }
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        capabilityListener =
                new CapabilityApi.CapabilityListener() {
                    @Override
                    public void onCapabilityChanged(CapabilityInfo capabilityInfo) {
                        Log.d(TAG,"Capability changed");
                        updateConnectToArduinoCapability(capabilityInfo);
                    }
                };
        // Pour savoir quand on est connecté au téléphone
        Wearable.CapabilityApi.addCapabilityListener(mGoogleApiClient,
                capabilityListener,CONNECT_TO_ARDUINO_CAPABILITY).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                Log.d(TAG,"capability listener :" + status.isSuccess());
            }
        });

        Wearable.CapabilityApi.getCapability(mGoogleApiClient, CONNECT_TO_ARDUINO_CAPABILITY,CapabilityApi.FILTER_REACHABLE).setResultCallback(new ResultCallback<CapabilityApi.GetCapabilityResult>() {
            @Override
            public void onResult(@NonNull CapabilityApi.GetCapabilityResult getCapabilityResult) {
                updateConnectToArduinoCapability(getCapabilityResult.getCapability());
            }
        });
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.d(TAG, "onConnectionSuspended: " + cause);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed: " + connectionResult);
    }
}
