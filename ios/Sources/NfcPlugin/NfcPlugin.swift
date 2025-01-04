import Foundation
import Capacitor
import CoreNFC

/**
 * Please read the Capacitor iOS Plugin Development Guide
 * here: https://capacitorjs.com/docs/plugins/ios
 */
@objc(NfcPlugin)
public class NfcPlugin: CAPPlugin, NFCNDEFReaderSessionDelegate {
    private var readerSession: NFCNDEFReaderSession?
    private var scanningCall: CAPPluginCall?
    private var writeData: String?
    private var writeSession: NFCNDEFReaderSession?
    
    @objc func isEnabled(_ call: CAPPluginCall) {
        if #available(iOS 13.0, *) {
            call.resolve(["enabled": NFCNDEFReaderSession.readingAvailable])
        } else {
            call.reject("NFC requires iOS 13.0 or later")
        }
    }
    
    @objc func startScanning(_ call: CAPPluginCall) {
        if #available(iOS 13.0, *) {
            guard NFCNDEFReaderSession.readingAvailable else {
                call.reject("NFC is not available on this device")
                return
            }
            
            if readerSession != nil {
                call.reject("NFC scanning session already in progress")
                return
            }
            
            readerSession = NFCNDEFReaderSession(delegate: self,
                                                queue: nil,
                                                invalidateAfterFirstRead: false)
            readerSession?.alertMessage = "Hold your iPhone near an NFC tag"
            readerSession?.begin()
            scanningCall = call
            call.resolve()
        } else {
            call.reject("NFC requires iOS 13.0 or later")
        }
    }
    
    @objc func stopScanning(_ call: CAPPluginCall) {
        if #available(iOS 13.0, *) {
            guard let session = readerSession else {
                call.reject("No active NFC scanning session")
                return
            }
            
            session.invalidate()
            readerSession = nil
            scanningCall = nil
            call.resolve()
        } else {
            call.reject("NFC requires iOS 13.0 or later")
        }
    }
    
    @objc func write(_ call: CAPPluginCall) {
        if #available(iOS 13.0, *) {
            guard NFCNDEFReaderSession.readingAvailable else {
                call.reject("NFC is not available on this device")
                return
            }
            
            guard let text = call.getString("text") else {
                call.reject("Text to write is required")
                return
            }
            
            if readerSession != nil {
                call.reject("Another NFC operation is in progress")
                return
            }
            
            // Store the text to write and the call
            writeData = text
            scanningCall = call
            
            // Create and configure write session
            writeSession = NFCNDEFReaderSession(delegate: self,
                                                queue: nil,
                                                invalidateAfterFirstRead: false)
            writeSession?.alertMessage = "Hold your iPhone near an NFC tag to write"
            writeSession?.begin()
        } else {
            call.reject("NFC requires iOS 13.0 or later")
        }
    }
    
    @objc func read(_ call: CAPPluginCall) {
        if #available(iOS 13.0, *) {
            guard NFCNDEFReaderSession.readingAvailable else {
                call.reject("NFC is not available on this device")
                return
            }
            
            scanningCall = call
            
            readerSession = NFCNDEFReaderSession(delegate: self,
                                                queue: nil,
                                                invalidateAfterFirstRead: true)
            readerSession?.alertMessage = "Hold your iPhone near an NFC tag"
            readerSession?.begin()
        } else {
            call.reject("NFC requires iOS 13.0 or later")
        }
    }
    
    // MARK: - NFCNDEFReaderSessionDelegate
    
    public func readerSession(_ session: NFCNDEFReaderSession,
                            didInvalidateWithError error: Error) {
        DispatchQueue.main.async {
            if let error = error as? NFCReaderError {
                switch error.code {
                case .readerSessionInvalidationErrorFirstNDEFTagRead:
                    // Handle successful read
                    break
                case .readerSessionInvalidationErrorUserCanceled:
                    self.scanningCall?.reject("User cancelled NFC session")
                default:
                    self.scanningCall?.reject("NFC session error: \(error.localizedDescription)")
                }
            } else {
                self.scanningCall?.reject("NFC session error: \(error.localizedDescription)")
            }
            
            self.readerSession = nil
            self.scanningCall = nil
        }
    }
    
    public func readerSession(_ session: NFCNDEFReaderSession,
                            didDetectNDEFs messages: [NFCNDEFMessage]) {
        // Process detected messages
        let results = messages.map { message -> [[String: Any]] in
            return message.records.map { record -> [String: Any] in
                return [
                    "type": String(data: record.type, encoding: .utf8) ?? "",
                    "payload": String(data: record.payload, encoding: .utf8) ?? "",
                    "identifier": String(data: record.identifier, encoding: .utf8) ?? ""
                ]
            }
        }
        
        DispatchQueue.main.async {
            self.notifyListeners("nfcTagDetected", data: ["messages": results])
        }
    }
    
    @available(iOS 13.0, *)
    public func readerSession(_ session: NFCNDEFReaderSession,
                             didDetect tags: [NFCNDEFTag]) {
        guard let tag = tags.first else {
            session.invalidate(errorMessage: "No tag found")
            return
        }
        
        session.connect(to: tag) { error in
            if let error = error {
                session.invalidate(errorMessage: "Connection error: \(error.localizedDescription)")
                return
            }
            
            tag.queryNDEFStatus { status, capacity, error in
                if let error = error {
                    session.invalidate(errorMessage: "Failed to query tag: \(error.localizedDescription)")
                    return
                }
                
                var result: [String: Any] = [
                    "type": "NDEF",
                    "isWritable": status == .readWrite,
                    "maxSize": capacity
                ]
                
                tag.readNDEF { message, error in
                    if let error = error {
                        session.invalidate(errorMessage: "Read error: \(error.localizedDescription)")
                        return
                    }
                    
                    if let message = message {
                        let records = message.records.map { record -> [String: Any] in
                            var recordData: [String: Any] = [
                                "type": String(data: record.type, encoding: .utf8) ?? "",
                                "identifier": String(data: record.identifier, encoding: .utf8) ?? ""
                            ]
                            
                            if record.typeNameFormat == .nfcWellKnown && record.type == "T".data(using: .utf8) {
                                if let payload = String(data: record.payload.dropFirst(), encoding: .utf8) {
                                    recordData["payload"] = payload
                                }
                            } else {
                                recordData["payload"] = String(data: record.payload, encoding: .utf8) ?? ""
                            }
                            
                            return recordData
                        }
                        
                        result["records"] = records
                        
                        DispatchQueue.main.async {
                            self.scanningCall?.resolve(result)
                            session.invalidate()
                        }
                    }
                }
            }
        }
    }
    
    @available(iOS 13.0, *)
    private func handleTag(session: NFCNDEFReaderSession, tag: NFCNDEFTag) {
        // Query tag capabilities
        tag.queryNDEFStatus { status, capacity, error in
            if let error = error {
                session.invalidate(errorMessage: "Failed to query tag: \(error.localizedDescription)")
                return
            }
            
            var tagInfo: [String: Any] = [
                "isWritable": status == .readWrite,
                "maxSize": capacity,
                "isFormatted": status != .notSupported
            ]
            
            // Get tag type information
            if let tag = tag as? NFCFeliCaTag {
                self.handleFeliCaTag(tag, tagInfo: &tagInfo)
            } else if let tag = tag as? NFCISO15693Tag {
                self.handleISO15693Tag(tag, tagInfo: &tagInfo)
            } else if let tag = tag as? NFCISO7816Tag {
                self.handleISO7816Tag(tag, tagInfo: &tagInfo)
            } else if let tag = tag as? NFCMiFareTag {
                self.handleMiFareTag(tag, tagInfo: &tagInfo)
            }
            
            // Read NDEF message if available
            tag.readNDEF { message, error in
                if let error = error {
                    session.invalidate(errorMessage: "Read error: \(error.localizedDescription)")
                    return
                }
                
                if let message = message {
                    self.handleNdefMessage(message, tagInfo: &tagInfo)
                    DispatchQueue.main.async {
                        self.notifyListeners("nfcTagDetected", data: tagInfo)
                    }
                    session.alertMessage = "Tag read successfully!"
                    session.invalidate()
                }
            }
        }
    }
    
    @available(iOS 13.0, *)
    private func handleFeliCaTag(_ tag: NFCFeliCaTag, tagInfo: inout [String: Any]) {
        tagInfo["type"] = "FeliCa"
        tagInfo["currentSystemCode"] = Data(tag.currentSystemCode).hexString
        tagInfo["currentIDm"] = Data(tag.currentIDm).hexString
    }
    
    @available(iOS 13.0, *)
    private func handleISO15693Tag(_ tag: NFCISO15693Tag, tagInfo: inout [String: Any]) {
        tagInfo["type"] = "ISO15693"
        tagInfo["icManufacturerCode"] = tag.icManufacturerCode
        tagInfo["icSerialNumber"] = Data(tag.icSerialNumber).hexString
    }
    
    @available(iOS 13.0, *)
    private func handleISO7816Tag(_ tag: NFCISO7816Tag, tagInfo: inout [String: Any]) {
        tagInfo["type"] = "ISO7816"
        tagInfo["initialSelectedAID"] = tag.initialSelectedAID?.hexString
        tagInfo["identifier"] = Data(tag.identifier).hexString
    }
    
    @available(iOS 13.0, *)
    private func handleMiFareTag(_ tag: NFCMiFareTag, tagInfo: inout [String: Any]) {
        tagInfo["type"] = "MiFare"
        tagInfo["identifier"] = Data(tag.identifier).hexString
        tagInfo["mifareFamily"] = tag.mifareFamily.rawValue
    }
    
    @available(iOS 13.0, *)
    private func performWrite(session: NFCNDEFReaderSession, tag: NFCNDEFTag, text: String) {
        // Check tag status
        tag.queryNDEFStatus { status, capacity, error in
            if let error = error {
                session.invalidate(errorMessage: "Failed to query tag: \(error.localizedDescription)")
                return
            }
            
            guard status == .readWrite else {
                session.invalidate(errorMessage: "Tag is not writable")
                return
            }
            
            // Create NDEF message
            let textPayload = NFCNDEFPayload.wellKnownTypeTextPayload(
                string: text,
                locale: Locale(identifier: "en")
            )
            
            guard let payload = textPayload else {
                session.invalidate(errorMessage: "Failed to create NDEF payload")
                return
            }
            
            let message = NFCNDEFMessage(records: [payload])
            
            // Write to tag
            tag.writeNDEF(message) { error in
                if let error = error {
                    session.invalidate(errorMessage: "Write failed: \(error.localizedDescription)")
                    self.scanningCall?.reject("Failed to write to tag: \(error.localizedDescription)")
                } else {
                    session.alertMessage = "Successfully wrote to tag!"
                    session.invalidate()
                    self.scanningCall?.resolve()
                }
                
                // Clear write data
                self.writeData = nil
            }
        }
    }
    
    private func handleNdefMessage(_ message: NFCNDEFMessage) {
        let records = message.records.map { record -> [String: Any] in
            var recordData: [String: Any] = [
                "type": String(data: record.type, encoding: .utf8) ?? "",
                "identifier": String(data: record.identifier, encoding: .utf8) ?? ""
            ]
            
            // Handle different types of records
            if record.typeNameFormat == .nfcWellKnown && record.type == "T".data(using: .utf8) {
                // Text record
                if let payload = String(data: record.payload.dropFirst(), encoding: .utf8) {
                    recordData["payload"] = payload
                }
            } else {
                // Other record types
                recordData["payload"] = String(data: record.payload, encoding: .utf8) ?? ""
            }
            
            return recordData
        }
        
        DispatchQueue.main.async {
            self.notifyListeners("nfcTagDetected", data: [
                "messages": [
                    [
                        "records": records
                    ]
                ]
            ])
        }
    }
}

// Add extension for hex string conversion
extension Data {
    var hexString: String {
        return map { String(format: "%02hhx", $0) }.joined()
    }
}
