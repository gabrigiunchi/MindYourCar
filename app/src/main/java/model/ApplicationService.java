package model;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import java.io.FileOutputStream;
import java.io.IOException;
import mindyourcar.mindyourcar.R;
import utility.Settings;
import utility.Utility;
import view.MainActivity;
import static model.Event.CAR_CLOSED;
import static model.Event.CAR_NOT_CLOSED;
import static model.MyIntentFilter.CLOSE_CONNECTION;
import static model.MyIntentFilter.SET_DEVICE;
import static model.MyIntentFilter.STOP_SERVICE;


/**
 * Service che implementa la logica dell'applicazione.
 * Una volta avviato continua a lavorare in background finchè l'applicazione non viene chiusa.
 * Come ogni service viene attivato con un Intent esplicito, nell'Intent può essere inserito un indirizzo fisico di un device
 * attraverso il metodo Intent.putStringExtra("address", stringaIndirizzo). Il Service si connetterà quindi a quell'indirizzo
 * Inoltre resta in ascolto di tre Intent attraverso un LocalBroadcastReceiver:
 *
 * - SET_DEVICE, permette connettersi ad un altro dispositivo. Nell'Intent va inserito l'indirizzo del dispositivo usando il metodo
 * Intent.putStringExtra("address", stringaIndirizzo)
 *
 * - CLOSE_CONNECTION, chiude la connessione bluetooth e interrompe l'applicazione.
 *
 * - STOP_SERVICE, interrompe la computazione, chiude la connessione e termina il service.
 *
 * N.B : Questi Intent devono essere mandati usando il metodo LocalBroadcastManager.sendBroadcast(intent) in quanto
 *       questo Service utilizza un receiver locale
 */
public class ApplicationService extends IntentService {

    private ConnectionHandlerThread connectionHandlerThread;
    private final MyBroadcastReceiver myBroadcastReceiver = new MyBroadcastReceiver();
    private int actualProbability = -1;
    private long lastUpdateTime;
    private boolean stop;

    public ApplicationService() {
        super("ApplicationService");
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        Log.d("AndroidCar", "service partito");
        this.setupBroadcastReceiver();

        final String address = intent.getStringExtra("address");

        if(address != null && !address.isEmpty()){
            this.startApplicationService(address);
        }

        while (!stop) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(this.myBroadcastReceiver);
        this.stopComputing();
        stopSelf();
    }

    public void notifyEvent(Event event, String message) {
        switch (event) {
            case MESSAGE_RECEIVED:
                try {
                    this.actualProbability = Integer.parseInt(message);
                }catch (Exception e) {e.printStackTrace();}

                this.lastUpdateTime = System.currentTimeMillis();
                this.sendBroadcast(event, message);
                break;

            case DISCONNECTED: this.valutaChiusuraMacchina(); break;
            default: this.sendBroadcast(event, message); break;
        }
    }

    private void startApplicationService(final String address) {

        // Se sono già connesso al dispositivo non interrompo la connessione
        if(this.connectionHandlerThread != null && this.connectionHandlerThread.isConnectedWith(address)) {
            Log.d("AndroidCar", "Già connesso al dispositivo");
        } else {
            this.stopComputing();
            final BluetoothDevice device = Utility.getDeviceByAddress(address);
            this.connectionHandlerThread = new ConnectionHandlerThread(device, this);
            this.connectionHandlerThread.start();
        }
    }

    /* Per mandare un Intent implicito attraverso il LocalBroadcastManager in modo più semplice. */
    private void sendBroadcast(final Event event, final String s) {
        final Intent intent = new Intent(event.name());
        final Bundle bundle = new Bundle();
        bundle.putString("message", s);
        intent.putExtras(bundle);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
    }

    private void stopComputing() {
        if(this.connectionHandlerThread != null) {
            Log.d("AndroidCar", "Termino connectionHandler");
            this.connectionHandlerThread.stopComputing();
        }
    }

    private void valutaChiusuraMacchina() {
        Log.d("AndroidCar", "Valuto chiusura macchina");

        // Probabilità attuale minore di quella minima ---> lancio allarme
        if(this.actualProbability <= Utility.getMinimumProbability(getApplicationContext())) {
            Log.d("AndroidCar", "Non hai chiuso la macchina!");
            this.notifyEvent(CAR_NOT_CLOSED, Integer.toString(this.actualProbability));
            this.sendNotification();

        } else {
            final long time = System.currentTimeMillis() - this.lastUpdateTime;

            if(time < 15000) {
                this.actualProbability += 35;
            } else if(time > 15000 && time < 30000){
                this.actualProbability += 25;
            } else if(time > 30000 && time < 45000){
                this.actualProbability += 15;
            } else {
                this.actualProbability += 5;
            }

            Log.d("AndroidCar", "Hai chiuso la macchina al " + this.actualProbability + "%");
            this.notifyEvent(CAR_CLOSED, this.actualProbability + "");
        }
    }

    private void setupBroadcastReceiver() {
        // Uso un LocalBroadcastReceiver per non mandare i miei intent fuori dall'applicazione
        final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastManager.registerReceiver(this.myBroadcastReceiver, new IntentFilter(SET_DEVICE.name()));
        localBroadcastManager.registerReceiver(this.myBroadcastReceiver, new IntentFilter(CLOSE_CONNECTION.name()));
    }

    private void sendNotification() {
        final NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        final Intent i = new Intent(this, MainActivity.class);
        final PendingIntent pi = PendingIntent.getActivity(this, 1, i, 0);

        // Creo la notifca
        NotificationCompat.Builder notifica = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Mind Your Car")
                .setContentText("Non hai chiuso la macchina!");

        // Carico il suono di avviso
        final Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Setto i parametri della notifica
        notifica.setSound(sound);
        notifica.setAutoCancel(true);
        notifica.setContentIntent(pi);

        // Lancio la notifica
        notificationManager.notify(1, notifica.build());

        // Scrivo su file il fatto che ho mandato la notifica
        try {
            final FileOutputStream outputStream = openFileOutput(Settings.NOTIFICATION_FILENAME, MODE_PRIVATE);
            outputStream.write(1);
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopService() {
        this.stop = true;
    }

    private final class MyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action.equals(SET_DEVICE.name())) {
                final String address = intent.getStringExtra("address");
                startApplicationService(address);

            } else if(action.equals(CLOSE_CONNECTION.name())) {
                stopComputing();
            } else if(action.equals(STOP_SERVICE.name())) {
                stopService();
            }
        }
    }
}
