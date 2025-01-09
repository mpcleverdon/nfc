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
        <option value="clone">Clone Card Mode</option>`;

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
    
    // Add to the basic controls section
    basicControls.appendChild(createButton('Read Tag', async () => {
        try {
            const result = await Nfc.read();
            log('Read Result:', result);
        } catch (error) {
            logError('Read Error:', error);
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
    document.body.appendChild(logDiv);
    
    // Add clear log button
    const clearLogButton = createButton('Clear Log', () => {
        logDiv.innerHTML = '<h3>Event Log</h3>';
        log('Log cleared');
    });
    clearLogButton.style.backgroundColor = '#dc3545';
    controlsDiv.appendChild(clearLogButton);
    
    // Add clone card button
    basicControls.appendChild(createButton('Clone Card', async () => {
        try {
            // First read the original card
            const originalCard = await Nfc.read();
            log('Original Card Data:', originalCard);
            
            if (!originalCard || !originalCard.type) {
                throw new Error('Could not determine card type');
            }
            
            // Store card data for emulation
            await Nfc.write({
                mode: 'emulator',
                cardType: originalCard.type,
                aid: originalCard.aid || "F0010203040506",
                originalData: {
                    type: originalCard.type,
                    data: originalCard.data,
                    techTypes: originalCard.techTypes,
                    id: originalCard.id
                }
            });
            
            log('Ready to emulate card. Touch another device to clone.');
        } catch (error) {
            logError('Clone Error:', error);
        }
    }));
    
    // Add a specific clone button for MIFARE Ultralight
    basicControls.appendChild(createButton('Clone MIFARE Ultralight', async () => {
        try {
            // First read the original card
            const tagInfo = await Nfc.getTagInfo();
            log('Original Card Info:', tagInfo);
            
            if (tagInfo.type !== 'MIFARE_ULTRALIGHT') {
                throw new Error('This is not a MIFARE Ultralight card');
            }
            
            // Store the data for emulation
            await Nfc.write({
                mode: 'emulator',
                cardType: 'MIFARE_ULTRALIGHT',
                originalData: {
                    type: tagInfo.type,
                    data: tagInfo.data,
                    ndefMessage: tagInfo.ndefMessage
                }
            });
            
            log('Ready to emulate MIFARE Ultralight card. Touch another device to clone.');
        } catch (error) {
            logError('Clone Error:', error);
        }
    }));
    
    // Add a verify button to check if clone was successful
    basicControls.appendChild(createButton('Verify Clone', async () => {
        try {
            const newTagInfo = await Nfc.getTagInfo();
            log('Cloned Card Info:', newTagInfo);
            
            // Compare with original data
            if (savedOriginalData) {
                const isMatch = compareCardData(savedOriginalData, newTagInfo);
                log('Clone Verification:', isMatch ? 'SUCCESS' : 'FAILED');
            }
        } catch (error) {
            logError('Verify Error:', error);
        }
    }));
    
    // Helper function to compare card data
    function compareCardData(original, clone) {
        return original.data === clone.data && 
               original.ndefMessage === clone.ndefMessage;
    }
    
    return container;
}

function log(...args) {
    const timestamp = new Date().toISOString();
    const message = args.map(arg => 
        typeof arg === 'object' ? JSON.stringify(arg, null, 2) : arg
    ).join(' ');
    
    // Console logging
    console.log(`[${timestamp}]`, ...args);
    
    // UI logging
    const logSection = document.getElementById('logSection');
    if (!logSection) {
        console.error('Log section not found!');
        return;
    }

    const logEntry = document.createElement('div');
    logEntry.className = 'log-entry';
    logEntry.innerHTML = `
        <span class="timestamp">${timestamp}</span>
        <pre class="log-message">${message}</pre>
    `;
    logSection.insertBefore(logEntry, logSection.firstChild);
    
    // Keep only last 100 entries to prevent memory issues
    const entries = logSection.getElementsByClassName('log-entry');
    while (entries.length > 100) {
        logSection.removeChild(entries[entries.length - 1]);
    }
}

function logError(...args) {
    const timestamp = new Date().toISOString();
    const message = args.map(arg => {
        if (arg instanceof Error) {
            return `${arg.message}\n${arg.stack}`;
        }
        if (typeof arg === 'object') {
            try {
                return JSON.stringify(arg, null, 2);
            } catch (e) {
                return String(arg);
            }
        }
        return String(arg);
    }).join(' ');
    
    // Console logging
    console.error(`[${timestamp}] ERROR:`, ...args);
    
    // UI logging
    const logEntry = document.createElement('div');
    logEntry.className = 'log-entry error';
    logEntry.innerHTML = `
        <span class="timestamp">${timestamp}</span>
        <pre class="log-message error">${message}</pre>
    `;
    logSection.insertBefore(logEntry, logSection.firstChild);
}

async function setupEventListeners() {
    try {
        // Check initial NFC status
        const { enabled } = await Nfc.isEnabled();
        statusDiv.innerHTML = `NFC ${enabled ? 'is' : 'is not'} enabled`;
        
        // Set up NFC event listeners
        Nfc.addListener('nfcTagDetected', (event) => {
            log('Tag Detected:', {
                timestamp: new Date().toISOString(),
                ...event
            });
        });

        Nfc.addListener('nfcStatus', (event) => {
            log('NFC Status Changed:', {
                timestamp: new Date().toISOString(),
                ...event
            });
        });

        Nfc.addListener('writeSuccess', (event) => {
            log('Write Success:', {
                timestamp: new Date().toISOString(),
                ...event
            });
        });

        Nfc.addListener('writeError', (event) => {
            logError('Write Error:', {
                timestamp: new Date().toISOString(),
                ...event
            });
        });

        Nfc.addListener('nfcError', (event) => {
            logError('NFC Error:', event);
        });

        Nfc.addListener('readSuccess', (event) => {
            log('Read Success:', {
                timestamp: new Date().toISOString(),
                ...event
            });
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

        // Add a general error listener
        window.addEventListener('error', (event) => {
            logError('Global Error:', {
                message: event.message,
                filename: event.filename,
                lineno: event.lineno,
                colno: event.colno,
                error: event.error
            });
        });

        // Add unhandled promise rejection listener
        window.addEventListener('unhandledrejection', (event) => {
            logError('Unhandled Promise Rejection:', {
                reason: event.reason
            });
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

// Add a test log to verify it's working
window.addEventListener('DOMContentLoaded', () => {
    log('App initialized');
});

// Make log functions globally available
window.log = log;
window.logError = logError;

// Example from plugin
const writeTag = async () => {
  try {
    await Nfc.startScanning();
    const result = await Nfc.write({
      text: 'Hello from Capacitor NFC Plugin!',
      format: true // Format tag before writing
    });
    console.log('Write successful:', result);
  } catch (err) {
    console.error('Write failed:', err);
  } finally {
    await Nfc.stopScanning();
  }
};

const readTag = async () => {
  try {
    await Nfc.startScanning();
    const tag = await Nfc.read();
    console.log('Tag data:', tag);
  } catch (err) {
    console.error('Read failed:', err);
  } finally {
    await Nfc.stopScanning();
  }
};
