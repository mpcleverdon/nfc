import Foundation
import Capacitor
import CoreNFC

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(NfcPlugin)
public class NfcPlugin: CAPPlugin {
    private var readerSession: NFCTagReaderSession?
    private var writeSession: NFCTagReaderSession?
    private var emulationSession: NFCISO15693ReaderSession? // For tag emulation
    private var savedData: [String: Any]?
    
    @objc func isEnabled(_ call: CAPPluginCall) {
        if #available(iOS 13.0, *) {
            call.resolve(["enabled": NFCTagReaderSession.readingAvailable])
        } else {
            call.reject("NFC requires iOS 13.0 or later")
        }
    }
    
    @objc func write(_ call: CAPPluginCall) {
        guard #available(iOS 13.0, *) else {
            call.reject("NFC requires iOS 13.0 or later")
            return
        }
        
        let mode = call.getString("mode", "reader")
        
        if mode == "emulator" {
            if let originalData = call.getObject("originalData") {
                // iOS can emulate ISO15693 tags on iPhone 7 and later
                startTagEmulation(data: originalData, call: call)
            } else {
                // Normal HCE-F emulation (FeliCa)
                startHCEEmulation(call: call)
            }
        } else {
            startWriteSession(call: call)
        }
    }
    
    private func startTagEmulation(data: JSObject, call: CAPPluginCall) {
        guard #available(iOS 13.0, *) else {
            call.reject("Tag emulation requires iOS 13.0 or later")
            return
        }
        
        // iOS can emulate ISO15693 tags
        emulationSession = NFCISO15693ReaderSession(delegate: self)
        savedData = data as? [String: Any]
        
        let result = JSObject()
        result.setValue(true, forKey: "success")
        result.setValue("Device ready for tag emulation", forKey: "message")
        call.resolve(result)
    }
    
    private func startHCEEmulation(call: CAPPluginCall) {
        // iOS supports FeliCa card emulation on iPhone 7 and later
        if #available(iOS 13.0, *) {
            // Configure FeliCa system codes and service codes
            let systemCode = "0003" // Example system code
            let serviceCode = "000B" // Example service code
            
            // Start FeliCa card emulation
            // Note: This requires special entitlements from Apple
            call.reject("FeliCa card emulation requires special entitlements")
        } else {
            call.reject("HCE requires iOS 13.0 or later")
        }
    }
    
    // Add support for different card types
    private func handleTag(_ tag: NFCTag, session: NFCTagReaderSession) {
        switch tag {
        case .iso7816(let tag):
            handleISO7816Tag(tag)
        case .feliCa(let tag):
            handleFeliCaTag(tag)
        case .iso15693(let tag):
            handleISO15693Tag(tag)
        case .miFare(let tag):
            handleMiFareTag(tag)
        @unknown default:
            let error = JSObject()
            error.setValue("Unknown tag type", forKey: "error")
            notifyListeners("nfcError", data: error)
        }
    }
    
    // Add handlers for each card type...
    private func handleMiFareTag(_ tag: NFCMiFareTag) {
        let tagInfo = JSObject()
        tagInfo.setValue("MIFARE", forKey: "type")
        tagInfo.setValue(tag.identifier.map { String(format: "%02hhx", $0) }.joined(), forKey: "id")
        
        // Add MIFARE specific data
        if tag.mifareFamily == .desfire {
            tagInfo.setValue("MIFARE_DESFIRE", forKey: "subtype")
        } else if tag.mifareFamily == .ultralight {
            tagInfo.setValue("MIFARE_ULTRALIGHT", forKey: "subtype")
        }
        
        notifyListeners("nfcTagDetected", data: tagInfo)
    }
    
    // ... Add other handler implementations
}

// Add necessary protocol conformance
@available(iOS 13.0, *)
extension NfcPlugin: NFCTagReaderSessionDelegate {
    // Implement delegate methods
}
