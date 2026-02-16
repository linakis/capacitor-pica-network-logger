import { defineConfig } from 'vite';

export default defineConfig({
  resolve: {
    alias: {
      '@capacitor/http': 'capacitor-pica-network-logger'
    }
  }
});
