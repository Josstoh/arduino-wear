package com.sullygroup.arduinotest;

import android.app.PendingIntent;
import android.content.Intent;
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
                                for (DataItem item : dataItems) {
                                    if (item.getUri().getPath().equals("/temp")) {
                                        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                                        float temp = dataMap.getLong("temp");
                                        Log.d("provider",temp+"");
                                        ComplicationData complicationData = new ComplicationData.Builder(ComplicationData.TYPE_LONG_TEXT)
                                                .setLongText(ComplicationText.plainText(temp+""))
                                                .setIcon(Icon.createWithResource(getApplicationContext(), R.drawable.ic_cc_checkmark))
                                                .setTapAction(PendingIntent.getActivity(getApplicationContext(),0,new Intent(getApplicationContext(),MainActivity.class),0))
                                                .build();

                                        complicationManager.updateComplicationData(complicationId, complicationData);
                                    }
                                }
                                googleApiClient.disconnect();
                            }
                        });
                    }

                    @Override
                    public void onConnectionSuspended(int i) {

                    }
                })
                .build();
        googleApiClient.connect();
    }
}
