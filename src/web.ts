import { WebPlugin } from '@capacitor/core';
import type { NfcPlugin } from './definitions';

export class NfcWeb extends WebPlugin implements NfcPlugin {
  async isEnabled(): Promise<{ enabled: boolean }> {
    throw this.unavailable('NFC not available in browser');
  }

  async startScanning(): Promise<void> {
    throw this.unavailable('NFC not available in browser');
  }

  async stopScanning(): Promise<void> {
    throw this.unavailable('NFC not available in browser');
  }

  async write(options: { text: string }): Promise<void> {
    throw this.unavailable('NFC not available in browser');
  }
}
