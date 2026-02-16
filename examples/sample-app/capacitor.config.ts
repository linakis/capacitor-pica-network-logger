import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'com.linakis.caphttpinspector.sample',
  appName: 'CapHttpInspectorSample',
  webDir: 'dist',
  plugins: {
    PicaNetworkLogger: {
      enabled: true,
      notify: true,
      maxBodySize: 131072,
      redactHeaders: ['authorization', 'cookie'],
      redactJsonFields: ['password', 'token']
    }
  }
};

export default config;
