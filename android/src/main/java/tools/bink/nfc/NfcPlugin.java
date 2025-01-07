package tools.bink.nfc;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.content.Context;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.getcapacitor.JSObject;
import com.getcapacitor.JSArray;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;

import android.nfc.tech.*;
import java.util.ArrayList;
import java.util.List;

@CapacitorPlugin(
    name = "Nfc",
    permissions = {
        @Permission(
            alias = "nfc",
            strings = { "android.permission.NFC" }
        )
    }
)
public class NfcPlugin extends Plugin {
    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private boolean isScanning = false;
    private static final String TAG = "NfcPlugin";
    private String savedText;
    private String savedCallId;
    private WriteParameters writeParams;

    private static class WriteParameters {
        String text;
        String cardType;
        String aid;
        boolean secure;
        int timeout;

        WriteParameters(String text, String cardType, String aid, boolean secure, int timeout) {
            this.text = text;
            this.cardType = cardType;
            this.aid = aid;
            this.secure = secure;
            this.timeout = timeout;
        }
    }

    @Override
    public void load() {
        super.load();
        try {
            NfcManager nfcManager = (NfcManager) getContext().getSystemService(Context.NFC_SERVICE);
            nfcAdapter = nfcManager.getDefaultAdapter();
            
            // Create a generic PendingIntent that will be delivered to this activity
            Intent intent = new Intent(getContext(), getActivity().getClass());
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            
            int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE :
                PendingIntent.FLAG_UPDATE_CURRENT;
                
            pendingIntent = PendingIntent.getActivity(getContext(), 0, intent, flags);
        } catch (Exception e) {
            notifyListeners("nfcError", new JSObject().put("error", "Failed to initialize NFC: " + e.getMessage()));
        }
    }

    @PluginMethod
    public void isEnabled(PluginCall call) {
        try {
            JSObject ret = new JSObject();
            if (nfcAdapter == null) {
                ret.put("enabled", false);
                ret.put("message", "NFC is not available on this device");
            } else {
                ret.put("enabled", nfcAdapter.isEnabled());
                ret.put("message", nfcAdapter.isEnabled() ? "NFC is enabled" : "NFC is disabled");
            }
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("Error checking NFC status: " + e.getMessage());
        }
    }

