package com.sullygroup.arduinotest;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationManager;
import android.support.wearable.complications.ComplicationProviderService;
import android.support.wearable.complications.ComplicationText;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.TimeUnit;

/**
 * Provider service qui permet de mettre à jour la "Complication"(widget) sur la watchface.
 * Created by jocelyn.caraman on 20/03/2017.
 */

public class TempAndHumProviderService extends ComplicationProviderService {
    private String TAG = "TAHProviderService";
    private GoogleApiClient googleApiClient;

    @Override
    public void onComplicationUpdate(final int complicationId,final int dataType, final ComplicationManager complicationManager) {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {
                        PendingResult<DataItemBuffer> result = Wearable.DataApi.getDataItems(googleApiClient);
                        result.setResultCallback(new ResultCallback<DataItemBuffer>() {
                            @Override
                            public void onResult(@NonNull DataItemBuffer dataItems) {
                                int temp = -1;
                                int hum = -1;
                                String text = "";
                                for (DataItem item : dataItems) {
                                    if (item.getUri().getPath().equals("/stats/temp")) {
                                        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                                        temp = dataMap.getInt("value");
                                        text += " temp. :" + temp + "C°";
                                    }
                                    else if(item.getUri().getPath().equals("/stats/hum")) {
                                        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                                        hum = dataMap.getInt("value");
                                        text += "hum. :" + hum + "%";
                                    }
                                }
                                ComplicationData complicationData;
                                if(dataType == ComplicationData.TYPE_SHORT_TEXT){
                                    // Type icone + valeur
                                    SharedPreferences sharedPref = getSharedPreferences(
                                            getString(R.string.preference_complications), Context.MODE_PRIVATE);
                                    String type = sharedPref.getString(String.valueOf(complicationId), "none");
                                    Log.d("test",type);
                                    if(!type.equals("none")) {
                                        Icon icon;
                                        if(type.equals("temp")) {
                                            text = temp + "C°";
                                            icon = Icon.createWithResource(getApplicationContext(), R.drawable.ic_thermometer);
                                        }
                                        else {
                                            text = hum + "%";
                                            icon = Icon.createWithResource(getApplicationContext(), R.drawable.ic_humidity);
                                        }
                                        complicationData = new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                                                .setShortText(ComplicationText.plainText(text))
                                                .setIcon(icon)
                                                .setTapAction(PendingIntent.getActivity(getApplicationContext(),0,new Intent(getApplicationContext(),MainActivity.class),0))
                                                .build();
                                        complicationManager.updateComplicationData(complicationId, complicationData);
                                    }

                                }
                                else {
                                    // Type 2 valeurs (temp et hum)
                                    complicationData = new ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                                            .setLongText(ComplicationText.plainText(text))
                                            .setIcon(Icon.createWithResource(getApplicationContext(), R.drawable.ic_thermometer))
                                            .setTapAction(PendingIntent.getActivity(getApplicationContext(),0,new Intent(getApplicationContext(),MainActivity.class),0))
                                            .build();
                                    complicationManager.updateComplicationData(complicationId, complicationData);
                                }
                                googleApiClient.disconnect();
                                dataItems.release();
                            }
                        });
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        Log.d(TAG,"Error connection.");
                    }
                })
                .build();
        googleApiClient.connect();
    }
}
