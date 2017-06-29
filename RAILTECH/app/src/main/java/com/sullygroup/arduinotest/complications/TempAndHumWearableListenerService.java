package com.sullygroup.arduinotest.complications;

import android.content.ComponentName;
import android.support.wearable.complications.ProviderUpdateRequester;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.sullygroup.arduinotest.complications.TempAndHumProviderService;

import java.util.concurrent.TimeUnit;

/**
 * Service qui permet de savoir lorsqu'il y a des changements de température/humidité/etc... en
 * tâche de fond. Et de pouvoir approvisionner {@link TempAndHumProviderService}
 * Created by jocelyn.caraman on 20/03/2017.
 */
public class TempAndHumWearableListenerService extends WearableListenerService {

    private static final String TAG = "TAHWearServ";
    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String DATA_ITEM_RECEIVED_PATH = "/data-item-received";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG,"onCreate");
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onDataChanged: " + dataEvents);
        }

        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();

        ConnectionResult connectionResult =
                googleApiClient.blockingConnect(30, TimeUnit.SECONDS);

        if (!connectionResult.isSuccess()) {
            Log.e(TAG, "Failed to connect to GoogleApiClient.");
            dataEvents.release();
            return;
        }

        // Notifie le provider de se mettre à jour.
        ComponentName providerComponentName = new ComponentName(this,TempAndHumProviderService.class);
        ProviderUpdateRequester providerUpdateRequester = new ProviderUpdateRequester(this, providerComponentName);
        providerUpdateRequester.requestUpdateAll();

        dataEvents.release();
    }
}
