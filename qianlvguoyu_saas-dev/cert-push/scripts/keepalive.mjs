const { spawn } = require('child_process');
const fs = require('fs');
const p = spawn('cmd.exe', ['/c', 'D:\workspace\qianlvguoyu_saas\scripts\run-backend-visible.cmd'], {
  env: {
    ...process.env,
    SERVER_PORT: '8080',
    MYSQL_URL: 'jdbc:mysql://localhost:3308/gjjd_test?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai',
    MYSQL_USERNAME: 'root',
    MYSQL_PASSWORD: 'guoyu_dev!513',
    JWT_SECRET: 'dev-only-change-me-dev-only-change-me'
  },
  detached: false,
  stdio: ['ignore', fs.openSync('D:\\workspace\\qianlvguoyu_saas\\outputs\\logs\\platform-api-node3.log', 'w'), fs.openSync('D:\\workspace\\qianlvguoyu_saas\\outputs\\logs\\platform-api-node3.log', 'w')]
});
console.log('PID: ' + p.pid);
setInterval(() => { try { fs.writeFileSync('D:\\workspace\\qianlvguoyu_saas\\outputs\\logs\\keepalive.txt', String(Date.now())); } catch(e) {} }, 10000);
