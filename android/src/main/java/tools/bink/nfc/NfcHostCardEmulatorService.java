package tools.bink.nfc;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

public class NfcHostCardEmulatorService extends HostApduService {
    private static final String TAG = "NfcHCEService";
    
    // ISO-7816 SELECT command header
    private static final String SELECT_APDU_HEADER = "00A40400";
    // "Hello World" AID
    private static final String AID = "F0010203040506";
    private static final byte[] SELECT_OK = {(byte)0x90, (byte)0x00};
    private static final byte[] UNKNOWN_CMD = {(byte)0x6F, (byte)0x00};
    
    private static String messageToShare = "";
    
    public static void setMessageToShare(String message) {
        messageToShare = message;
    }

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        String hexCommandApdu = bytesToHex(commandApdu);
        Log.d(TAG, "Received APDU: " + hexCommandApdu);

        if (hexCommandApdu.startsWith(SELECT_APDU_HEADER)) {
            // Return success response for SELECT command
            return SELECT_OK;
        } else if (hexCommandApdu.startsWith("00D0")) {
            // Handle READ command
            byte[] messageBytes = messageToShare.getBytes();
            byte[] response = new byte[messageBytes.length + 2];
            System.arraycopy(messageBytes, 0, response, 0, messageBytes.length);
            response[messageBytes.length] = (byte)0x90;
            response[messageBytes.length + 1] = (byte)0x00;
            return response;
        }
        
        return UNKNOWN_CMD;
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