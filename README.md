<!-- trunk-ignore-all(prettier) -->
# Capacitor NFC Plugin

![npm version](https://img.shields.io/npm/v/capacitor-nfc-plugin)
![npm downloads](https://img.shields.io/npm/dm/capacitor-nfc-plugin)
![npm license](https://img.shields.io/npm/l/capacitor-nfc-plugin)

## Install

```bash
npm install capacitor-nfc-plugin
npx cap sync
```

## Usage Examples

### Basic NFC Reading

```typescript
// Read any NFC tag
const readResult = await Nfc.read();
console.log('Read Result:', readResult);

// Get detailed tag info
const tagInfo = await Nfc.getTagInfo();
console.log('Tag Info:', tagInfo);
```

### Reading Different Card Types

1. NDEF Formatted Cards:

```typescript
// Read NDEF data
const result = await Nfc.read();
// Result will contain formatted NDEF messages
console.log('NDEF Data:', result.records);
```

2. MIFARE Classic Cards:

```typescript
// Read MIFARE Classic card
const tagInfo = await Nfc.getTagInfo();
if (tagInfo.type === 'MIFARE_CLASSIC') {
    console.log('Card Size:', tagInfo.size);
    console.log('Sector Count:', tagInfo.sectorCount);
    console.log('Block Count:', tagInfo.blockCount);
    // If authentication succeeded, you'll see block0 data
    if (tagInfo.block0) {
        console.log('Block 0 Data:', tagInfo.block0);
    }
}
```

3. ISO-DEP Cards (Banking cards, ID cards):

```typescript
// Read ISO-DEP card with specific AID
await Nfc.write({
    mode: 'reader',
    cardType: 'iso_dep',
    aid: 'F0010203040506', // Replace with your card's AID
    text: ''  // Empty text indicates read mode
});

// Listen for the read result
Nfc.addListener('readSuccess', (event) => {
    console.log('Card Data:', event.data);
    console.log('Historical Bytes:', event.historicalBytes);
});
```

### Example Response Data

1. MIFARE Classic Response:

```json
{
    "type": "MIFARE_CLASSIC",
    "size": 1024,
    "sectorCount": 16,
    "blockCount": 64,
    "block0": "0123456789ABCDEF",
    "maxTransceiveLength": 253
}
```

2. ISO-DEP Response:

```json
{
    "type": "ISO_DEP",
    "hiLayerResponse": "0123456789",
    "historicalBytes": "ABCDEF0123",
    "cardData": "0123456789ABCDEF"
}
```

3. NDEF Message Response:

```json
{
    "type": "NDEF",
    "records": [
        {
            "type": "text/plain",
            "payload": "Hello NFC",
            "identifier": ""
        }
    ]
}
```

## API

<docgen-index>

* [`isEnabled()`](#isenabled)
* [`startScanning()`](#startscanning)
* [`stopScanning()`](#stopscanning)
* [`write(...)`](#write)
* [`addListener('nfcTagDetected', ...)`](#addlistenernfctagdetected-)
* [`addListener('nfcStatus', ...)`](#addlistenernfcstatus-)
* [`addListener('nfcError', ...)`](#addlistenernfcerror-)
* [`addListener('readSuccess', ...)`](#addlistenerreadsuccess-)
* [`addListener('writeSuccess', ...)`](#addlistenerwritesuccess-)
* [`addListener('writeError', ...)`](#addlistenerwriteerror-)
* [`getTagInfo()`](#gettaginfo)
* [`read()`](#read)
* [Interfaces](#interfaces)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### echo(...)

```typescript
echo(options: { value: string; }) => Promise<{ value: string; }>
```

| Param         | Type                            |
| ------------- | ------------------------------- |
| **`options`** | <code>{ value: string; }</code> |

**Returns:** <code>Promise&lt;{ value: string; }&gt;</code>

--------------------

## API Reference

### Methods

#### `isEnabled()`
Check if NFC is available and enabled.

- Returns: `Promise<{ enabled: boolean }>`

#### `startScanning()`
Start listening for NFC tags.

- Returns: `Promise<void>`

#### `stopScanning()`
Stop listening for NFC tags.

- Returns: `Promise<void>`

#### `write(options: WriteOptions)`
Write data to an NFC tag or share data via HCE.

- Parameters:
  ```typescript
  interface WriteOptions {
    text?: string;
    webrtcData?: WebRTCConnectionInfo;
    mode?: 'reader' | 'emulator' | 'read';
    cardType?: string;
    aid?: string;
    secure?: boolean;
    timeout?: number;
  }
  ```
- Returns: `Promise<void>`

#### `getTagInfo()`
Get technical details about a detected NFC tag.

- Returns: `Promise<NFCTagInfo>`

### Events

#### `nfcTagDetected`
Emitted when an NFC tag is detected.

- Data: `{ messages: NFCTagData[] }`

#### `nfcStatus`
Emitted when NFC status changes.

- Data: `{ status: string }`

#### `writeSuccess`
Emitted when data is successfully written.

- Data: `{ written: boolean, type: string, message: string }`

#### `writeError`
Emitted when a write operation fails.

- Data: `{ error: string }`

#### `readSuccess`
Emitted when data is successfully read.

- Data: `{ type: string, data: string }`

#### `webrtcOffer`
Emitted when a WebRTC offer is received.

- Data: `WebRTCConnectionEvent`

#### `webrtcAnswer`
Emitted when a WebRTC answer is received.

- Data: `WebRTCConnectionEvent`

## Supported Tag Types

### Android

- NDEF formatted tags
- ISO-DEP (ISO 14443-4)
- MIFARE Classic*
- MIFARE Ultralight
- ISO 15693
- ISO 7816

### iOS

- NDEF formatted tags
- ISO 7816 (ISO-DEP)
- ISO 15693
- FeliCa
- MIFARE
- ISO 14443 Type A/B
- VAS (Value Added Service)

*Note: 

- MIFARE Classic support varies by device manufacturer on Android
- iOS 13+ required for tag reading
- iOS 14+ required for Value Added Service (VAS) tags
- Some tag types may require special entitlements in iOS

## iOS Configuration

1. Add NFC capabilities in Xcode:
   - Navigate to your target's "Signing & Capabilities"
   - Click "+" and add "Near Field Communication Tag Reading"

2. Add required entries to Info.plist:

```xml
<!-- Basic NFC reading -->
<key>NFCReaderUsageDescription</key>
<string>This app needs access to NFC reading to communicate with NFC tags and devices.</string>

<!-- For ISO7816, ISO15693, MIFARE tags -->
<key>com.apple.developer.nfc.readersession.formats</key>
<array>
    <string>NDEF</string>
    <string>TAG</string>
</array>

<!-- For specific Application IDs (AIDs) -->
<key>com.apple.developer.nfc.readersession.iso7816.select-identifiers</key>
<array>
    <string>F0010203040506</string>
</array>

<!-- For VAS (iOS 14+) -->
<key>com.apple.developer.nfc.readersession.vpay</key>
<true/>
```

3. Required Entitlements:

- com.apple.developer.nfc.readersession
- com.apple.developer.nfc.readersession.formats
- com.apple.developer.nfc.readersession.iso7816.select-identifiers (for ISO7816)
- com.apple.developer.nfc.readersession.vpay (for VAS)

4. Minimum iOS Version Requirements:

- iOS 13+: Basic NFC tag reading
- iOS 14+: VAS support
- iOS 15+: Enhanced security features

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

MIT License - see LICENSE file for details

## Support

For issues and feature requests, please use the GitHub issue tracker.

## Credits

Created by Mark Cleverdon (@mpcleverdon)
