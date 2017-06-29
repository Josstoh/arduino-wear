package com.sullygroup.arduinotest.services;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
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
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.sullygroup.arduinotest.utils.Tools;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Service qui gère la connexion avec la carte Arduino par l'intermédiaire du service {@link BluetoothService}
 * et la gestion des données échangées entre la carte et la montre connectée.
 * Created by jocelyn.caraman on 16/03/2017.
 */
public class TempAndHumService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        DataApi.DataListener, MessageApi.MessageListener{
    public final static String TAG = "TempAndHumService";

    // Constantes
    public static final String TEMPERATURE_CMD = "a";
    public static final String HUMIDITY_CMD = "b";
    public static final String ROTATE_CMD = "c";
    public static final String COLOR_CMD = "d";
    public static final String TEMP_AND_HUM_CMD = "e";

    public static final String EVENT_RESPONSE_RECEIVED = "event_response_received";
    public static final String EVENT_DEVICE_CONNECTED = "event_device_connected";
    public static final String EVENT_BTSERVICE_FAILED = "event_btservice_failed";
    public static final String EVENT_SEND_REQUEST = "event_send_request";
    public static final String EVENT_BTSERVICE_CLOSE = "event_btservice_close";

    public static  final int TYPE_TEMP = 0;
    public static  final int TYPE_HUM = 1;
    public static  final int TYPE_ROT = 2;

    public static final String PATH_TEMP = "/stats/temp";
    public static final String PATH_HUM = "/stats/hum";
    public static final String PATH_ROT = "/rotate";
    public static final String PATH_COLOR = "/color";

    public static  final int TEMP_AND_HUM_JOB_ID = 0;
    public static  final int ROTATE_JOB_ID = 1;
    public static  final int LED_COLOR_JOB_ID = 2;

    public static final long DELAIS_RAFFRAICHISSEMENT_TEMP_AND_HUM = 10000;

    public static final String CONNECT_TO_ARDUINO_CAPABILITY = "connect_to_arduino";
    public static final String CONNECT_TO_ARDUINO_MESSAGE_PATH = "/connect_to_arduino";

    // -------------------------------------------------------------------------------------
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private static final String DIALOG_ERROR = "dialog_error";
    private boolean mResolvingError = false;
    private static final String STATE_RESOLVING_ERROR = "resolving_error";

    private JobScheduler mJobScheduler;
    GoogleApiClient mGoogleApiClient;

    private final IBinder mBinder = new LocalBinder();
    private TempAndHumServiceListener activity;
    private TempAndHumService _this = this;
    private ConnectionMode mode = ConnectionMode.BT;

    CapabilityApi.CapabilityListener capabilityListener;
    // Node qui est connecté à la carte Arduino (le téléphone)
    private String ConnectedToArduinoNodeId = null;

