const http = require('http');
const fs = require('fs');
const path = require('path');

const BACKEND = 'http://localhost:8080';
const PORT = 5174;
const HTML = fs.readFileSync('D:/workspace/qianlvguoyu_saas/outputs/login-test.html','utf8');

const server = http.createServer((req, res) => {
  if (req.url === '/' || req.url === '/index.html') {
    res.writeHead(200, {'Content-Type':'text/html;charset=utf-8'});
    res.end(HTML);
    return;
  }
  if (req.url.startsWith('/api/')) {
    const opts = new URL(BACKEND + req.url);
    opts.method = req.method;
    opts.headers = {...req.headers, host: opts.hostname};
    const proxy = http.request(opts, (pr) => {
      res.writeHead(pr.statusCode, pr.headers);
      pr.pipe(res);
    });
    proxy.on('error', (e) => { res.writeHead(502); res.end(JSON.stringify({error:e.message})); });
    req.pipe(proxy);
    return;
  }
  res.writeHead(404);
  res.end('Not Found');
});

server.listen(PORT, '0.0.0.0', () => {
  console.log('Dev server on http://localhost:' + PORT);
});
