import { defineConfig } from '@playwright/test';
export default defineConfig({testDir:'./tests',fullyParallel:false,workers:1,reporter:'list',use:{baseURL:process.env.E2E_BASE_URL||'http://localhost:5173',browserName:'chromium',launchOptions:process.env.E2E_CHROME_PATH?{executablePath:process.env.E2E_CHROME_PATH}:undefined,trace:'retain-on-failure'}});
