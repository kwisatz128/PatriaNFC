package com.example.patrianfc;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.os.Build;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;
import java.util.List;

import com.example.patrianfc.parser.NdefMessageParser;
import com.example.patrianfc.record.ParsedNdefRecord;

public class MainActivity extends AppCompatActivity {

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private TextView text;

    private String providerId = LocationManager.GPS_PROVIDER;

    private LocationManager locationManager;
    //Dichiariamo un LocationListener usato per rimanere in ascolto di cambi di posizione
    private LocationListener locationListener;
    private static final int MIN_DIST = 20;
    private static final int MIN_PERIOD = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        text = (TextView) findViewById(R.id.text);
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        //Istanziamo il LocationManager recuperando il servizio di localizzazione
        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        //Istanziamo il listener che monitorerà cambi nella posizione
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateGUI(location);

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                /*
                 * Metodo invocato quando cambia lo stato del provider, ad esempio quando questo non
                 * è in grado di recuperare la posizione, oppure quando è ritornato disponibile dopo
                 * un periodo di non disponibilità
                 */
            }

            @Override
            public void onProviderEnabled(String provider) {
                /*
                 * Metodo invocato quando il provider viene abilitato dall'utente
                 */
            }

            @Override
            public void onProviderDisabled(String provider) {
                /*
                 * Metodo invocato quando l'utente disabilita il provider o quando è già disabilitato
                 * al momento della chiamata del metodo "requestLocationUpdates()"
                 */
            }
        };
        /*
         * Se stiamo esegendo l'app su una versione di Android uguale o superiore alla 6.0
         * dobbiamo richiedere necessariamente il permesso di accedere alla localizzazione a
         * runtime prima di poter avere accesso a questa, altrimenti registriamo direttamente il listener
         */
        if (Build.VERSION.SDK_INT >= 23) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                //Richiediamo il permesso all'utente
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                return;
            } else {
                //Registriamo il listener per ricevere aggiornamenti sulla posizione
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            }
        } else {
            //Registriamo il listener per ricevere aggiornamenti sulla posizione
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
        }

        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location!=location){
            updateGUI(location);
            Toast.makeText(this, "Lettura della posizione in corso", Toast.LENGTH_SHORT).show();
        }

        if (nfcAdapter == null) {
            Toast.makeText(this, "Devi abilitare il modulo NFC", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        pendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, this.getClass())
                        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);


    }

    void showWirelessSettings() {

        Toast.makeText(this, "Devi abilitare l'NFC", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(Settings.ACTION_NFC_SETTINGS);
        startActivity(intent);
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (nfcAdapter != null) {
            if (!nfcAdapter.isEnabled())
                showWirelessSettings();

            nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        locationManager.requestLocationUpdates(providerId, MIN_PERIOD,MIN_DIST, locationListener);
        Toast.makeText(this, "Lettura della posizione in corso", Toast.LENGTH_SHORT).show();


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        /*
         * Metodo invocato alla richiesta del permesso a runtime
         */
        //Se il permesso specifico è stato concesso, ci registriamo per la posizione
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Registriamo il listener per ricevere aggiornamenti sulla posizione
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            }

        }
    }

    @Override
    protected void onPause(){
        super.onPause();

        if (nfcAdapter != null){
            nfcAdapter.disableForegroundDispatch(this);
        }

        if (locationManager!=null && locationManager.isProviderEnabled(providerId))
            locationManager.removeUpdates(locationListener);
    }

    @Override
    protected void onNewIntent(Intent intent){
        setIntent(intent);
        resolveIntent(intent);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.clear();
    }

    private void resolveIntent(Intent intent){
        String action = intent.getAction();

        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
            || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
            || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs;

            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];

                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            } else {
                byte[] empty = new byte[0];
                byte[] id = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);
                Tag tag = (Tag) intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                byte[] payload = dumpTagData(tag).getBytes();
                NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN, empty, id, payload);
                NdefMessage msg = new NdefMessage(new NdefRecord[] {record});
                msgs = new NdefMessage[] {msg};
            }

            displayMsgs(msgs);

        }

    }

    private void displayMsgs(NdefMessage[] msgs){
        if (msgs == null || msgs.length == 0)
            return;

        StringBuilder builder = new StringBuilder();
        List<ParsedNdefRecord> records = NdefMessageParser.parse(msgs[0]);
        final int size = records.size();

        for (int i = 0; i < size; i++) {
            ParsedNdefRecord record = records.get(i);
            String str = record.str();
            builder.append(str).append("\n");
        }

        text.setText(builder.toString());
    }

    //servizio di localizzazione strada

    /*private class AddressSolver extends AsyncTask<Location, Void, String>
    {
        @Override
        protected String doInBackground(Location... params)
        {
            Location pos=params[0];
            double latitude = pos.getLatitude();
            double longitude = pos.getLongitude();
            List<Address> addresses = null;
            try
            {
                addresses = geo.getFromLocation(latitude, longitude, 1);
            }
            catch (IOException e)
            {
            }
            if (addresses!=null)
            {
                if (addresses.isEmpty())
                {
                    return null;
                }
                else {
                    if (addresses.size() > 0)
                    {
                        StringBuffer address=new StringBuffer();
                        Address tmp=addresses.get(0);
                        for (int y=0;y<tmp.getMaxAddressLineIndex();y++)
                            address.append(tmp.getAddressLine(y)+"\n");
                        return address.toString();
                    }
                }
            }
            return null;
        }
        @Override
        protected void onPostExecute(String result)
        {
            if (result!=null)
                updateText(R.id.where, result);
            else
                updateText(R.id.where, "N.A.");
        }
    }
    */
    private void updateGUI(Location location)
    {
        Date timestamp = new Date(location.getTime());
        updateText(R.id.timestamp, timestamp.toString());
        double latitude = location.getLatitude();
        updateText(R.id.latitude, String.valueOf(latitude));
        double longitude = location.getLongitude();
        updateText(R.id.longitude, String.valueOf(longitude));
        //new AddressSolver().execute(location);
    }

    private void updateText(int id, String text)
    {
        TextView textView = (TextView) findViewById(id);
        textView.setText(text);
    }


    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = bytes.length - 1; i >= 0; --i) {
            int b = bytes[i] & 0xff;
            if (b < 0x10)
                sb.append('0');
            sb.append(Integer.toHexString(b));
            if (i > 0) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    private String toReversedHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; ++i) {
            if (i > 0) {
                sb.append(" ");
            }
            int b = bytes[i] & 0xff;
            if (b < 0x10)
                sb.append('0');
            sb.append(Integer.toHexString(b));
        }
        return sb.toString();
    }

    private long toDec(byte[] bytes) {
        long result = 0;
        long factor = 1;
        for (int i = 0; i < bytes.length; ++i) {
            long value = bytes[i] & 0xffl;
            result += value * factor;
            factor *= 256l;
        }
        return result;
    }

    private long toReversedDec(byte[] bytes) {
        long result = 0;
        long factor = 1;
        for (int i = bytes.length - 1; i >= 0; --i) {
            long value = bytes[i] & 0xffl;
            result += value * factor;
            factor *= 256l;
        }
        return result;
    }

    private String dumpTagData(Tag tag) {
        StringBuilder sb = new StringBuilder();
        byte[] id = tag.getId();
        sb.append("ID (hex): ").append(toHex(id)).append('\n');
        sb.append("ID (reversed hex): ").append(toReversedHex(id)).append('\n');
        sb.append("ID (dec): ").append(toDec(id)).append('\n');
        sb.append("ID (reversed dec): ").append(toReversedDec(id)).append('\n');

        String prefix = "android.nfc.tech.";
        sb.append("Technologies: ");
        for (String tech : tag.getTechList()) {
            sb.append(tech.substring(prefix.length()));
            sb.append(", ");
        }

        sb.delete(sb.length() - 2, sb.length());

        for (String tech : tag.getTechList()) {
            if (tech.equals(MifareClassic.class.getName())) {
                sb.append('\n');
                String type = "Unknown";

                try {
                    MifareClassic mifareTag = MifareClassic.get(tag);

                    switch (mifareTag.getType()) {
                        case MifareClassic.TYPE_CLASSIC:
                            type = "Classic";
                            break;
                        case MifareClassic.TYPE_PLUS:
                            type = "Plus";
                            break;
                        case MifareClassic.TYPE_PRO:
                            type = "Pro";
                            break;
                    }
                    sb.append("Mifare Classic type: ");
                    sb.append(type);
                    sb.append('\n');

                    sb.append("Mifare size: ");
                    sb.append(mifareTag.getSize() + " bytes");
                    sb.append('\n');

                    sb.append("Mifare sectors: ");
                    sb.append(mifareTag.getSectorCount());
                    sb.append('\n');

                    sb.append("Mifare blocks: ");
                    sb.append(mifareTag.getBlockCount());
                } catch (Exception e) {
                    sb.append("Mifare classic error: " + e.getMessage());
                }
            }

            if (tech.equals(MifareUltralight.class.getName())) {
                sb.append('\n');
                MifareUltralight mifareUlTag = MifareUltralight.get(tag);
                String type = "Unknown";
                switch (mifareUlTag.getType()) {
                    case MifareUltralight.TYPE_ULTRALIGHT:
                        type = "Ultralight";
                        break;
                    case MifareUltralight.TYPE_ULTRALIGHT_C:
                        type = "Ultralight C";
                        break;
                }
                sb.append("Mifare Ultralight type: ");
                sb.append(type);
            }
        }

        return sb.toString();
    }
}
