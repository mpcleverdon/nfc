import type { PluginListenerHandle } from '@capacitor/core';

/**
 * @since 1.0.0
 * @interface NFCPlugin
 * @description Capacitor plugin for NFC operations including reading, writing, and card emulation.
 * @category Native
 */
export interface NFCPlugin {
  /**
   * Check if NFC is available and enabled on the device.
   * @since 1.0.0
   * @returns Promise with boolean indicating if NFC is enabled
   * @example
   * const { enabled } = await Nfc.isEnabled();
   */
  isEnabled(): Promise<{ enabled: boolean }>;

  /**
   * Start scanning for NFC tags.
   * @since 1.0.0
   * @returns Promise that resolves when scanning starts
   * @example
   * await Nfc.startScanning();
   */
  startScanning(): Promise<void>;

  /**
   * Stop scanning for NFC tags.
   * @returns Promise that resolves when scanning stops
   */
  stopScanning(): Promise<void>;

  /**
   * Write data to an NFC tag or share data via HCE.
   * @param options WriteOptions object containing the data and configuration
   * @returns Promise that resolves when write is complete
   */
  write(options: WriteOptions): Promise<void>;

  /**
   * Read data from an NFC tag.
   * @returns Promise with the read data
   */
  read(): Promise<{ data: string }>;

  /**
   * Get detailed information about a detected NFC tag.
   * @returns Promise with the tag information
   */
  getTagInfo(): Promise<NFCTagInfo>;

  /**
   * Add listener for NFC tag detection.
   * @param eventName - The name of the event to listen for
   * @param listenerFunc - The listener function to call
   * @returns Promise that resolves with the listener handle
   */
  addListener(
    eventName: 'nfcTagDetected',
    listenerFunc: (tag: NFCTagInfo) => void
  ): Promise<PluginListenerHandle>;

  /**
   * Add listener for NFC status changes.
   * @param eventName - The name of the event to listen for
   * @param listenerFunc - The listener function to call
   * @returns Promise that resolves with the listener handle
   */
  addListener(
    eventName: 'nfcStatus',
    listenerFunc: (status: { status: string }) => void
  ): Promise<PluginListenerHandle>;

  /**
   * Add listener for NFC errors.
   * @param eventName - The name of the event to listen for
   * @param listenerFunc - The listener function to call
   * @returns Promise that resolves with the listener handle
   */
  addListener(
    eventName: 'nfcError',
    listenerFunc: (error: { error: string }) => void
  ): Promise<PluginListenerHandle>;

  /**
   * Add listener for successful reads.
   * @param eventName - The name of the event to listen for
   * @param listenerFunc - The listener function to call
   * @returns Promise that resolves with the listener handle
   */
  addListener(
    eventName: 'readSuccess',
    listenerFunc: (tag: NFCTagInfo) => void
  ): Promise<PluginListenerHandle>;

  /**
   * Add listener for successful writes.
   * @param eventName - The name of the event to listen for
   * @param listenerFunc - The listener function to call
   * @returns Promise that resolves with the listener handle
   */
  addListener(
    eventName: 'writeSuccess',
    listenerFunc: (result: { written: boolean; type: string; message: string }) => void
  ): Promise<PluginListenerHandle>;

  /**
   * Add listener for write errors.
   * @param eventName - The name of the event to listen for
   * @param listenerFunc - The listener function to call
   * @returns Promise that resolves with the listener handle
   */
  addListener(
    eventName: 'writeError',
    listenerFunc: (error: { error: string }) => void
  ): Promise<PluginListenerHandle>;
}

export interface WriteOptions {
  /**
   * Text content to write to the tag
   */
  text?: string;
  
  /**
   * Operation mode: 'reader', 'emulator', or 'read'
   */
  mode?: 'reader' | 'emulator' | 'read';
  
  /**
   * Type of card to emulate or read
   */
  cardType?: string;
  
  /**
   * Application ID for ISO-DEP cards
   */
  aid?: string;
  
  /**
   * Enable secure communication
   */
  secure?: boolean;
  
  /**
   * Timeout in milliseconds
   */
  timeout?: number;
  
  /**
   * Original card data for cloning
   */
  originalData?: {
    type: string;
    data: string;
    techTypes?: string[];
    id?: string;
    ndefMessage?: string;
  };
}

export interface NFCTagInfo {
  /**
   * Unique identifier of the tag
   */
  id: string;
  
  /**
   * Type of NFC tag
   */
  type: string;
  
  /**
   * List of supported technologies
   */
  techTypes: string[];
  
  /**
   * Maximum size in bytes
   */
  maxSize?: number;
  
  /**
   * Whether the tag is writable
   */
  isWritable?: boolean;
  
  /**
   * NDEF message data if available
   */
  ndefMessage?: any;
  
  /**
   * Card-specific data
   */
  cardData?: string;
}

export interface NFCPluginEvents {
  /**
   * Emitted when an NFC tag is detected
   */
  nfcTagDetected: NFCTagInfo;
  
  /**
   * Emitted when NFC status changes
   */
  nfcStatus: { status: string };
  
  /**
   * Emitted when an error occurs
   */
  nfcError: { error: string };
  
  /**
   * Emitted when data is successfully read
   */
  readSuccess: NFCTagInfo;
  
  /**
   * Emitted when data is successfully written
   */
  writeSuccess: { written: boolean; type: string; message: string };
  
  /**
   * Emitted when a write operation fails
   */
  writeError: { error: string };
}
