import type { PluginListenerHandle } from '@capacitor/core';

export interface NfcPlugin {
  /**
   * Check if NFC is available and enabled on the device
   * @returns Promise with boolean indicating if NFC is enabled
   */
  isEnabled(): Promise<{ enabled: boolean }>;

  /**
   * Start listening for NFC tags
   * @returns Promise that resolves when the listener is started
   */
  startScanning(): Promise<void>;

  /**
   * Stop listening for NFC tags
   * @returns Promise that resolves when the listener is stopped
   */
  stopScanning(): Promise<void>;

  /**
   * Write data to an NFC tag
   * @param options Data to write to the tag
   * @returns Promise that resolves when data is written
   */
  write(options: { text: string }): Promise<void>;

  /**
   * Add listener for NFC events
   * @param eventName Name of the event to listen for
   * @param listenerFunc Callback function when event occurs
   */
  addListener(eventName: 'nfcTagDetected', listenerFunc: (event: {
    messages: NFCTagData[];
  }) => void): Promise<PluginListenerHandle>;
  /**
   * Add listener for NFC status events
   * @param eventName Name of the event to listen for
   * @param listenerFunc Callback function when event occurs
   */
  addListener(eventName: 'nfcStatus', listenerFunc: (event: {
    status: string;
  }) => void): Promise<PluginListenerHandle>;
  /**
   * Add listener for NFC error events
   * @param eventName Name of the event to listen for
   * @param listenerFunc Callback function when event occurs
   */
  addListener(eventName: 'nfcError', listenerFunc: (event: {
    error: string;
  }) => void): Promise<PluginListenerHandle>;
  /**
   * Add listener for NFC read success events
   * @param eventName Name of the event to listen for
   * @param listenerFunc Callback function when event occurs
   */
  addListener(eventName: 'readSuccess', listenerFunc: (event: {
    data: string;
  }) => void): Promise<PluginListenerHandle>;
  /**
   * Add listener for NFC write success events
   * @param eventName Name of the event to listen for
   * @param listenerFunc Callback function when event occurs
   */
  addListener(eventName: 'writeSuccess', listenerFunc: (event: {
    data: string;
  }) => void): Promise<PluginListenerHandle>;
  /**
   * Add listener for NFC write error events
   * @param eventName Name of the event to listen for
   * @param listenerFunc Callback function when event occurs
   */
  addListener(eventName: 'writeError', listenerFunc: (event: {
    error: string;
  }) => void): Promise<PluginListenerHandle>;

  /**
   * Get technical details about an NFC tag
   * @returns Promise with tag technology information
   */
  getTagInfo(): Promise<NFCTagInfo>;

  /**
   * Read data from an NFC tag
   * @returns Promise that resolves with the read data
   */
  read(): Promise<NFCReadResult>;
}

export interface NFCTagInfo {
  id: string;
  techTypes: string[];
  maxSize?: number;
  isWritable?: boolean;
  canMakeReadOnly?: boolean;
  type?: string;
  isFormatted?: boolean;
}

export interface NFCTagData {
  id?: string;
  type?: string;
  payload?: string;
  records?: NFCRecord[];
  techTypes?: string[];
  maxSize?: number;
  isWritable?: boolean;
}

export interface NFCRecord {
  type: string;
  payload: string;
  identifier: string;
}

export interface WebRTCConnectionInfo {
  type: 'offer' | 'answer';
  sdp: string;
  iceServers?: RTCIceServer[];
}

export interface WriteOptions {
  text?: string;
  webrtcData?: WebRTCConnectionInfo;
  mode?: 'reader' | 'emulator' | 'read';
  cardType?: string;
  aid?: string;
  secure?: boolean;
  timeout?: number;
}

// Add new event type
export interface WebRTCConnectionEvent {
  type: 'offer' | 'answer';
  data: WebRTCConnectionInfo;
}

// Add new interface for read results
export interface NFCReadResult {
  type: string;
  data?: string;
  records?: NFCRecord[];
  techTypes?: string[];
  id?: string;
  maxSize?: number;
  isWritable?: boolean;
}
