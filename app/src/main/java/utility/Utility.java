package utility;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Bundle;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;


/**
 * Classe che contiene metodi statici per effettuare operazioni di utilità come leggere/scrivere su uno stream di dati,
 * leggere/salvare impostazioni ecc...
 */
public final class Utility {

    private static final String minimumProbabilityKey = "probability";
    private static final String defaultAddressKey = "address";

    private Utility() { }

    /**
     * Manda una stringa su uno stream di dati.
     * La funzione provvede ad inserire il carattere terminatore (\n) alla fine della stringa prima di mandarla.
     * @param output
     * @param s stringa da inviare
     * @throws IOException
     */
    public static void sendToStream(final OutputStream output, final String s) throws IOException {
        final byte[] bytes = (s + '\n').getBytes();
        output.write(bytes, 0, bytes.length);
        output.flush();
    }

    /**
     * Legge da uno stream di dati una stringa. La funzione riconosce come terminatore il carattere '\n',
     * se manca il metodo entra in un loop infinito.
     * @param input stream di dati da cui leggere la stringa
     * @return la stringa letta
     * @throws IOException
     */
    public static String readFromStream(final InputStream input) throws IOException{
        final int lenght = 1024;
        final byte[] bytes = new byte[lenght];
        boolean exit = false;
        final StringBuilder string = new StringBuilder();

        do {
            int receiveData = input.read(bytes, 0, lenght);

            if(receiveData > 0) {
                final char c = (char) bytes[receiveData - 1];

                if(c == '\n' || c == 13) {
                    receiveData--;
                    exit = true;
                }

                string.append(new String(bytes, 0, receiveData));

            } else {
                exit = true;
            }
        } while(!exit);

        return string.toString();
    }

    /**
     * Viene memorizzato l'indirizzo del device settato dall'utente come default.
     * @param context
     * @param address
     * @throws IOException
     */
    public static void saveDefaultDeviceAddress(final Context context, final String address) throws IOException {
        final Bundle settings = new Bundle();
        settings.putInt(minimumProbabilityKey, getMinimumProbability(context));
        settings.putString(defaultAddressKey, address);
        writeSettings(context, settings);
    }

    /**
     * Viene memorizzata la probabilità minima di allarme settata come default.
     * @param context
     * @param probabilità
     * @throws IOException
     */
    public static void saveDefaultProbability(final Context context, final int probabilità) throws IOException {
        final Bundle settings = new Bundle();
        settings.putInt(minimumProbabilityKey, probabilità);
        settings.putString(defaultAddressKey, getDefaultDeviceAddress(context));
        writeSettings(context, settings);
    }

    /**
     * Restituisce l'indirizzo del device impostato come default.
     * Quando l'applicazione è installata il device di default è HC-05, poi l'utente potrà cambiarlo dalla schermata impostazioni.
     * La funzione esegue le seguenti operazioni:
     * - Controlla se è presente un'impostazione salvata dall'utente
     * - Se è presente controlla se l'indirizzo salvato appartiene ad un device che è accoppiato con il telefono
     * - Se si, restituisce l'indirizzo
     * - Altrimenti significa che l'utente ha disaccoppiato il device, quindi
     *   carica i nomi dei dispositivi di default e controlla se sono accoppiati
     * - Se anche questa ricerca fallisce, resituisce stringa vuota, altrimenti resituisce l'indirizzo del device.
     *
     * @param context
     * @return
     */
    public static String getDefaultDeviceAddress(final Context context) {
        final Bundle settings = readSettings(context);

        if(!settings.isEmpty()) {
            final String address = settings.getString(defaultAddressKey);
            if (getDeviceByAddress(address) != null) {
                return address;
            }
        }

        for (String s : Settings.DEFAULT_DEVICE_NAMES) {
            BluetoothDevice device = getDeviceByName(s);
            if(device != null) {
                return device.getAddress();
            }
        }

        return "";
    }

    /**
     * Viene restuita la probabilità minima di allarme con lui l'utente deve essere avvisato in caso di "non chiusura della macchina".
     * Restituisce il valore salvato dall'utente, il valore di default altrimenti.
     * @param context
     * @return
     */
    public static int getMinimumProbability(final Context context) {
        final Bundle settings = readSettings(context);
        return settings.isEmpty()? Settings.DEFAULT_MINIMUM_PROBABILITY : settings.getInt(minimumProbabilityKey);
    }

    /**
     * Funzione che trova un device accoppiato a partire dal suo nome.
     * Occore prima effettuare l'operazione di pairing, questa funzione non effettua discovery di device nelle vicinanze per trovare
     * quello cercato!
     * @param name Nome del device che si vuole cercare
     * @return device trovato, null altrimenti
     */
    public static BluetoothDevice getDeviceByName(final String name) {
        for(BluetoothDevice b : BluetoothAdapter.getDefaultAdapter().getBondedDevices()) {
            if(b.getName().equals(name)) {
                return b;
            }
        }

        return null;
    }

    /**
     * Trova un device accoppiato a partire dal suo indirizzo fisico.
     * Occore prima effettuare l'operazione di pairing, questa funzione non effettua discovery!
     * @param address Indirizzo fisico del device che si vuole cercare
     * @return device trovato, null altrimenti
     */
    public static BluetoothDevice getDeviceByAddress(final String address){
        for(BluetoothDevice b : BluetoothAdapter.getDefaultAdapter().getBondedDevices()) {
            if(b.getAddress().equals(address)) {
                return b;
            }
        }
        return null;
    }

    /* Scrive su file le impostazioni salvate dall'utente */
    private static void writeSettings(final Context context, final Bundle settings) throws IOException {
        final ObjectOutputStream output = new ObjectOutputStream(context.openFileOutput(Settings.SETTINGS_FILENAME, Context.MODE_PRIVATE));
        output.writeUTF(settings.getString(defaultAddressKey));
        output.writeInt(settings.getInt(minimumProbabilityKey));
        output.close();
    }

    /* Legge le impostazioni salvate sul file. */
    private static Bundle readSettings(final Context context) {
        try {
            final ObjectInputStream input = new ObjectInputStream(context.openFileInput(Settings.SETTINGS_FILENAME));
            final Bundle settings = new Bundle();
            settings.putString(defaultAddressKey, input.readUTF());
            settings.putInt(minimumProbabilityKey, input.readInt());
            input.close();
            return settings;
        } catch (IOException e) {e.printStackTrace();}

        return Bundle.EMPTY;
    }
}