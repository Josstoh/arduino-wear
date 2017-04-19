package com.sullygroup.arduinotest;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
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
 * Provider service qui permet de mettre à jour les "Complications"(widget) sur la watchface.
 * Created by jocelyn.caraman on 20/03/2017.
 */

public class TempAndHumProviderService extends ComplicationProviderService {
    private String TAG = "TAHProviderService";
    private GoogleApiClient googleApiClient;
    private GoogleAPIHandler googleAPIHandler;
    private Looper looper;

    @Override
    public void onComplicationUpdate(final int complicationId,final int dataType, final ComplicationManager complicationManager) {
        Log.d(TAG,"updating " + complicationId + "thread " + getMainLooper().toString());
        HandlerThread myThread = new HandlerThread("BTHandlerThread");
        myThread.start();
        looper = myThread.getLooper();
        googleAPIHandler = new GoogleAPIHandler(looper);
        Message m = googleAPIHandler.obtainMessage(1,complicationId,dataType,complicationManager);
        googleAPIHandler.sendMessage(m);
    }

    private class GoogleAPIHandler extends Handler {

        GoogleAPIHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(final Message msg) {
            Log.d(TAG,"handler début");
            if(msg.what == 1){
                googleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                    .addApi(Wearable.API)
                    .build();
                ConnectionResult connectionResult = googleApiClient.blockingConnect();
                if(connectionResult.isSuccess()){
                    Log.d(TAG, "onconnected " + msg.arg1+ "thread " + getMainLooper().toString());
                    PendingResult<DataItemBuffer> result = Wearable.DataApi.getDataItems(googleApiClient);
                    DataItemBuffer dataItems = result.await();
                    Log.d(TAG, "onresult " + msg.arg1 + " status " + dataItems.getStatus()+ "thread " + getMainLooper().toString());
                    int temp = -1;
                    int hum = -1;
                    String text = "";
                    for (DataItem item : dataItems) {
                        if (item.getUri().getPath().equals("/stats/temp")) {
                            DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                            temp = dataMap.getInt("value");
                            text += " temp. :" + temp + "C°";
                        } else if (item.getUri().getPath().equals("/stats/hum")) {
                            DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                            hum = dataMap.getInt("value");
                            text += "hum. :" + hum + "%";
                        }
                    }
                    ComplicationData complicationData;
                    if (msg.arg2 == ComplicationData.TYPE_SHORT_TEXT) {
                        Log.d(TAG, "short " + msg.arg1);
                        // Type icone + valeur
                        SharedPreferences sharedPref = getSharedPreferences(
                                getString(R.string.preference_complications), Context.MODE_PRIVATE);
                        String type = sharedPref.getString(String.valueOf(msg.arg1), "none");
                        if (!type.equals("none")) {
                            Icon icon;
                            if (type.equals("temp")) {
                                text = temp + "C°";
                                icon = Icon.createWithResource(getApplicationContext(), R.drawable.ic_thermometer);
                            } else {
                                text = hum + "%";
                                icon = Icon.createWithResource(getApplicationContext(), R.drawable.ic_humidity);
                            }
                            complicationData = new ComplicationData.Builder(ComplicationData.TYPE_SHORT_TEXT)
                                    .setShortText(ComplicationText.plainText(text))
                                    .setIcon(icon)
                                    .setTapAction(PendingIntent.getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(), MainActivity.class), 0))
                                    .build();
                            ((ComplicationManager)msg.obj).updateComplicationData(msg.arg1, complicationData);
                            Log.d(TAG, "updated " + msg.arg1 + " " + text);
                        } else
                            Log.d(TAG, "error type in preference file " + msg.arg1);

                    } else {
                        Log.d(TAG, "long " + msg.arg1);
                        // Type 2 valeurs (temp et hum)
                        complicationData = new ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                                .setLongText(ComplicationText.plainText(text))
                                .setIcon(Icon.createWithResource(getApplicationContext(), R.drawable.ic_thermometer))
                                .setTapAction(PendingIntent.getActivity(getApplicationContext(), 0, new Intent(getApplicationContext(), MainActivity.class), 0))
                                .build();
                        ((ComplicationManager)msg.obj).updateComplicationData(msg.arg1, complicationData);
                        Log.d(TAG, "updated " + msg.arg1 + " " + text);
                    }
                    dataItems.release();
                } else {
                    Log.d(TAG,"error " + msg.arg1);
                }
            }
            Log.d(TAG,"handler fin");
            super.handleMessage(msg);
        }
    }
}
