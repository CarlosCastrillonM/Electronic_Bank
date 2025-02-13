package com.example.appcarlbanl;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.os.Parcelable;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] intentFiltersArray;
    private EditText editTextName, editTextRead;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextName = findViewById(R.id.editTextName);
        editTextRead = findViewById(R.id.editTextRead);
        btnSave = findViewById(R.id.btnSave);

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Toast.makeText(this, "Este dispositivo no soporta NFC", Toast.LENGTH_LONG).show();
            finish();
        }

        // Intent for detect NFC
        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE);
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        intentFiltersArray = new IntentFilter[]{tagDetected};

        btnSave.setOnClickListener(view -> {
            String textToWrite = editTextName.getText().toString();
            if (!textToWrite.isEmpty()) {
                Toast.makeText(this, "Acerca la tarjeta NFC para escribir...", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "El campo no puede estar vacío", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String textToWrite = editTextName.getText().toString();

            if (!textToWrite.isEmpty()) {
                writeNfcTag(detectedTag, textToWrite);
            }

            // If `readFromNfc()` uses Intent, pass the Intent instead of the Tag
            readFromNfc(intent);
        }
    }

    private void writeNfcTag(Tag tag, String text) {
        NdefMessage message = createNdefMessage(text);
        Ndef ndef = Ndef.get(tag);

        try {
            if (ndef != null) {
                ndef.connect();
                if (!ndef.isWritable()) {
                    Toast.makeText(this, "La tarjeta NFC no es escribible", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Overwrite data
                ndef.writeNdefMessage(message);
                Toast.makeText(this, "Datos escritos correctamente en NFC", Toast.LENGTH_SHORT).show();

                ndef.close();
            } else {
                // If the card is not NDEF, try formatting it
                NdefFormatable formatable = NdefFormatable.get(tag);
                if (formatable != null) {
                    formatable.connect();
                    formatable.format(message);
                    Toast.makeText(this, "Tarjeta formateada y datos escritos", Toast.LENGTH_SHORT).show();
                    formatable.close();
                } else {
                    Toast.makeText(this, "Tarjeta NFC no compatible", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error al escribir en NFC: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }


    private void readFromNfc(Intent intent) {
        Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (rawMessages != null && rawMessages.length > 0) {
            NdefMessage ndefMessage = (NdefMessage) rawMessages[0];
            NdefRecord[] records = ndefMessage.getRecords();
            if (records.length > 0) {
                NdefRecord record = records[0];
                byte[] payload = record.getPayload();

                // Solution: Skip the first byte (language size)
                int languageCodeLength = payload[0] & 0x3F; // Extraer el tamaño del idioma (primeros 6 bits)
                String textRead = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, Charset.forName("UTF-8"));

                // Show read content
                editTextRead.setText(textRead);
                Toast.makeText(this, "Leído de NFC: " + textRead, Toast.LENGTH_SHORT).show();
            }
        }
    }



    private NdefMessage createNdefMessage(String text) {
        NdefRecord ndefRecord = NdefRecord.createTextRecord("en", text);
        return new NdefMessage(new NdefRecord[]{ndefRecord});
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFiltersArray, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }
}
