import { WebPlugin } from '@capacitor/core';

import type { NFCPlugin, WriteOptions, NFCTagInfo } from './definitions';

/**
 * @capacitor-plugin Nfc
 * @name Nfc
 * @description Capacitor plugin for NFC operations including reading, writing, and card emulation.
 * @packageName capacitor-nfc-plugin
 * @repo https://github.com/mpcleverdon/capacitor-nfc-plugin
 * @issue https://github.com/mpcleverdon/capacitor-nfc-plugin/issues
 * @version 1.1.2
 * @since 1.0.0
 * @platforms android,ios
 */
export class NFCWeb extends WebPlugin implements NFCPlugin {
  constructor() {
    super({
      name: 'Nfc',
      platforms: ['web']
    });
  }

  /**
   * Check if NFC is available and enabled.
   * @returns Promise that resolves with NFC availability status
   */
  async isEnabled(): Promise<{ enabled: boolean }> {
    return { enabled: false };
  }

  /**
   * Start scanning for NFC tags.
   * @throws Error NFC is not available in browser
   */
  async startScanning(): Promise<void> {
    throw this.unavailable('NFC not available in browser');
  }

  /**
   * Stop scanning for NFC tags.
   * @throws Error NFC is not available in browser
   */
  async stopScanning(): Promise<void> {
    throw this.unavailable('NFC not available in browser');
  }

  /**
   * Write data to an NFC tag.
   * @param _options WriteOptions object containing the data and configuration
   * @throws Error NFC is not available in browser
   */
  async write(_options: WriteOptions): Promise<void> {
    throw this.unavailable('NFC not available in browser');
  }

  /**
   * Read data from an NFC tag.
   * @returns Promise that resolves with the read data
   * @throws Error NFC is not available in browser
   */
  async read(): Promise<{ data: string }> {
    throw this.unavailable('NFC not available in browser');
  }

  /**
   * Get detailed information about a detected NFC tag.
   * @returns Promise that resolves with the tag information
   * @throws Error NFC is not available in browser
   */
  async getTagInfo(): Promise<NFCTagInfo> {
    throw this.unavailable('NFC not available in browser');
  }

  /**
   * Helper method to generate consistent error messages.
   * @param feature - The feature that is not available
   * @returns Error with a formatted message
   */
  protected unavailable(feature: string): Error {
    return new Error(`${feature}. This feature is only available on native platforms.`);
  }
}

// Export the plugin instance
export const Nfc = new NFCWeb();
