package model;


/**
 * IntentFilter registrati dal BroadcastReceiver di ApplicationService.
 */
public enum MyIntentFilter {
    SET_DEVICE, // Per indicare il device con cui connettersi
    CLOSE_CONNECTION, // Per interrompere l'applicazione
    STOP_SERVICE // Interrompe il service
}
