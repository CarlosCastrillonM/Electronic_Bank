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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.nio.charset.Charset;

public class MainActivity extends AppCompatActivity {

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] intentFiltersArray;
    private EditText editBalanceSize, editBalanceRead;
    private EditText NumbersCard, ownerCardName;
    private int balance;
    private Button btnSave;
    private RadioGroup rGroupBtn;
    private RadioButton rBtnAddBalance;
    private RadioButton rBtnReduceBalance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NumbersCard = findViewById(R.id.numbersCreditCard);
        ownerCardName = findViewById(R.id.nameUserCard);

        editBalanceSize = findViewById(R.id.editCardBalance);
        editBalanceRead = findViewById(R.id.balanceCard);
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
            String textToWrite = editBalanceSize.getText().toString();
            String balanceRead = editBalanceRead.getText().toString();

            balance = calcBalance(textToWrite, balanceRead);


            if (!textToWrite.isEmpty()) {
                Toast.makeText(this, "Acerca la tarjeta NFC para escribir...", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "El campo no puede estar vacío", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private int calcBalance(String textBalance, String balanceRead) {

        rGroupBtn = findViewById(R.id.rGroupButton);
        rBtnAddBalance = findViewById(R.id.rButtonAddBalance);
        rBtnReduceBalance = findViewById(R.id.rButtonReduceBalance);

        int balance = Integer.parseInt(textBalance);
        int totalBalance = Integer.parseInt(balanceRead);

        if (rBtnAddBalance.isChecked()) {
            this.balance = totalBalance + balance;

        } if (rBtnReduceBalance.isChecked()) {
            this.balance = totalBalance - balance;

        } else if (!rBtnAddBalance.isChecked() && !rBtnReduceBalance.isChecked()){
            Toast.makeText(this, "Debe seleccionar una opcion", Toast.LENGTH_LONG).show();
        }

        return this.balance;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String textToWrite = editBalanceSize.getText().toString();
            String newBalance = String.valueOf(this.balance);

            if (!textToWrite.isEmpty()) {
                writeNfcTag(detectedTag, newBalance);
            }

            // If `readFromNfc()` uses Intent, pass the Intent instead of the Tag
            readFromNfc(intent);
        }
    }

    private void writeNfcTag(Tag tag, String newBalance) {
        Ndef ndef = Ndef.get(tag);

        try {
            if (ndef != null) {
                ndef.connect();
                if (!ndef.isWritable()) {
                    Toast.makeText(this, "La tarjeta NFC no es escribible", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Leer los registros actuales desde la tarjeta
                NdefMessage existingMessage = ndef.getNdefMessage();
                String record2 = "";
                String record3 = "";

                if (existingMessage != null) {
                    NdefRecord[] records = existingMessage.getRecords();

                    if (records.length > 1) {
                        byte[] payload2 = records[1].getPayload();
                        int langCodeLen2 = payload2[0] & 0x3F;
                        record2 = new String(payload2, langCodeLen2 + 1, payload2.length - langCodeLen2 - 1, Charset.forName("UTF-8"));
                    }

                    if (records.length > 2) {
                        byte[] payload3 = records[2].getPayload();
                        int langCodeLen3 = payload3[0] & 0x3F;
                        record3 = new String(payload3, langCodeLen3 + 1, payload3.length - langCodeLen3 - 1, Charset.forName("UTF-8"));
                    }
                }

                // Crear registros manteniendo los datos actuales y actualizando solo el saldo
                NdefRecord recordBalance = NdefRecord.createTextRecord("en", newBalance);
                NdefRecord recordNumbersCard = NdefRecord.createTextRecord("en", record2);
                NdefRecord recordOwnerCard = NdefRecord.createTextRecord("en", record3);

                NdefMessage newMessage = new NdefMessage(new NdefRecord[]{recordBalance, recordNumbersCard, recordOwnerCard});

                // Escribir los registros en la tarjeta
                ndef.writeNdefMessage(newMessage);
                Toast.makeText(this, "Saldo actualizado correctamente", Toast.LENGTH_SHORT).show();

                ndef.close();
            } else {
                NdefFormatable formatable = NdefFormatable.get(tag);
                if (formatable != null) {
                    formatable.connect();
                    formatable.format(new NdefMessage(new NdefRecord[]{NdefRecord.createTextRecord("en", newBalance)}));
                    Toast.makeText(this, "Tarjeta formateada y saldo guardado", Toast.LENGTH_SHORT).show();
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

            // Variables para almacenar cada registro
            String record1 = "";
            String record2 = "";
            String record3 = "";

            for (int i = 0; i < records.length; i++) {
                byte[] payload = records[i].getPayload();
                int languageCodeLength = payload[0] & 0x3F;
                String textRead = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, Charset.forName("UTF-8"));

                // Asignar cada registro a una variable según su posición
                if (i == 0) {
                    record1 = textRead;
                } else if (i == 1) {
                    record2 = textRead;
                } else if (i == 2) {
                    record3 = textRead;
                }
            }

            // Mostrar los registros en los campos de texto
            editBalanceRead.setText(record1);
            NumbersCard.setText(record2);
            ownerCardName.setText(record3);

            Toast.makeText(this, "Registro 1: " + record1 + "\nRegistro 2: " + record2 + "\nRegistro 3: " + record3, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No se encontraron registros en la tarjeta NFC", Toast.LENGTH_SHORT).show();
        }
    }


    private void readSpecificRecord(Intent intent, int index) {
        Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (rawMessages != null && rawMessages.length > 0) {
            NdefMessage ndefMessage = (NdefMessage) rawMessages[0];
            NdefRecord[] records = ndefMessage.getRecords();

            // Verificar que el índice solicitado existe
            if (index < 0 || index >= records.length) {
                Toast.makeText(this, "Registro no encontrado", Toast.LENGTH_SHORT).show();
                return;
            }

            // Obtener el registro en la posición específica
            NdefRecord record = records[index];
            byte[] payload = record.getPayload();

            // Ignorar el código de idioma y extraer el texto
            int languageCodeLength = payload[0] & 0x3F;
            String textRead = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, Charset.forName("UTF-8"));

            // Mostrar el texto leído
            editBalanceRead.setText(textRead);
            Toast.makeText(this, "Registro leído: " + textRead, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "No se encontraron registros en la tarjeta NFC", Toast.LENGTH_SHORT).show();
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
