package com.example.plugin;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.util.Log;
import com.getcapacitor.BridgeActivity;
import android.nfc.Tag;
import java.util.Arrays;
import android.content.IntentFilter;

public class MainActivity extends BridgeActivity {
    private static final String TAG = "NfcPlugin";
    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "MainActivity created");
        
        // Register the plugin
        registerPlugin(tools.bink.nfc.NfcPlugin.class);

        // Initialize NFC adapter
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            Log.d(TAG, "NFC is not available on this device");
            return;
        }

        // Create a PendingIntent for NFC intents
        Intent intent = new Intent(this, getClass());
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            // Enable foreground dispatch with high priority
            IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
            IntentFilter[] writeTagFilters = new IntentFilter[] {tagDetected};
            nfcAdapter.enableForegroundDispatch(
                this, 
                getPendingIntent(), 
                writeTagFilters, 
                null
            );
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "New intent received with action: " + action);
        
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) ||
            NfcAdapter.ACTION_TECH_DISCOVERED.equals(action) ||
            NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                String[] techList = tag.getTechList();
                Log.d(TAG, "Tag technologies: " + Arrays.toString(techList));
                Log.d(TAG, "Tag ID: " + bytesToHex(tag.getId()));
            }
        }
        
        super.onNewIntent(intent);
        setIntent(intent);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private PendingIntent getPendingIntent() {
        Intent intent = new Intent(this, getClass());
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        return PendingIntent.getActivity(
            this, 
            0, 
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );
    }
}