    /**
     * BroadcastReceiver qui reçoit les messages envoyer grâce au LocalBroadcastManager.
     */
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, intent.getAction());
            float t,h;
            switch(intent.getAction())
            {
                case EVENT_DEVICE_CONNECTED:
                    isConnectedToDevice = true;
                    if(activity != null)
                        activity.onConnected();

                    scheduleTempAndHumCMD(DELAIS_RAFFRAICHISSEMENT_TEMP_AND_HUM);
                    break;
                case EVENT_RESPONSE_RECEIVED:
                    if(activity != null)
                        activity.updateTime();
                    t = intent.getFloatExtra("temp",-100f);
                    if(t != -100){
                        temp = (int) t;
                        if(activity != null)
                            activity.onTempResponse(t);
                        updateDataItem(TYPE_TEMP, temp);
                    }
                    h = intent.getFloatExtra("hum",-100f);
                    if(h != -100){
                        hum = (int) h;
                        if(activity != null)
                            activity.onHumResponse(h);
                        updateDataItem(TYPE_HUM, hum);
                    }

                    scheduleTempAndHumCMD(DELAIS_RAFFRAICHISSEMENT_TEMP_AND_HUM);

                    break;
                case EVENT_BTSERVICE_FAILED:
                    // il y a eu une erreur dans le BTService, on stoppe ce service aussi...
                    Log.d(TAG,"BTService failed...");
                    Toast.makeText(getApplicationContext(),intent.getStringExtra("message"),Toast.LENGTH_LONG).show();
                    //stop();
                    break;
            }
        }
    };
    private Intent btServiceIntent;
    private boolean isConnectedToDevice = false;
    private int temp = -1;
    private int hum = -1;
    private int rotate = -1;

    private static boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();

        isRunning = true;
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
        mJobScheduler = (JobScheduler) getSystemService( Context.JOB_SCHEDULER_SERVICE );
        btServiceIntent = new Intent(TempAndHumService.this, BluetoothService.class);

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(EVENT_RESPONSE_RECEIVED));
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(EVENT_DEVICE_CONNECTED));
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(EVENT_BTSERVICE_FAILED));

        Log.d(TAG, "SERVICE CREATED");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "startcommand");
        if(intent != null) {
            String action = intent.getAction();
            if(action != null)
            {
                if(action.equals("STOP_SERVICE")){
                    Log.d(TAG,"stop");
                    stop();
                    return START_NOT_STICKY;
                }
            }
            mode = intent.getBooleanExtra("bt_mode",true) ? ConnectionMode.BT : ConnectionMode.PHONE;
            return START_STICKY;
        }
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected: " + bundle);
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Connected to Google Api Service");
        }
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient,this);

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

        Wearable.CapabilityApi.getCapability(mGoogleApiClient, CONNECT_TO_ARDUINO_CAPABILITY,
                CapabilityApi.FILTER_REACHABLE)
                .setResultCallback(new ResultCallback<CapabilityApi.GetCapabilityResult>() {
                    @Override
                    public void onResult(@NonNull CapabilityApi.GetCapabilityResult getCapabilityResult) {
                        updateConnectToArduinoCapability(getCapabilityResult.getCapability());
                    }
                });

        PendingResult<DataItemBuffer> result = Wearable.DataApi.getDataItems(mGoogleApiClient);
        result.setResultCallback(new ResultCallback<DataItemBuffer>() {
            @Override
            public void onResult(@NonNull DataItemBuffer dataItems) {
                for (DataItem item : dataItems) {
                    if (item.getUri().getPath().equals("/stats/temp")) {
                        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                        int temp = dataMap.getInt("value");
                        //updateTemp(temp);
                    }
                    else if(item.getUri().getPath().equals("/stats/hum")) {
                        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                        int hum = dataMap.getInt("value");
                        //updateHum(hum);
                    }
                }
                dataItems.release();
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended: " + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed: " + connectionResult);
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (connectionResult.hasResolution()) {
            try {
                mResolvingError = true;
                connectionResult.startResolutionForResult((Activity)activity, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                mGoogleApiClient.connect();
            }
        } else {
            // Show dialog using GoogleApiAvailability.getErrorDialog()
            showErrorDialog(connectionResult.getErrorCode());
            mResolvingError = true;
        }
    }

    /**
     * Fonction appelée à chaque fois qu'un data item est modifié.
     */
    @Override
    public void onDataChanged(DataEventBuffer dataEventBuffer) {
        for (DataEvent event : dataEventBuffer) {
            if(activity != null) {
                if (event.getType() == DataEvent.TYPE_DELETED) {
                    Log.d(TAG, "DataItem deleted: " + event.getDataItem().getUri());
                } else if (event.getType() == DataEvent.TYPE_CHANGED) {
                    Log.d(TAG, "DataItem changed: " + event.getDataItem().getUri());
                    DataItem item = event.getDataItem();
                    if (item.getUri().getPath().compareTo("/stats/temp") == 0) {
                        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                        activity.onTempResponse((float)dataMap.getInt("value"));
                    }
                    else if(item.getUri().getPath().compareTo("/stats/hum") == 0) {
                        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                        activity.onHumResponse((float)dataMap.getInt("value"));
                    }
                    else if(item.getUri().getPath().compareTo("/rotate") == 0) {
                        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                        activity.onRotateModified(dataMap.getInt("value"));
                    }
                    else if(item.getUri().getPath().compareTo("/color") == 0) {
                        DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                        //updateColor(dataMap.getInt("red"),dataMap.getInt("red"),dataMap.getInt("blue"));
                    }
                }
            }
        }
        dataEventBuffer.release();
    }

    /**
     * Reçoit les messages de la montre.
     * @param messageEvent
     */
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(TAG,"messageapi reçu");
        if (messageEvent.getPath().equals(CONNECT_TO_ARDUINO_MESSAGE_PATH)) {
            Log.d(TAG,"messageapi type " + CONNECT_TO_ARDUINO_MESSAGE_PATH);
            try {
                String command = new String(messageEvent.getData(), StandardCharsets.UTF_8);
                if(command.startsWith(TempAndHumService.ROTATE_CMD)) {
                    int angle = Integer.valueOf(command.substring(1));
                    requestRotate(angle);
                }
                else if(command.startsWith(TempAndHumService.COLOR_CMD)) {
                    String[] colors = command.substring(1).split(";");
                    requestLEDColor(Integer.valueOf(colors[0]),Integer.valueOf(colors[1]),Integer.valueOf(colors[2]));
                }
            }
            catch(Exception e) {
                Log.e(TAG,"A String Object is requested.");
                e.printStackTrace();
            }
        }
    }

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        TempAndHumService.ErrorDialogFragment dialogFragment = new TempAndHumService.ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        if(activity != null)
            dialogFragment.show(((Activity)activity).getFragmentManager(), "errordialog");
    }

    public void onDialogDismissed() {
        mResolvingError = false;
    }

    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() { }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GoogleApiAvailability.getInstance().getErrorDialog(
                    this.getActivity(), errorCode, REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            //_this.onDialogDismissed();
        }
    }

    /**
     * S'inscrire aux appels de ce service.
     * @param activity l'activité qui veut s'inscrire.
     */
    public void registerClient(Activity activity){
        Log.d(TAG,"registerClient");
        this.activity = (TempAndHumServiceListener)activity;
    }

    /**
     * Désinscrire l'activité aux appels de ce service.
     */
    public void unRegisterClient(){
        Log.d(TAG,"unRegisterClient");
        this.activity = null;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG,"onDestroy");
        super.onDestroy();
        if (null != mGoogleApiClient && mGoogleApiClient.isConnected()) {
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            Wearable.CapabilityApi.removeCapabilityListener(mGoogleApiClient,capabilityListener,CONNECT_TO_ARDUINO_CAPABILITY);
            mGoogleApiClient.disconnect();
        }
        mJobScheduler.cancelAll();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        isRunning = false;
        stopService(btServiceIntent);
    }

    /**
     * Renvoie si le service est connecté à la carte Arduino ou non.
     * @return true si vrai sinon false.
     */
    public boolean isConnectedToDevice()
    {
        return isConnectedToDevice;
    }

    /**
     * Met à jour le data item contenant l'information de type {@param type_value}, afin de
     * partager l'information avec la montre.
     * @param typeValue type de l'information à changer, constantes dans {@link TempAndHumService}.
     * @param value valeur de cette information.
     */
    private void updateDataItem(int typeValue, int value) {
        String path="";
        switch (typeValue){
            case TYPE_TEMP:
                path = PATH_TEMP;
                break;
            case TYPE_HUM:
                path = PATH_HUM;
                break;
            case TYPE_ROT:
                path = PATH_ROT;
                break;
            default:
                Log.e(TAG,"Wrong type...");
        }
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(path);
        putDataMapReq.getDataMap().putInt("value", value);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(final DataApi.DataItemResult result) {
                if(result.getStatus().isSuccess()) {
                    Log.d(TAG, "Data item set: " + result.getDataItem().getUri());
                }
            }
        });
    }

    /**
     * Met à jour le data item contenant les composants RGB de la couleur de la LED, afin de
     * partager l'information avec la montre.
     * @param red composante rouge de RGB.
     * @param green composante verte de RGB.
     * @param blue composante bleue de RGB.
     */
    private void updateColorDataItem(int red, int green, int blue) {
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(PATH_COLOR);
        putDataMapReq.getDataMap().putInt("red", red);
        putDataMapReq.getDataMap().putInt("green", green);
        putDataMapReq.getDataMap().putInt("blue", blue);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(mGoogleApiClient, putDataReq);
        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(final DataApi.DataItemResult result) {
                if(result.getStatus().isSuccess()) {
                    Log.d(TAG, "Data item set: " + result.getDataItem().getUri());
                }
            }
        });
    }

    /**
     * Stoppe le service et libère les ressources utilisées.
     */
    private void stop() {
        if(activity != null)
            activity.onServiceClosing();
        mJobScheduler.cancelAll();
        stopForeground(true);
        stopSelf();
    }

    /**
     * Retourne si une instance du service est déjà lancée.
     * @return true si oui, false sinon.
     */
    public static boolean isRunning() {
        return isRunning;
    }

    /**
     * Envoye une reqûete à la carte Arduino pour changer l'angle du servomoteur.
     * @param angle l'angle entre 0 et 145
     * @return true si l'opération a été effectué, false sinon.
     */
    public boolean requestRotate(int angle) {
        try{
            if(Tools.checkValue(angle,0,145)) {
                Tools.sendCommand(getApplicationContext(),ROTATE_CMD + angle);
                rotate = angle;
                updateDataItem(TYPE_ROT,rotate);
                return true;
            }
            else
            {
                Toast.makeText(getApplicationContext(),"L'angle doit être compris entre 0 et 145° inclus",Toast.LENGTH_LONG).show();
                return false;
            }

        }catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Envoye une reqûete à la carte Arduino pour changer la couleur de la LED.
     * @param red Entier RGB entre 0 et 255 représentant la partie rouge de la couleur.
     * @param green Entier RGB entre 0 et 255 représentant la partie verte de la couleur.
     * @param blue Entier RGB entre 0 et 255 représentant la partie bleue de la couleur.
     */
    public void requestLEDColor(int red, int green, int blue) {
        try{
            if(Tools.checkValueColor(red) && Tools.checkValueColor(green) && Tools.checkValueColor(blue)) {
                Tools.sendCommand(getApplicationContext(),COLOR_CMD + red + ";" + green + ";" + blue);
                updateColorDataItem(red,green,blue);
            }
            else
                Toast.makeText(getBaseContext(),"Each field must be a number between 0 and 255.",Toast.LENGTH_LONG).show();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Envoye une reqûete à la carte Arduino pour obtenir la température et l'humidité actuelles du capteur.
     */
    public void requestTempAndHum() {
        Tools.sendCommand(getApplicationContext(),TEMP_AND_HUM_CMD);
    }

    /**
     * Méthode pour commencer à envoyer des reqûetes d'actualisation des données de manière répété
     * @param latency temps entre les appels à cette méthode
     */
    protected void scheduleTempAndHumCMD(long latency) {
        JobInfo.Builder builder = new JobInfo.Builder( TEMP_AND_HUM_JOB_ID,
                new ComponentName( getPackageName(),
                        TempAndHumJobScheduler.class.getName() ) );
        PersistableBundle bundle = new PersistableBundle(1);
        bundle.putString("command",TEMP_AND_HUM_CMD);
        builder.setExtras(bundle);
        builder.setMinimumLatency(latency);

        if( mJobScheduler.schedule( builder.build() ) <= 0 ) {
            //If something goes wrong
            Log.d(TAG,"JobScheduler : Something went wrong...");
        }
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
        //todo
        for (Node node : nodes) {
            if (node.isNearby()) {
                //connectedTextView.setVisibility(View.GONE);
                return node.getId();
            }
            bestNodeId = node.getId();
        }
        //connectedTextView.setVisibility(View.VISIBLE);
        return bestNodeId;
    }

    /**
     * Pour envoyer un message à l'Arduino. Passe par le téléphone.
     * @param string le message en Bytes à envoyé.
     */
    private void requestConnectToArduino(byte[] string) {
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

    /**
     * Pour envoyer une commande à l'Arduino suivant le mode de connexion
     */
    public void requestCommand(String command, Bundle extras) {
        switch (mode) {
            case BT: {
                switch (command) {
                    case TEMP_AND_HUM_CMD:
                        requestTempAndHum();
                        break;
                    case ROTATE_CMD:
                        if(extras != null) {
                            if(extras.containsKey("angle")) {
                                requestRotate(extras.getInt("angle"));
                            }
                        }
                        else
                            Log.e(TAG,"Cant send rotate request : missing angle argument");
                        break;
                    case COLOR_CMD:
                        if(extras != null) {
                            if(extras.containsKey("red") && extras.containsKey("green") && extras.containsKey("blue")) {
                                requestLEDColor(extras.getInt("red"),extras.getInt("green"),extras.getInt("blue"));
                            }
                        }
                        else
                            Log.e(TAG,"Cant send rotate request : missing angle argument");
                        break;
                }
                break;
            }

            case PHONE:
                requestConnectToArduino(command.getBytes());
                break;
        }
    }

    /**
     * Définit le mode utilisé.
     * @param m
     */
    public void setConnexionMode(ConnectionMode m) {
        Log.d(TAG,"changed mode + " + m.toString());
        if(mode == ConnectionMode.PHONE && m == ConnectionMode.BT) {
            mode = m;
            startService(btServiceIntent);
        }
        else if (mode == ConnectionMode.BT && m == ConnectionMode.PHONE) {
            mode = m;
            stopService(btServiceIntent);
        }

    }

    /**
     * Démarre le service @link {@link BluetoothService}
     */
    public void connectToDevice() {
        if(mode == ConnectionMode.BT && !BluetoothService.isRunning()) {
            startService(btServiceIntent);
        }
    }

    /**
     * Stop le service {@link BluetoothService}
     */
    public void disconnectToDevice() {
        if(mode == ConnectionMode.BT && BluetoothService.isRunning()) {
            Intent stop = new Intent(this, BluetoothService.class);
            stop.setAction("STOP_SERVICE");
            startService(stop);
        }
    }

    /**
     * Binder pour ce service qui sert à obtenir son instance depuis un Context qui s'y est "bind".
     */
    public class LocalBinder extends Binder {
        public TempAndHumService getServiceInstance(){
            return TempAndHumService.this;
        }
    }

    /**
     * Interface implémentée par l'activité pour avoir des retours sur ce que fait le service et les MAJs de données.
     */
    public interface TempAndHumServiceListener{
        /**
         * Le service est lancé et connecté à l'Arduino grâce au BTService.
         */
        void onConnected();

        /**
         * Une réponse de l'Arduino contenant la température à été reçu.
         */
        void onTempResponse(float msg);

        /**
         * Une réponse de l'Arduino contenant l'humidité à été reçu.
         */
        void onHumResponse(float msg);

        /**
         * L'angle de rotation a été modifié, on prévient l'activité pour MAJ.
         */
        void onRotateModified(int angle);

        /**
         * On prévient que le service va s'arrêter.
         */
        void onServiceClosing();

        /**
         * Préviens l'activity de mettre à jour l'heure de capture de la temp et de l'hum
         * pour un prochain relevé
         */
        void updateTime();

    }

    /**
     * Enum pour le type de connexion vers l'Arduino.
     */
    public enum ConnectionMode {BT,PHONE}
}
