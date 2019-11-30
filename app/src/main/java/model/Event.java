package model;

/**
 * Eventi gestiti dall'applicazione.
 */
public enum Event {
    CONNECTION_ESTABLISHED,
    TRYING_TO_CONNECT,
    DISCONNECTED,
    CONNECTION_FAILED,
    CAR_CLOSED,
    CAR_NOT_CLOSED,
    APPPLICATION_STOPPED, // Applicazione stoppata dall'utente attraverso il comando "Disconnect" dalla MainActivity
    BLUETOOTH_DISABLED,
    MESSAGE_RECEIVED,
    DEVICE_NOT_FOUND, //Il device cercato non è presente tra i dispositivi accoppiati al telefono
    NO_DEVICES_PAIRED //La lista dei device accoppiati al telefono è vuota
}
