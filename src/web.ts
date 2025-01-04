import { WebPlugin } from '@capacitor/core';
import type { PluginListenerHandle } from '@capacitor/core';
import type { NfcPlugin, NFCTagInfo, NFCReadResult } from './definitions';

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
    console.log('Write attempted with options:', options);
    throw this.unavailable('NFC not available in browser');
  }

  async getTagInfo(): Promise<NFCTagInfo> {
    throw this.unavailable('NFC not available in browser');
  }

  async addListener(
    eventName: string,
    listenerFunc: (event: any) => void
  ): Promise<PluginListenerHandle> {
    return super.addListener(eventName, listenerFunc);
  }

  async read(): Promise<NFCReadResult> {
    throw this.unavailable('NFC not available in browser');
  }
}
