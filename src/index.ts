import { registerPlugin } from '@capacitor/core';
import type { LoggerConfig, RequestMeta, ResponseMeta } from './types';
import { PicaNetworkLoggerWeb } from './web';

export interface PicaNetworkLoggerPlugin {
  startRequest(options: RequestMeta): Promise<{ id: string }>;
  finishRequest(options: ResponseMeta): Promise<void>;
  getLogs(options?: { limit?: number; offset?: number }): Promise<{ logs: Record<string, unknown>[] }>; 
  getLog(options: { id: string }): Promise<{ log?: Record<string, unknown> }>;
  clearLogs(): Promise<void>;
  getConfig(): Promise<LoggerConfig>;
  openInspector(): Promise<void>;
  showNotification(): Promise<void>;
  requestNotificationPermission(): Promise<{ granted?: boolean } | void>;
}

export const PicaNetworkLogger = registerPlugin<PicaNetworkLoggerPlugin>('PicaNetworkLogger', {
  web: () => new PicaNetworkLoggerWeb()
});

export * from './httpWithLogging';
export * from './types';
