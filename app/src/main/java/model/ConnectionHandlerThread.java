package model;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;

import utility.Settings;
import utility.Utility;

/**
 * Thread che gestisce la connessione bluetooth con il device, scambia/invia i messaggi.
 * Non termina finchè non viene invocato il metodo stopComputing().
 * Questo thread può trovarsi in 3 stati consecutivi :
 *
 * - Tentativo di connessione: continua finchè non riesce a connettersi
 * - Gestione comunicazione: invia/riceve dati con il dispositivo a cui è connesso.
 *                           Esce da questo stato quando la connessione viene persa
 *
 * - Connessione persa: In questa fase aspetta 10 secondi prima di passare allo stato 1
 *
 *
 *
 */
public final class ConnectionHandlerThread extends Thread {

    private final ApplicationService service;
    private final BluetoothDevice device;
    private BluetoothSocket socket;
    private volatile boolean stop; // Per stoppare il thread

    public ConnectionHandlerThread(final BluetoothDevice device, final ApplicationService service) {
        this.service = service;
        this.device = device;
        this.stop = false;
    }

    @Override
    public void run() {
        /* Finchè non viene terminato prova a connettersi al device e ad interagire con lui. */
        while(!stop) {

            if(this.connect()) {
                service.notifyEvent(Event.CONNECTION_ESTABLISHED, this.device.getName());
                this.handleConnection();
            }

            // Aspetto 10 secondi prima di ricominciare a connettermi
            try {
                sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @return true se la connessione viene stabilita con successo, false altrimenti
     */
    private boolean connect() {
        int sleepTime = 0;

        while(!this.stop) {

            Log.d("AndroidCar", "Provo a connettermi a " + this.device.getName());
            service.notifyEvent(Event.TRYING_TO_CONNECT, this.device.getName());

            // Se il bluetooth è disattivato interrompo il thread e lo notifico al service
            if(!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                Log.d("AndroidCar", "Bluetooth disattivato mentre tentavo di connettermi");
                service.notifyEvent(Event.BLUETOOTH_DISABLED, "");
                this.stopComputing();
                return false;
            }

            // Tento di creare la connessione
            try {
                this.socket = this.device.createRfcommSocketToServiceRecord(Settings.MY_UUID);
                this.socket.connect();
                Log.d("AndroidCar", "Connesso a " + device.getName());
                return true;
            } catch (IOException e) {e.printStackTrace();}

            try {
                sleep(this.stop? 0: sleepTime);
            } catch (InterruptedException e) { e.printStackTrace(); }

            sleepTime += sleepTime < 10000? 1000 : 0; // Aumento il tempo fino ad arrivare ad un massimo di 10 secondi
        }

        return false;
    }

    private void handleConnection() {
        boolean stopHandlingConnection = false;

        while(!stop && !stopHandlingConnection) {
            try {
                String receive = Utility.readFromStream(this.socket.getInputStream());
                Log.d("AndroidCar", "Ricevuto: " + receive);
                service.notifyEvent(Event.MESSAGE_RECEIVED, receive);
                sleep(100);
            } catch (IOException | IllegalStateException e) {
                this.closeConnection();

                if(!this.stop){
                    service.notifyEvent(Event.DISCONNECTED, this.device.getName());
                }

                stopHandlingConnection = true;
            } catch (InterruptedException e1) { e1.printStackTrace(); }
        }
    }

    /**
     * Interrompe il thread e chiude la connessione bluetooth.
     */
    public void stopComputing() {
        this.stop = true;
        this.closeConnection();
    }

    /**
     * Consente di sapere se il thread ha una connessione attiva con un dispositivo avente l'indirizzo fisico passato
     *
     * @param address
     * @return
     */
    public boolean isConnectedWith(final String address) {
        return this.socket.isConnected() && this.device.getAddress().equals(address);
    }

    private void closeConnection() {
        try {
            this.socket.close();
        } catch (IOException e) {e.printStackTrace();}
    }
}