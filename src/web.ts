import type { PicaNetworkLoggerPlugin } from './index';

export class PicaNetworkLoggerWeb implements PicaNetworkLoggerPlugin {
  async startRequest(options: any): Promise<{ id: string }> {
    return { id: options?.id ?? '' };
  }

  async finishRequest(): Promise<void> {
    return;
  }

  async getLogs(): Promise<{ logs: Record<string, unknown>[] }> {
    return { logs: [] };
  }

  async getLog(): Promise<{ log?: Record<string, unknown> }> {
    return { log: undefined };
  }

  async clearLogs(): Promise<void> {
    return;
  }

  async getConfig(): Promise<any> {
    return {};
  }

  async openInspector(): Promise<void> {
    return;
  }

  async showNotification(): Promise<void> {
    return;
  }

  async requestNotificationPermission(): Promise<{ granted?: boolean }> {
    return { granted: false };
  }
}