    @PluginMethod
    public void startScanning(PluginCall call) {
        if (!hasRequiredPermissions()) {
            requestPermissions(call);
            return;
        }

        if (nfcAdapter == null) {
            call.reject("NFC is not available on this device");
            return;
        }

        if (!nfcAdapter.isEnabled()) {
            call.reject("NFC is disabled. Please enable NFC in your device settings.");
            return;
        }

        Activity activity = getActivity();
        if (activity == null) {
            call.reject("Activity not available");
            return;
        }

        try {
            if (isScanning) {
                call.reject("NFC scanning is already in progress");
                return;
            }

            // Set up NDEF discovery
            IntentFilter[] intentFiltersArray = new IntentFilter[] {
                new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED),
                new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
                new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
            };

            String[][] techListsArray = new String[][] {
                new String[] { Ndef.class.getName() }
            };

            nfcAdapter.enableForegroundDispatch(activity, pendingIntent, intentFiltersArray, techListsArray);
            isScanning = true;
            call.resolve();
        } catch (Exception e) {
            call.reject("Failed to start NFC scanning: " + e.getMessage());
        }
    }

    @PluginMethod
    public void stopScanning(PluginCall call) {
        if (nfcAdapter == null) {
            call.reject("NFC is not available on this device");
            return;
        }

        Activity activity = getActivity();
        if (activity == null) {
            call.reject("Activity not available");
            return;
        }

        try {
            if (!isScanning) {
                call.reject("NFC scanning is not active");
                return;
            }

            nfcAdapter.disableForegroundDispatch(activity);
            isScanning = false;
            call.resolve();
        } catch (Exception e) {
            call.reject("Failed to stop NFC scanning: " + e.getMessage());
        }
    }

    @PluginMethod
    public void write(PluginCall call) {
        if (!hasRequiredPermissions()) {
            requestPermissions(call);
            return;
        }

        String text = call.getString("text");
        String mode = call.getString("mode", "reader"); // "reader" or "emulator"

        if ("emulator".equals(mode)) {
            // Set up as card emulator
            NfcHostCardEmulatorService.setMessageToShare(text);
            JSObject result = new JSObject();
            result.put("success", true);
            result.put("message", "Device ready to share data via NFC");
            call.resolve(result);
        } else {
            // Normal reader mode
            String cardType = call.getString("cardType", "auto");
            String aid = call.getString("aid", "F0010203040506");
            boolean secure = call.getBoolean("secure", false);
            int timeout = call.getInt("timeout", 5000);

            this.writeParams = new WriteParameters(text, cardType, aid, secure, timeout);
            savedCallId = call.getCallbackId();
            
            try {
                Activity activity = getActivity();
                IntentFilter[] writeTagFilters = new IntentFilter[] {
                    new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED)
                };
                nfcAdapter.enableForegroundDispatch(activity, pendingIntent, writeTagFilters, null);
                notifyListeners("nfcStatus", new JSObject().put("status", "Ready to write. Please touch another NFC device."));
            } catch (Exception e) {
                call.reject("Failed to start NFC write mode: " + e.getMessage());
            }
        }
    }

    @Override
    protected void handleOnNewIntent(Intent intent) {
        super.handleOnNewIntent(intent);
        
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action) ||
            NfcAdapter.ACTION_TECH_DISCOVERED.equals(action) ||
            NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                if (writeParams != null) {
                    writeNdefMessage(tag, writeParams);
                    writeParams = null;
                } else if (savedCallId != null) {
                    // This is a read operation
                    try {
                        JSObject result = readTag(tag);
                        PluginCall savedCall = bridge.getSavedCall(savedCallId);
                        if (savedCall != null) {
                            savedCall.resolve(result);
                            bridge.releaseCall(savedCallId);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading tag", e);
                        PluginCall savedCall = bridge.getSavedCall(savedCallId);
                        if (savedCall != null) {
                            savedCall.reject("Error reading tag: " + e.getMessage());
                            bridge.releaseCall(savedCallId);
                        }
                    }
                }
                savedCallId = null;
            }
        }
    }

    @Override
    protected void handleOnDestroy() {
        super.handleOnDestroy();
        // Clean up any saved calls
        if (savedCallId != null) {
            bridge.releaseCall(savedCallId);
            savedCallId = null;
        }
    }

    private String bytesToHexString(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void writeNdefMessage(Tag tag, WriteParameters params) {
        try {
            String[] techList = tag.getTechList();
            if (Arrays.asList(techList).contains("android.nfc.tech.IsoDep")) {
                // For ISO-DEP tags, use the stored AID if available
                String aid = params.aid != null ? params.aid : "F0010203040506"; // Default AID
                writeToIsoDep(tag, params.text, aid, params.timeout);
            } else {
                // Try NDEF first
                Ndef ndef = Ndef.get(tag);
                if (ndef != null) {
                    writeToNdefTag(ndef, params.text);
                } else {
                    // Try to format if not NDEF
                    NdefFormatable formatable = NdefFormatable.get(tag);
                    if (formatable != null) {
                        writeToFormattableTag(formatable, params.text);
                    } else {
                        throw new Exception("Tag doesn't support any known write methods");
                    }
                }
            }
        } catch (Exception e) {
            JSObject error = new JSObject();
            error.put("error", e.getMessage());
            error.put("stackTrace", Log.getStackTraceString(e));
            notifyListeners("writeError", error);
            Log.e(TAG, "Error writing to NFC tag", e);
        }
    }

    private void writeToNdefTag(Ndef ndef, String text) throws Exception {
        try {
            ndef.connect();
            
            // Create the message
            NdefRecord textRecord = createTextRecord(text);
            if (textRecord == null) {
                throw new Exception("Failed to create NDEF record");
            }
            
            NdefMessage message = new NdefMessage(new NdefRecord[] { textRecord });

            // Check if tag is writable and has enough space
            if (!ndef.isWritable()) {
                throw new Exception("Tag is read-only");
            }

            int size = message.getByteArrayLength();
            int maxSize = ndef.getMaxSize();
            if (maxSize < size) {
                throw new Exception("Tag has insufficient space. Required: " + size + " bytes, Available: " + maxSize + " bytes");
            }

            // Write the message
            ndef.writeNdefMessage(message);
            
            // Notify success
            JSObject result = new JSObject();
            result.put("written", true);
            result.put("type", "NDEF");
            result.put("message", "Data written successfully to NDEF tag");
            result.put("size", size);
            result.put("maxSize", maxSize);
            notifyListeners("writeSuccess", result);
        } finally {
            try {
                ndef.close();
            } catch (Exception e) {
                Log.w(TAG, "Error closing NDEF connection", e);
            }
        }
    }

    private void writeToFormattableTag(NdefFormatable formatable, String text) throws Exception {
        try {
            formatable.connect();
            
            // Create the message
            NdefRecord textRecord = createTextRecord(text);
            if (textRecord == null) {
                throw new Exception("Failed to create NDEF record");
            }
            
            NdefMessage message = new NdefMessage(new NdefRecord[] { textRecord });

            // Format and write
            formatable.format(message);
            
            // Notify success
            JSObject result = new JSObject();
            result.put("written", true);
            result.put("type", "FORMATTED");
            result.put("message", "Tag formatted and data written successfully");
            notifyListeners("writeSuccess", result);
        } finally {
            try {
                formatable.close();
            } catch (Exception e) {
                Log.w(TAG, "Error closing NdefFormatable connection", e);
            }
        }
    }

    private NdefRecord createTextRecord(String text) {
        try {
            if (text == null || text.isEmpty()) {
                throw new Exception("Text cannot be empty");
            }
            
            byte[] langBytes = "en".getBytes(StandardCharsets.US_ASCII);
            byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
            
            int langLength = langBytes.length;
            int textLength = textBytes.length;
            
            if (1 + langLength + textLength > 0xFF) {
                throw new Exception("Text is too long");
            }

            byte[] payload = new byte[1 + langLength + textLength];
            payload[0] = (byte) langLength;

            System.arraycopy(langBytes, 0, payload, 1, langLength);
            System.arraycopy(textBytes, 0, payload, 1 + langLength, textLength);

            return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, 
                                NdefRecord.RTD_TEXT, 
                                new byte[0], 
                                payload);
        } catch (Exception e) {
            Log.e(TAG, "Error creating NDEF record", e);
            return null;
        }
    }

    private void handleNdefMessage(NdefMessage ndefMessage, JSObject parentObject) {
        try {
            JSArray records = new JSArray();

            if (ndefMessage != null && ndefMessage.getRecords() != null) {
                for (NdefRecord ndefRecord : ndefMessage.getRecords()) {
                    if (ndefRecord != null) {
                        JSObject record = new JSObject();
                        
                        // Get record type with null check
                        byte[] type = ndefRecord.getType();
                        String typeString = type != null ? new String(type, StandardCharsets.UTF_8) : "";
                        record.put("type", typeString);
                        
                        // Get record payload with null check
                        byte[] payload = ndefRecord.getPayload();
                        if (payload != null && payload.length > 0) {
                            String payloadString;
                            if (type != null && Arrays.equals(type, NdefRecord.RTD_TEXT)) {
                                int languageCodeLength = payload[0] & 0x3F;
                                if (languageCodeLength < payload.length) {
                                    payloadString = new String(payload, languageCodeLength + 1, 
                                        payload.length - languageCodeLength - 1, StandardCharsets.UTF_8);
                                } else {
                                    payloadString = "";
                                }
                            } else {
                                payloadString = new String(payload, StandardCharsets.UTF_8);
                            }
                            record.put("payload", payloadString);
                        } else {
                            record.put("payload", "");
                        }
                        
                        // Get record ID with null check
                        byte[] id = ndefRecord.getId();
                        record.put("identifier", (id != null && id.length > 0) ? bytesToHexString(id) : "");
                        
                        records.put(record);
                    }
                }
            }
            
            if (parentObject != null) {
                parentObject.put("records", records);
            } else {
                JSObject tagData = new JSObject();
                tagData.put("records", records);
                notifyListeners("nfcTagDetected", tagData);
            }
        } catch (Exception e) {
            JSObject error = new JSObject();
            error.put("error", "Failed to parse NDEF message: " + e.getMessage());
            error.put("stackTrace", Log.getStackTraceString(e));
            notifyListeners("nfcError", error);
            Log.e(TAG, "Error handling NDEF message", e);
        }
    }

    private void handleTag(Tag tag) {
        try {
            JSObject tagInfo = new JSObject();
            
            // Safely handle tag ID
            byte[] tagId = tag != null ? tag.getId() : null;
            if (tagId != null && tagId.length > 0) {
                tagInfo.put("id", bytesToHexString(tagId));
            } else {
                tagInfo.put("id", "");
            }
            
            // Get all available tech types
            List<String> techList = new ArrayList<>();
            if (tag != null && tag.getTechList() != null) {
                for (String tech : tag.getTechList()) {
                    if (tech != null) {
                        techList.add(tech.replace("android.nfc.tech.", ""));
                    }
                }
            }
            tagInfo.put("techTypes", new JSArray(techList));

            // Handle different tag technologies with null checks
            if (techList.contains("Ndef")) {
                handleNdefTag(tag, tagInfo);
            } else if (techList.contains("NdefFormatable")) {
                handleFormattableTag(tag, tagInfo);
            } else if (techList.contains("MifareClassic")) {
                handleMifareClassic(tag, tagInfo);
            } else if (techList.contains("MifareUltralight")) {
                handleMifareUltralight(tag, tagInfo);
            } else if (techList.contains("IsoDep")) {
                handleIsoDep(tag, tagInfo);
            }

            notifyListeners("nfcTagDetected", tagInfo);
        } catch (Exception e) {
            JSObject error = new JSObject();
            error.put("error", "Failed to handle tag: " + e.getMessage());
            error.put("stackTrace", Log.getStackTraceString(e));
            notifyListeners("nfcError", error);
            Log.e(TAG, "Error handling NFC tag", e);
        }
    }

    private void handleNdefTag(Tag tag, JSObject tagInfo) throws Exception {
        Ndef ndef = Ndef.get(tag);
        if (ndef != null) {
            ndef.connect();
            try {
                tagInfo.put("type", ndef.getType());
                tagInfo.put("maxSize", ndef.getMaxSize());
                tagInfo.put("isWritable", ndef.isWritable());
                tagInfo.put("canMakeReadOnly", ndef.canMakeReadOnly());
                
                NdefMessage ndefMessage = ndef.getNdefMessage();
                if (ndefMessage != null) {
                    handleNdefMessage(ndefMessage, tagInfo);
                }
            } finally {
                ndef.close();
            }
        }
    }

    private void handleFormattableTag(Tag tag, JSObject tagInfo) {
        NdefFormatable formatable = NdefFormatable.get(tag);
        tagInfo.put("type", "FORMATABLE");
        tagInfo.put("isFormatted", false);
    }

    private void handleMifareClassic(Tag tag, JSObject tagInfo) throws Exception {
        MifareClassic mifare = MifareClassic.get(tag);
        if (mifare != null) {
            mifare.connect();
            try {
                // Read specific sectors
                byte[] data = new byte[16];
                boolean auth = mifare.authenticateSectorWithKeyA(0, MifareClassic.KEY_DEFAULT);
                if (auth) {
                    // Read block 0 of sector 0
                    data = mifare.readBlock(0);
                    tagInfo.put("block0", bytesToHexString(data));
                }
                
                tagInfo.put("type", "MIFARE_CLASSIC");
                tagInfo.put("size", mifare.getSize());
                tagInfo.put("sectorCount", mifare.getSectorCount());
                tagInfo.put("blockCount", mifare.getBlockCount());
                tagInfo.put("maxTransceiveLength", mifare.getMaxTransceiveLength());
            } finally {
                mifare.close();
            }
        }
    }

    private void handleMifareUltralight(Tag tag, JSObject tagInfo) throws Exception {
        MifareUltralight ultralight = MifareUltralight.get(tag);
        if (ultralight != null) {
            ultralight.connect();
            try {
                tagInfo.put("type", "MIFARE_ULTRALIGHT");
                tagInfo.put("type", ultralight.getType());
                tagInfo.put("maxTransceiveLength", ultralight.getMaxTransceiveLength());
            } finally {
                ultralight.close();
            }
        }
    }

    private void handleIsoDep(Tag tag, JSObject tagInfo) throws Exception {
        IsoDep isoDep = IsoDep.get(tag);
        if (isoDep != null) {
            isoDep.connect();
            try {
                // Example: Read card number (for some banking cards)
                byte[] command = new byte[] {
                    (byte)0x00, // CLA
                    (byte)0xB2, // INS
                    (byte)0x01, // P1
                    (byte)0x0C, // P2
                    (byte)0x00  // Le
                };
                
                byte[] response = isoDep.transceive(command);
                if (response != null && response.length > 2) {
                    tagInfo.put("cardData", bytesToHexString(response));
                }
                
                tagInfo.put("type", "ISO_DEP");
                tagInfo.put("hiLayerResponse", bytesToHexString(isoDep.getHiLayerResponse()));
                tagInfo.put("historicalBytes", bytesToHexString(isoDep.getHistoricalBytes()));
            } finally {
                isoDep.close();
            }
        }
    }

    private void writeToIsoDep(Tag tag, String text, String aid, int timeout) throws Exception {
        IsoDep isoDep = IsoDep.get(tag);
        if (isoDep == null) {
            throw new Exception("Failed to create IsoDep instance");
        }

        try {
            isoDep.connect();
            isoDep.setTimeout(timeout);

            // Convert AID string to bytes
            byte[] aidBytes = hexStringToByteArray(aid);
            
            // Select Application with provided AID
            byte[] selectAID = new byte[5 + aidBytes.length];
            selectAID[0] = (byte)0x00; // CLA
            selectAID[1] = (byte)0xA4; // INS (SELECT)
            selectAID[2] = (byte)0x04; // P1
            selectAID[3] = (byte)0x00; // P2
            selectAID[4] = (byte)aidBytes.length; // Lc
            System.arraycopy(aidBytes, 0, selectAID, 5, aidBytes.length);
            
            Log.d(TAG, "Sending SELECT AID command: " + bytesToHexString(selectAID));
            byte[] selectResponse = isoDep.transceive(selectAID);
            Log.d(TAG, "SELECT AID response: " + bytesToHexString(selectResponse));
            if (!isSuccessful(selectResponse)) {
                throw new Exception("Failed to select application with AID: " + aid);
            }

            if (text != null && !text.isEmpty()) {
                // Write data command
                byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
                byte[] writeCommand = new byte[5 + textBytes.length];
                writeCommand[0] = (byte)0x00; // CLA
                writeCommand[1] = (byte)0xD0; // INS (WRITE)
                writeCommand[2] = (byte)0x00; // P1
                writeCommand[3] = (byte)0x00; // P2
                writeCommand[4] = (byte)textBytes.length; // Lc
                System.arraycopy(textBytes, 0, writeCommand, 5, textBytes.length);

                Log.d(TAG, "Sending WRITE command: " + bytesToHexString(writeCommand));
                byte[] writeResponse = isoDep.transceive(writeCommand);
                Log.d(TAG, "WRITE response: " + bytesToHexString(writeResponse));
                if (!isSuccessful(writeResponse)) {
                    throw new Exception("Write command failed. Response: " + bytesToHexString(writeResponse));
                }

                // Try to read back the data to verify
                byte[] readCommand = new byte[] {
                    (byte)0x00, // CLA
                    (byte)0xD0, // INS (READ)
                    (byte)0x01, // P1 (read mode)
                    (byte)0x00  // P2
                };
                
                byte[] readResponse = isoDep.transceive(readCommand);
                String readData = new String(readResponse, 0, readResponse.length - 2, StandardCharsets.UTF_8);
                
                JSObject result = new JSObject();
                result.put("written", true);
                result.put("type", "ISO_DEP");
                result.put("message", "Data written successfully to ISO-DEP tag");
                result.put("bytesWritten", textBytes.length);
                result.put("aid", aid);
                result.put("verifiedData", readData);
                notifyListeners("writeSuccess", result);
            } else {
                // Just read mode
                byte[] readCommand = new byte[] {
                    (byte)0x00, // CLA
                    (byte)0xD0, // INS (READ)
                    (byte)0x01, // P1 (read mode)
                    (byte)0x00  // P2
                };
                
                byte[] readResponse = isoDep.transceive(readCommand);
                String readData = new String(readResponse, 0, readResponse.length - 2, StandardCharsets.UTF_8);
                
                JSObject result = new JSObject();
                result.put("type", "ISO_DEP");
                result.put("message", "Data read successfully from ISO-DEP tag");
                result.put("data", readData);
                result.put("aid", aid);
                notifyListeners("readSuccess", result);
            }

        } finally {
            try {
                isoDep.close();
            } catch (Exception e) {
                Log.w(TAG, "Error closing IsoDep connection", e);
            }
        }
    }

    private boolean isSuccessful(byte[] response) {
        if (response == null || response.length < 2) {
            return false;
        }
        // Check for success response (0x90 0x00 is typical success response)
        return response[response.length - 2] == (byte)0x90 && 
               response[response.length - 1] == (byte)0x00;
    }

    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                             + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    @PluginMethod
    public void read(PluginCall call) {
        if (!hasRequiredPermissions()) {
            requestPermissions(call);
            return;
        }

        if (nfcAdapter == null) {
            call.reject("NFC is not available on this device");
            return;
        }

        if (!nfcAdapter.isEnabled()) {
            call.reject("NFC is disabled");
            return;
        }

        try {
            Activity activity = getActivity();
            IntentFilter[] readTagFilters = new IntentFilter[] {
                new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED),
                new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
                new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED)
            };

            // Store the call for later use
            savedCallId = call.getCallbackId();
            bridge.saveCall(call);

            // Enable foreground dispatch
            nfcAdapter.enableForegroundDispatch(activity, pendingIntent, readTagFilters, null);
            notifyListeners("nfcStatus", new JSObject().put("status", "Ready to read. Please touch an NFC tag."));
        } catch (Exception e) {
            call.reject("Failed to start NFC reading: " + e.getMessage());
        }
    }

    private JSObject readTag(Tag tag) throws Exception {
        JSObject result = new JSObject();
        
        // Get basic tag info
        byte[] tagId = tag != null ? tag.getId() : null;
        if (tagId != null && tagId.length > 0) {
            result.put("id", bytesToHexString(tagId));
        }
        
        // Get tech types
        List<String> techList = new ArrayList<>();
        if (tag.getTechList() != null) {
            for (String tech : tag.getTechList()) {
                techList.add(tech.replace("android.nfc.tech.", ""));
            }
        }
        result.put("techTypes", new JSArray(techList));

        // Try to read NDEF data first
        Ndef ndef = Ndef.get(tag);
        if (ndef != null) {
            ndef.connect();
            try {
                result.put("type", "NDEF");
                result.put("maxSize", ndef.getMaxSize());
                result.put("isWritable", ndef.isWritable());
                
                NdefMessage ndefMessage = ndef.getNdefMessage();
                if (ndefMessage != null) {
                    handleNdefMessage(ndefMessage, result);
                }
            } finally {
                ndef.close();
            }
            return result;
        }

        // If not NDEF, try ISO-DEP
        IsoDep isoDep = IsoDep.get(tag);
        if (isoDep != null) {
            isoDep.connect();
            try {
                result.put("type", "ISO_DEP");
                result.put("hiLayerResponse", bytesToHexString(isoDep.getHiLayerResponse()));
                result.put("historicalBytes", bytesToHexString(isoDep.getHistoricalBytes()));
                
                // Try to read using standard READ command
                byte[] readCommand = new byte[] {
                    (byte)0x00, // CLA
                    (byte)0xD0, // INS (READ)
                    (byte)0x01, // P1 (read mode)
                    (byte)0x00  // P2
                };
                
                byte[] response = isoDep.transceive(readCommand);
                if (response != null && response.length > 2) {
                    String data = new String(response, 0, response.length - 2, StandardCharsets.UTF_8);
                    result.put("data", data);
                }
            } finally {
                isoDep.close();
            }
            return result;
        }

        // Add support for other tag types as needed
        throw new Exception("Unsupported tag type");
    }
}
