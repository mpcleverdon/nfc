import { Nfc } from 'nfc';

// Add this right after the import
console.log('Script loaded');

// UI Elements
const statusDiv = document.createElement('div');
const logDiv = document.createElement('div');
const controlsDiv = document.createElement('div');

function createButton(text, onClick) {
    const button = document.createElement('button');
    button.innerHTML = text;
    button.onclick = onClick;
    return button;
}

function createWebRTCControls() {
    const webrtcControls = document.createElement('div');
    webrtcControls.className = 'control-group';
    webrtcControls.innerHTML = '<h3>WebRTC Controls</h3>';

    const peerConnection = new RTCPeerConnection({
        iceServers: [
            { urls: 'stun:stun.l.google.com:19302' }
        ]
    });

    // Add data channel
    const dataChannel = peerConnection.createDataChannel('messageChannel');
    dataChannel.onmessage = event => {
        log('Received message:', event.data);
    };

    // Create Offer button
    webrtcControls.appendChild(createButton('Create Offer', async () => {
        try {
            const offer = await peerConnection.createOffer();
            await peerConnection.setLocalDescription(offer);
            
            // Share offer via NFC
            await Nfc.write({
                webrtcData: {
                    type: 'offer',
                    sdp: offer.sdp,
                    iceServers: peerConnection.getConfiguration().iceServers
                },
                mode: 'emulator'
            });
            log('Ready to share WebRTC offer. Touch devices together.');
        } catch (error) {
            logError('Create Offer Error:', error);
        }
    }));

    // Receive Offer button
    webrtcControls.appendChild(createButton('Receive Offer', async () => {
        try {
            await Nfc.write({
                mode: 'reader',
                text: ''  // Empty text indicates read mode
            });
            log('Ready to receive WebRTC offer. Touch devices together.');
        } catch (error) {
            logError('Receive Offer Error:', error);
        }
    }));

    // Handle ICE candidates
    peerConnection.onicecandidate = event => {
        if (event.candidate) {
            log('New ICE candidate:', event.candidate);
        }
    };

    // Handle connection state changes
    peerConnection.onconnectionstatechange = () => {
        log('Connection state:', peerConnection.connectionState);
    };

    return { webrtcControls, peerConnection };
}

function createTestUI() {
    const container = document.createElement('div');
    
    // Status display
    statusDiv.id = 'nfcStatus';
    statusDiv.innerHTML = 'NFC Status: Checking...';
    container.appendChild(statusDiv);
    
    // Controls Section
    controlsDiv.className = 'controls-section';
    
    // Basic NFC Controls
    const basicControls = document.createElement('div');
    basicControls.className = 'control-group';
    basicControls.innerHTML = '<h3>Basic Controls</h3>';
    
    const textInput = document.createElement('input');
    textInput.type = 'text';
    textInput.value = 'Test NFC Message';
    textInput.placeholder = 'Enter text to write';
    
    basicControls.appendChild(textInput);
    basicControls.appendChild(createButton('Check NFC Status', async () => {
        try {
            const { enabled } = await Nfc.isEnabled();
            log(`NFC is ${enabled ? 'enabled' : 'disabled'}`);
        } catch (error) {
            logError('Check Status Error:', error);
        }
    }));
    
    basicControls.appendChild(createButton('Start Scanning', async () => {
        try {
            await Nfc.startScanning();
            log('Scanning started');
        } catch (error) {
            logError('Start Scanning Error:', error);
        }
    }));
    
    basicControls.appendChild(createButton('Stop Scanning', async () => {
        try {
            await Nfc.stopScanning();
            log('Scanning stopped');
        } catch (error) {
            logError('Stop Scanning Error:', error);
        }
    }));
    
    // Add a mode selector
    const modeSelect = document.createElement('select');
    modeSelect.innerHTML = `
        <option value="reader">Reader Mode</option>
        <option value="emulator">Card Emulator Mode</option>
        <option value="read">Read Only Mode</option>
    `;

    basicControls.appendChild(modeSelect);
    basicControls.appendChild(createButton('Write/Share/Read', async () => {
        try {
            const mode = modeSelect.value;
            if (mode === 'read') {
                await Nfc.write({ 
                    mode: 'reader',
                    aid: "F0010203040506",
                    text: ''  // Empty text indicates read mode
                });
                log('Ready to read. Touch devices together.');
            } else {
                await Nfc.write({ 
                    text: textInput.value,
                    mode: mode,
                    aid: "F0010203040506"
                });
                log(`Ready to ${mode === 'emulator' ? 'share' : 'write'}. Touch devices together.`);
            }
        } catch (error) {
            logError('Operation Error:', error);
        }
    }));
    
    // Tag Info Section
    const tagControls = document.createElement('div');
    tagControls.className = 'control-group';
    tagControls.innerHTML = '<h3>Tag Information</h3>';
    
    tagControls.appendChild(createButton('Get Tag Info', async () => {
        try {
            const info = await Nfc.getTagInfo();
            log('Tag Info:', info);
        } catch (error) {
            logError('Get Tag Info Error:', error);
        }
    }));
    
    // Add WebRTC controls
    const { webrtcControls, peerConnection } = createWebRTCControls();
    window.webrtcState = { peerConnection };  // Store for global access
    controlsDiv.appendChild(webrtcControls);
    
    // Add controls to container
    controlsDiv.appendChild(basicControls);
    controlsDiv.appendChild(tagControls);
    container.appendChild(controlsDiv);
    
    // Log Section
    logDiv.id = 'logSection';
    logDiv.innerHTML = '<h3>Event Log</h3>';
    container.appendChild(logDiv);
    
    return container;
}

