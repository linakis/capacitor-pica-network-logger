import { Capacitor, registerPlugin } from '@capacitor/core';
import type { FinishRequestOptions, StartRequestOptions } from './types';

export interface PicaNetworkLoggerPlugin {
  startRequest(options: StartRequestOptions): Promise<{ id: string }>;
  finishRequest(options: FinishRequestOptions): Promise<void>;
  openInspector(): Promise<void>;
}

const createNoop = (): PicaNetworkLoggerPlugin => ({
  startRequest: async () => ({ id: '' }),
  finishRequest: async () => undefined,
  openInspector: async () => undefined
});

export const PicaNetworkLogger = Capacitor.isNativePlatform()
  ? registerPlugin<PicaNetworkLoggerPlugin>('PicaNetworkLogger')
  : createNoop();

export type { FinishRequestOptions, StartRequestOptions } from './types';
