import { registerPlugin } from '@capacitor/core';

import type { NfcPlugin } from './definitions';

const Nfc = registerPlugin<NfcPlugin>('Nfc', {
  web: () => import('./web').then((m) => new m.NfcWeb()),
});

export * from './definitions';
export { Nfc };
