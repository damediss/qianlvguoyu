// 本地回归专用：系统 Chrome（无内置 chromium 环境时使用），用完即删
import { defineConfig } from '@playwright/test';
import base from './playwright.config';

export default defineConfig({
  ...base,
  use: {
    ...base.use,
    channel: 'chrome',
    video: 'off',
  },
});
