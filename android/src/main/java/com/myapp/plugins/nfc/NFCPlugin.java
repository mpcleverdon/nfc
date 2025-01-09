package com.myapp.plugins.nfc;

import android.app.Activity;
import android.content.Intent;
import android.app.PendingIntent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcEvent;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Build;
import android.os.Parcelable;
import androidx.annotation.RequiresApi;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;

@CapacitorPlugin(name = "NFC", permissions = {
    @Permission(strings = { "android.permission.NFC" }, alias = "nfc")
})
public class NFCPlugin extends Plugin implements NfcAdapter.ReaderCallback, NfcAdapter.CreateNdefMessageCallback, NfcAdapter.OnNdefPushCompleteCallback {
    private NfcAdapter nfcAdapter;
    private static final String EVENT_TAG_SCANNED = "nfcTagScanned";
    private String pendingMessage;
    private PluginCall pendingShareCall;
    private boolean isReaderModeEnabled = false;

    @Override
    public void load() {
        Activity activity = getActivity();
        nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        enableReaderMode();
    }

    private void enableReaderMode() {
        if (nfcAdapter != null && !isReaderModeEnabled) {
            Activity activity = getActivity();
            nfcAdapter.enableReaderMode(activity, this,
                NfcAdapter.FLAG_READER_NFC_A |
                NfcAdapter.FLAG_READER_NFC_B |
                NfcAdapter.FLAG_READER_NFC_F |
                NfcAdapter.FLAG_READER_NFC_V |
                NfcAdapter.FLAG_READER_NFC_BARCODE,
                null);
            isReaderModeEnabled = true;
        }
    }

    private void disableReaderMode() {
        if (nfcAdapter != null && isReaderModeEnabled) {
            Activity activity = getActivity();
            nfcAdapter.disableReaderMode(activity);
            isReaderModeEnabled = false;
        }
    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        if (pendingMessage == null) {
            return null;
        }

        try {
            NdefRecord record = NdefRecord.createTextRecord(null, pendingMessage);
            return new NdefMessage(new NdefRecord[]{record});
        } catch (Exception e) {
            if (pendingShareCall != null) {
                pendingShareCall.reject("Failed to create NDEF message: " + e.getMessage());
            }
            return null;
        }
    }

    @PluginMethod
    public void addListener(PluginCall call) {
        String eventName = call.getString("eventName");
        if (EVENT_TAG_SCANNED.equals(eventName)) {
            call.resolve();
        }
    }

    @PluginMethod
    public void removeAllListeners(PluginCall call) {
        super.removeAllListeners(call);
        call.resolve();
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        Ndef ndef = Ndef.get(tag);
        if (ndef != null) {
            try {
                ndef.connect();
                NdefMessage ndefMessage = ndef.getNdefMessage();
                if (ndefMessage != null && ndefMessage.getRecords().length > 0) {
                    String message = new String(ndefMessage.getRecords()[0].getPayload());
                    onNfcTagScanned(message);
                }
                ndef.close();
            } catch (Exception e) {
                JSObject ret = new JSObject();
                ret.put("error", e.getMessage());
                notifyListeners("nfcError", ret);
            }
        }
    }

    @PluginMethod
    public void isEnabled(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("value", nfcAdapter != null && nfcAdapter.isEnabled());
        call.resolve(ret);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @PluginMethod
    public void share(final PluginCall call) {
        if (nfcAdapter == null || !nfcAdapter.isEnabled()) {
            call.reject("NFC is not available");
            return;
        }

        String message = call.getString("message");
        if (message == null) {
            call.reject("Message is required");
            return;
        }

        pendingMessage = message;
        pendingShareCall = call;
        
        getActivity().runOnUiThread(() -> {
            try {
                // Temporarily disable reader mode while sharing
                disableReaderMode();
                
                Activity activity = getActivity();
                NdefRecord record = NdefRecord.createTextRecord(null, message);
                final NdefMessage ndefMessage = new NdefMessage(new NdefRecord[]{record});
                
                // Setup foreground dispatch for sharing
                Intent intent = new Intent(activity, activity.getClass());
                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                
                int flags = PendingIntent.FLAG_UPDATE_CURRENT;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    flags |= PendingIntent.FLAG_MUTABLE;
                }
                
                PendingIntent pendingIntent = PendingIntent.getActivity(activity, 0, intent, flags);
                IntentFilter[] filters = new IntentFilter[] { 
                    new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED) 
                };
                
                nfcAdapter.enableForegroundDispatch(activity, pendingIntent, filters, null);
                
                JSObject ret = new JSObject();
                ret.put("shared", true);
                call.resolve(ret);
            } catch (Exception e) {
                enableReaderMode(); // Re-enable reader mode if sharing fails
                call.reject("Failed to share via NFC: " + e.getMessage());
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @PluginMethod
    public void stopSharing(PluginCall call) {
        if (nfcAdapter != null) {
            getActivity().runOnUiThread(() -> {
                try {
                    Activity activity = getActivity();
                    nfcAdapter.disableForegroundDispatch(activity);
                    pendingMessage = null;
                    pendingShareCall = null;
                    enableReaderMode(); // Re-enable reader mode after sharing
                    call.resolve();
                } catch (Exception e) {
                    call.reject("Failed to stop sharing: " + e.getMessage());
                }
            });
        } else {
            call.resolve();
        }
    }

    private void onNfcTagScanned(String message) {
        JSObject ret = new JSObject();
        ret.put("message", message);
        notifyListeners(EVENT_TAG_SCANNED, ret, true);
    }

    @Override
    public void handleOnDestroy() {
        if (nfcAdapter != null) {
            Activity activity = getActivity();
            if (activity != null) {
                nfcAdapter.disableReaderMode(activity);
                isReaderModeEnabled = false;
            }
        }
    }

    @Override
    protected void handleOnNewIntent(Intent intent) {
        super.handleOnNewIntent(intent);
        
        // Handle incoming NFC communication
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null && rawMsgs.length > 0) {
                NdefMessage msg = (NdefMessage) rawMsgs[0];
                if (msg.getRecords().length > 0) {
                    String message = new String(msg.getRecords()[0].getPayload());
                    onNfcTagScanned(message);
                }
            }
        }
    }

    @Override
    public void onNdefPushComplete(NfcEvent event) {
        // Called when the message has been sent
        if (pendingShareCall != null) {
            JSObject ret = new JSObject();
            ret.put("completed", true);
            notifyListeners("nfcPushComplete", ret);
        }
    }
} 