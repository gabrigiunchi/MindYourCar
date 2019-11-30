package utility;

import android.graphics.Color;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Classe che contiene impostazioni di default e altre informazioni utili.
 */
public final class Settings {

    private Settings() {}

    private static List<String> getDefaultDeviceNames() {
        final List<String> list = new ArrayList<>();
        Collections.addAll(list, "HC-05", "HC-06");
        return Collections.unmodifiableList(list);
    }

    // File dove salvare le impostazioni dell'utente (device di default ecc)
    public static final String SETTINGS_FILENAME = "settings.bin";

    // File dove segnare il fatto che ho inviato la notifica
    public static final String NOTIFICATION_FILENAME = "notifica.bin";

    public static final int DEFAULT_MINIMUM_PROBABILITY = 40;
    public static final List<String> DEFAULT_DEVICE_NAMES = getDefaultDeviceNames();
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // Colore da mostrare nella progressBar quando è probabile che la macchina sia chiusa
    public static final int CAR_CLOSED_COLOR = Color.rgb(19, 166, 13);

    // Colore da mostrare nella progressBar quando la macchina non è chiusa
    public static final int CAR_UNCLOSED_COLOR = Color.rgb(217, 0, 0);

}
