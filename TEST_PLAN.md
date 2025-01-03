# NFC Plugin Test Plan

## Prerequisites
- Android device with NFC capability
- iPhone 7 or newer with iOS 13+
- Various NFC tags (NDEF, MIFARE, FeliCa, etc.)

## Basic Functionality Tests

1. NFC Availability
   - [ ] Check if NFC is enabled on the device
   - [ ] Test behavior when NFC is disabled
   - [ ] Test behavior on devices without NFC

2. Scanning Tests
   - [ ] Start scanning
   - [ ] Stop scanning
   - [ ] Test concurrent scanning attempts
   - [ ] Test scanning timeout behavior

3. Tag Detection Tests
   - [ ] Test with NDEF formatted tags
   - [ ] Test with empty tags
   - [ ] Test with different tag types (MIFARE, FeliCa, etc.)
   - [ ] Verify tag ID detection
   - [ ] Verify tech list detection

4. Writing Tests
   - [ ] Write to empty NDEF tag
   - [ ] Write to pre-formatted tag
   - [ ] Test overwriting existing data
   - [ ] Test writing to read-only tags
   - [ ] Test writing with insufficient space

5. Error Handling Tests
   - [ ] Test tag removal during operations
   - [ ] Test invalid data writing
   - [ ] Test permission handling
   - [ ] Test error messages

## Platform Specific Tests

### Android
- [ ] Test foreground dispatch
- [ ] Test intent handling
- [ ] Test different Android versions
- [ ] Test permission requests

### iOS
- [ ] Test system NFC prompt
- [ ] Test background tag reading
- [ ] Test different iOS versions
- [ ] Test alert messages

## To Run Tests

1. Install the example app: