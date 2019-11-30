package view;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mindyourcar.mindyourcar.R;
import utility.Utility;


/**
 * Activity per gestire le impostazioni.
 * Da questa schermata si può cambiare il dispositivo di default e la probabilità minima di allarme.
 */
public class SettingsActivity extends AppCompatActivity {

    private TextView text;
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        this.text = (TextView)findViewById(R.id.valueProbabilità);
        final TextView selectionDeviceText = (TextView)findViewById(R.id.devicesSelectionText);
        this.button = (Button)findViewById(R.id.button);
        this.setupSeekBar();

        if(BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            final List<BluetoothDevice> devices = new ArrayList<>(BluetoothAdapter.getDefaultAdapter().getBondedDevices());

            // Non ci sono device accoppiati con il telefono
            if(devices.isEmpty()) {
                selectionDeviceText.setText("Nessun device accoppiato");
                this.button.setVisibility(View.INVISIBLE);
            } else {
                final Bundle bundle = new Bundle(); // Bundle per segnarmi la posizione in lista del device di default
                final String defaultDeviceAddress = Utility.getDefaultDeviceAddress(this.getApplicationContext());

                // Se la stringa è vuota significa che il device di default non è accoppiato con il telefono
                if(defaultDeviceAddress.isEmpty()) {
                    button.setText(devices.get(0).getName());
                } else {
                    // Cerco il nome associato all'indirizzo del device di default
                    for(BluetoothDevice d : devices) {
                        if(d.getAddress().equals(defaultDeviceAddress)) {
                            button.setText(d.getName());
                            bundle.putInt("index", devices.indexOf(d)); // Mi segno la posizione del device nella lista
                        }
                    }
                }

                this.button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        /* Quando clicco il bottone mostro la lista dei device che posso scegliere.
                           Il secondo parametro è l'opzione già 'cliccata', riprendo dal bundle la posizione salvata in precedenza
                         */
                        showDeviceList(devices, bundle.getInt("index"));
                    }
                });
            }
        } else {
            selectionDeviceText.setVisibility(View.INVISIBLE);
            this.button.setVisibility(View.INVISIBLE);
        }
    }

    private void showDeviceList(final List<BluetoothDevice> devices, final int defaultDevicePosition) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Scegli device");

        // Bundle per ricordare posizione del device scelto
        final Bundle selected = new Bundle();
        selected.putInt("selected", 0);

        // Creo array di CharSequence da inserire nel Dialog
        final CharSequence[] array = new CharSequence[devices.size()];
        for (int i = 0; i < devices.size(); i++) {
            array[i] = devices.get(i).getName();
        }

        // Inserisco nel dialog la lista dei device
        builder.setSingleChoiceItems(array, defaultDevicePosition, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Quando seleziono un device dalla lista memorizzo la sua posizione nel bundle
                selected.putInt("selected", which);
            }
        });

        // Bottone per salvare la scelta
        builder.setPositiveButton("Scegli", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Riprendo la posizione salvata nel bundle e cambio il device di default
                final BluetoothDevice device = devices.get(selected.getInt("selected"));
                button.setText(device.getName());
                try {
                    Utility.saveDefaultDeviceAddress(getApplicationContext(), device.getAddress());
                    Log.d("AndroidCar", "Settato default device: " + device.getName());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        // Bottone cancella
        builder.setNegativeButton("Cancella", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) { }
        });

        builder.create().show();
    }

    private void setupSeekBar() {
        final SeekBar seekBarProbabilità = (SeekBar)findViewById(R.id.seekBarProbabilità);
        final int defaultProbabilità = Utility.getMinimumProbability(this.getApplicationContext());

        // Setto la GUI (seekBar e relativo testo che mostra il valore)
        seekBarProbabilità.setProgress(defaultProbabilità / (100 / seekBarProbabilità.getMax()));
        this.text.setText(defaultProbabilità + "/" + seekBarProbabilità.getMax() * (100 / seekBarProbabilità.getMax()));

        seekBarProbabilità.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private int value;
            // La seekBar ha come massimo 20 ma io voglio mostrare 100, questo rapporto mi aiuta nei calcoli
            private final int rapporto = 100 / seekBarProbabilità.getMax();

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                this.value = progress;
                text.setText(this.value * rapporto + "/" + seekBar.getMax() * rapporto);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                try {
                    final int actualValue = this.value * this.rapporto;
                    Utility.saveDefaultProbability(getApplicationContext(), actualValue);
                    Log.d("AndroidCar", "Settata probabilità minima a " + actualValue);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
