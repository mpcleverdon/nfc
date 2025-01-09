import { registerPlugin } from '@capacitor/core';

import type { NFCPlugin } from './definitions';

export * from './definitions';
export * from './web';

const Nfc = registerPlugin<NFCPlugin>('Nfc', {
  web: () => import('./web').then(m => new m.NFCWeb()),
});

export { Nfc };