function log(...args) {
    const message = args.map(arg => 
        typeof arg === 'object' ? JSON.stringify(arg, null, 2) : arg
    ).join(' ');
    
    const logEntry = document.createElement('div');
    logEntry.className = 'log-entry';
    logEntry.innerHTML = `
        <span class="timestamp">${new Date().toLocaleTimeString()}</span>
        <pre class="log-message">${message}</pre>
    `;
    logDiv.insertBefore(logEntry, logDiv.firstChild);
    console.log(...args);
}

function logError(...args) {
    const logEntry = document.createElement('div');
    logEntry.className = 'log-entry error';
    const message = args.map(arg => {
        if (arg instanceof Error) return arg.message;
        if (typeof arg === 'object') {
            try {
                return JSON.stringify(arg, null, 2);
            } catch (e) {
                return String(arg);
            }
        }
        return String(arg);
    }).join(' ');
    
    logEntry.innerHTML = `
        <span class="timestamp">${new Date().toLocaleTimeString()}</span>
        <pre class="log-message error">${message}</pre>
    `;
    logDiv.insertBefore(logEntry, logDiv.firstChild);
    console.error(...args);
}

async function setupEventListeners() {
    try {
        // Check initial NFC status
        const { enabled } = await Nfc.isEnabled();
        statusDiv.innerHTML = `NFC ${enabled ? 'is' : 'is not'} enabled`;
        
        // Set up NFC event listeners
        Nfc.addListener('nfcTagDetected', (event) => {
            log('Tag Detected:', event);
        });

        Nfc.addListener('nfcStatus', (event) => {
            log('NFC Status:', event);
            statusDiv.innerHTML = `Status: ${event.status}`;
        });

        Nfc.addListener('writeSuccess', (event) => {
            log('Write Success:', event);
        });

        Nfc.addListener('writeError', (event) => {
            logError('Write Error:', event);
        });

        Nfc.addListener('nfcError', (event) => {
            logError('NFC Error:', event);
        });

        Nfc.addListener('readSuccess', (event) => {
            log('Read Success:', event);
            if (event.data) {
                const dataDiv = document.createElement('div');
                dataDiv.className = 'read-data';
                dataDiv.innerHTML = `<strong>Read Data:</strong> ${event.data}`;
                logDiv.insertBefore(dataDiv, logDiv.firstChild);
            }
        });

        // Add WebRTC-specific listeners
        Nfc.addListener('webrtcOffer', async (event) => {
            try {
                const { peerConnection } = window.webrtcState;
                const { sdp, type, iceServers } = event.data;

                // Update ICE servers if provided
                if (iceServers) {
                    peerConnection.setConfiguration({ iceServers });
                }

                // Handle offer
                await peerConnection.setRemoteDescription(new RTCSessionDescription({
                    type,
                    sdp
                }));

                // Create answer
                const answer = await peerConnection.createAnswer();
                await peerConnection.setLocalDescription(answer);

                // Share answer via NFC
                await Nfc.write({
                    webrtcData: {
                        type: 'answer',
                        sdp: answer.sdp
                    },
                    mode: 'emulator'
                });
                log('Ready to share WebRTC answer. Touch devices together.');
            } catch (error) {
                logError('Handle Offer Error:', error);
            }
        });

        Nfc.addListener('webrtcAnswer', async (event) => {
            try {
                const { peerConnection } = window.webrtcState;
                const { sdp, type } = event.data;

                await peerConnection.setRemoteDescription(new RTCSessionDescription({
                    type,
                    sdp
                }));
                log('WebRTC connection established!');
            } catch (error) {
                logError('Handle Answer Error:', error);
            }
        });

    } catch (error) {
        logError('Initialization Error:', error);
    }
}

// Initialize UI
export function initializeNfcTest() {
    console.log('Initializing NFC Test UI');
    try {
        const ui = createTestUI();
        console.log('UI created');
        document.body.appendChild(ui);
        console.log('UI added to document');
        setupEventListeners().catch(error => {
            console.error('Error setting up event listeners:', error);
        });
    } catch (error) {
        console.error('Error initializing UI:', error);
        // Add a visible error message to the page
        const errorDiv = document.createElement('div');
        errorDiv.style.color = 'red';
        errorDiv.style.padding = '20px';
        errorDiv.innerHTML = `Error initializing: ${error.message}`;
        document.body.appendChild(errorDiv);
    }
}

// Update the initialization
document.addEventListener('DOMContentLoaded', () => {
    console.log('DOM Content Loaded');
    initializeNfcTest();
});
