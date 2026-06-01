import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000,
    proxy: {
      // System 服务
      '/api/v1/system': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      // Workflow 服务
      '/api/v1/workflow': {
        target: 'http://localhost:8082',
        changeOrigin: true
      },
      // HR 服务
      '/api/v1/hr': {
        target: 'http://localhost:8085',
        changeOrigin: true
      },
      // 登录接口
      '/api/v1/auth': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  },
  build: {
    // 启用代码分割
    rollupOptions: {
      output: {
        manualChunks: {
          // 第三方库按需分割
          'vue-vendor': ['vue', 'vue-router', 'pinia'],
          'element-plus': ['element-plus'],
          'axios': ['axios']
        }
      }
    },
    // 启用 gzip 压缩
    chunkSizeWarningLimit: 500,
    // 生产环境禁用 source-map
    sourcemap: false
  }
})