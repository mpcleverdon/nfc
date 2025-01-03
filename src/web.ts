import { WebPlugin } from '@capacitor/core';

import type { NfcPlugin } from './definitions';

export class NfcWeb extends WebPlugin implements NfcPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}
