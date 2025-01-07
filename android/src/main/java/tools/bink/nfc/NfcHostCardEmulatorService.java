package tools.bink.nfc;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;
import java.nio.charset.StandardCharsets;

public class NfcHostCardEmulatorService extends HostApduService {
    private static final String TAG = "NfcHCEService";
    
    // ISO-7816 SELECT command header
    private static final String SELECT_APDU_HEADER = "00A40400";
    private static final String READ_COMMAND = "00D0";
    private static final String AID = "F0010203040506";
    private static final byte[] SELECT_OK = {(byte)0x90, (byte)0x00};
    private static final byte[] UNKNOWN_CMD = {(byte)0x6F, (byte)0x00};
    
    private static String emulationMode = "NDEF";
    private static String cardId = null;
    private static String cardData = "";
    private static String currentAID = "F0010203040506";
    private static String ndefMessage = null;
    
    public static void setEmulationMode(String mode) {
        emulationMode = mode;
    }
    
    public static void setCardData(String id, String data) {
        cardId = id;
        cardData = data;
    }
    
    public static void setAID(String aid) {
        currentAID = aid;
    }

    public static void setMessageToShare(String message) {
        cardData = message;
        emulationMode = "NDEF";
    }

    public static void setNdefMessage(String message) {
        ndefMessage = message;
    }

    public static String getNdefMessage() {
        return ndefMessage;
    }

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        String hexCommandApdu = bytesToHex(commandApdu);
        Log.d(TAG, "Received APDU: " + hexCommandApdu);

        switch (emulationMode) {
            case "MIFARE_CLASSIC":
                return processMifareClassic(commandApdu);
            case "ISO_DEP":
                return processIsoDep(commandApdu);
            case "NDEF":
                return processNdef(commandApdu);
            default:
                return UNKNOWN_CMD;
        }
    }

    private byte[] processNdef(byte[] commandApdu) {
        String hexCommandApdu = bytesToHex(commandApdu);
        if (isSelectAIDCommand(commandApdu)) {
            return SELECT_OK;
        } else if (isReadCommand(commandApdu)) {
            byte[] dataBytes = cardData.getBytes(StandardCharsets.UTF_8);
            byte[] response = new byte[dataBytes.length + 2];
            System.arraycopy(dataBytes, 0, response, 0, dataBytes.length);
            response[dataBytes.length] = (byte)0x90;
            response[dataBytes.length + 1] = (byte)0x00;
            return response;
        }
        return UNKNOWN_CMD;
    }

    private byte[] processMifareClassic(byte[] commandApdu) {
        if (cardId != null && commandApdu[0] == (byte)0x30) {
            return hexStringToByteArray(cardId);
        }
        return UNKNOWN_CMD;
    }

    private byte[] processIsoDep(byte[] commandApdu) {
        if (isSelectAIDCommand(commandApdu)) {
            return SELECT_OK;
        } else if (isReadCommand(commandApdu)) {
            byte[] dataBytes = cardData.getBytes(StandardCharsets.UTF_8);
            byte[] response = new byte[dataBytes.length + 2];
            System.arraycopy(dataBytes, 0, response, 0, dataBytes.length);
            response[dataBytes.length] = (byte)0x90;
            response[dataBytes.length + 1] = (byte)0x00;
            return response;
        }
        return UNKNOWN_CMD;
    }

    private byte[] processMifareUltralight(byte[] commandApdu) {
        if (commandApdu[0] == (byte)0x30) {
            // Handle anticollision
            return hexStringToByteArray("0400" + cardId);
        } else if (commandApdu[0] == (byte)0x60) {
            // Handle read command
            int page = commandApdu[1] & 0xFF;
            byte[] data = hexStringToByteArray(cardData);
            
            // If this is a request for NDEF data and we have NDEF message
            if (page == 4 && ndefMessage != null) {
                byte[] ndefData = ndefMessage.getBytes(StandardCharsets.UTF_8);
                byte[] response = new byte[16]; // 4 pages of 4 bytes each
                System.arraycopy(ndefData, 0, response, 0, Math.min(16, ndefData.length));
                return response;
            }
            
            // Normal data read
            byte[] response = new byte[16]; // 4 pages of 4 bytes each
            System.arraycopy(data, page * 4, response, 0, Math.min(16, data.length - page * 4));
            return response;
        }
        return UNKNOWN_CMD;
    }

    private boolean isSelectAIDCommand(byte[] commandApdu) {
        String hexCommand = bytesToHex(commandApdu);
        return hexCommand.startsWith(SELECT_APDU_HEADER);
    }

    private boolean isReadCommand(byte[] commandApdu) {
        String hexCommand = bytesToHex(commandApdu);
        return hexCommand.startsWith(READ_COMMAND);
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

    @Override
    public void onDeactivated(int reason) {
        Log.d(TAG, "Deactivated: " + reason);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString().toUpperCase();
    }
} 