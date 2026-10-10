# OSTA 职业标准 PDF 下载指南

> **用途**：从 [OSTA 国家职业标准查询系统](https://www.osta.org.cn/skillStandard) 下载指定职业标准的 PDF 文件
> **最后更新**：2026-09-09（补充执行环境要点、验证步骤与故障排查）
> **状态**：✅ 已验证可用

---

## 快速使用

只需提供**职业名称**（如"重冶湿法冶炼工"），Agent 按以下流程自动完成下载：

```
1. 打开浏览器 → 访问 https://www.osta.org.cn/skillStandard
2. 在搜索框输入职业名称 → 点击"搜索"
3. 点击"点击查看" → 打开 PDF 查看器
4. 从 performance 资源列表中提取 PDF URL
5. 获取浏览器 cookies
6. 用 Node.js https 模块 + cookies 下载 PDF
7. 保存到 outputs/osta-standards/{职业名称}.pdf
```

---

## 详细步骤

### 第 1 步：初始化浏览器

```javascript
// 确保 browser 已初始化
if (globalThis.browser == null) {
  globalThis.browser = await agent.browsers.getForUrl("https://www.osta.org.cn/skillStandard");
}
var tab = await browser.tabs.new();
await tab.goto('https://www.osta.org.cn/skillStandard');
await tab.playwright.waitForTimeout(3000);
```

### 第 2 步：搜索职业标准

```javascript
// 方式一（推荐，2026-09-09 验证）：Playwright fill + click
var searchInput = tab.playwright.getByRole('textbox', { name: '输入标准名称或职业编码进行查询' });
await searchInput.fill('{职业名称}');
var searchBtn = tab.playwright.getByRole('button', { name: '搜索' });
await searchBtn.click();
await tab.playwright.waitForTimeout(3000);
```

```javascript
// 方式二（兜底）：通过 evaluate 直接操作 DOM
// ⚠️ 2026-09-09 验证发现此方式可能失效：设置 input.value 并派发 input 事件后，
// 前端框架（Arco/Vue）状态未同步，搜索返回 0 条。优先使用方式一。
await tab.evaluate(function(occupationName) {
  var input = document.querySelector('input[placeholder*="标准名称"]');
  if (input) {
    var nativeInputValueSetter = Object.getOwnPropertyDescriptor(
      window.HTMLInputElement.prototype, 'value'
    ).set;
    nativeInputValueSetter.call(input, occupationName);
    input.dispatchEvent(new Event('input', { bubbles: true }));
  }
  var buttons = document.querySelectorAll('button');
  for (var i = 0; i < buttons.length; i++) {
    if (buttons[i].textContent.trim() === '搜索') {
      buttons[i].click();
      break;
    }
  }
}, '{职业名称}');
```

### 第 3 步：打开 PDF 查看器

```javascript
// 搜索后应该只有一行结果
var link = tab.playwright.getByText('点击查看');
await link.click();
await tab.playwright.waitForTimeout(5000);  // 等待 PDF 加载
```

### 第 4 步：捕获 PDF 下载 URL

```javascript
// 清空之前的性能记录
await tab.evaluate(function() { performance.clearResourceTimings(); });

// 点击后重新加载 PDF（如果需要）
// 从 performance 资源列表中找到 PDF URL
var resources = await tab.evaluate(function() {
  return performance.getEntriesByType('resource').map(function(r) { return r.name; });
});

var pdfUrl = null;
for (var i = 0; i < resources.length; i++) {
  if (resources[i].indexOf('.pdf') !== -1) {
    pdfUrl = resources[i];
    break;
  }
}

// pdfUrl 示例：
// https://www.osta.org.cn/api/sys/downloadFile/decrypt?fileName=4eNsZIOv3WFNDfbnldJHeA%2F2024%2F4%2F29%2F140c0c12a4d047b6a49ceb5790668053.pdf
```

### 第 5 步：获取浏览器 Cookies

```javascript
var cookies = await tab.evaluate(function() {
  return document.cookie;
});
// 示例：_gscu_486005091=85679129a75zvh19; _gscbrs_486005091=1
```

### 第 6 步：下载 PDF 文件

```javascript
var https = await import('https');
var fs = await import('fs');

var downloadPromise = new Promise(function(resolve, reject) {
  var parsedUrl = new URL(pdfUrl);
  
  var options = {
    hostname: parsedUrl.hostname,
    port: 443,
    path: parsedUrl.pathname + parsedUrl.search,
    method: 'GET',
    headers: {
      'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
      'Accept': 'application/pdf,application/octet-stream,*/*',
      'Referer': 'https://www.osta.org.cn/skillStandard',
      'Cookie': cookies,
      'Accept-Encoding': 'gzip, deflate, br',
      'Accept-Language': 'zh-CN,zh;q=0.9,en;q=0.8',
      'Connection': 'keep-alive',
      'Sec-Fetch-Dest': 'document',
      'Sec-Fetch-Mode': 'navigate',
      'Sec-Fetch-Site': 'same-origin',
      'Upgrade-Insecure-Requests': '1',
    },
    rejectUnauthorized: false
  };
  
  var req = https.request(options, function(res) {
    // 处理重定向
    if (res.statusCode === 301 || res.statusCode === 302) {
      res.resume();
      resolve({ redirected: true, location: res.headers.location });
      return;
    }
    
    var chunks = [];
    res.on('data', function(chunk) { chunks.push(chunk); });
    res.on('end', function() {
      var buffer = Buffer.concat(chunks);
      resolve({ ok: true, size: buffer.length, header: buffer.slice(0, 4).toString(), buffer: buffer });
    });
  });
  
  req.on('error', function(e) {
    resolve({ ok: false, error: e.message });
  });
  
  req.setTimeout(15000, function() {
    req.destroy();
    resolve({ ok: false, error: 'timeout' });
  });
  
  req.end();
});

var result = await downloadPromise;

if (result.ok && result.header === '%PDF') {
  var outputPath = 'D:/workspace/qianlvguoyu_saas/outputs/osta-standards/{职业名称}.pdf';
  fs.writeFileSync(outputPath, result.buffer);
  console.log('下载成功: ' + outputPath + ' (' + result.size + ' bytes)');
}
```

### 第 7 步：验证下载结果（推荐，约 10 秒）

下载成功后不要直接交付，验证 PDF 有效且内容正确：

```bash
python -c "import pymupdf; d = pymupdf.open('outputs/osta-standards/{职业名称}.pdf'); print('pages:', d.page_count); print(d[0].get_text()[:200])"
```

确认两点：页数合理（职业标准一般 10~40 页）；首页标题包含目标职业名称与"国家职业技能标准"字样。验证通过后把记录追加到本文档末尾"已验证可下载的职业标准"清单。

⚠️ **注意"无文本层"PDF**（2026-09-09 矿井通风工案例）：部分标准（尤其是"试行"版）的文字是矢量轮廓而非文本，`get_text()` 返回空、看起来"像坏文件"，但渲染出来完全正常。验证时加一步渲染目检：

```bash
python -c "import pymupdf; d = pymupdf.open('outputs/osta-standards/{职业名称}.pdf'); d[0].get_pixmap(dpi=80).save('tmp_p1.png')"
```

`text_len=0` 但 drawings 较多（每页数十到数百个矢量图元）即属此类，渲染首页确认标题后照常交付。

---

## 关键要点

### 执行环境要点（2026-09-09 补充，ZCode browser-use skill）

1. **每个 `js` 调用都是全新内核**：`globalThis.browser` 等变量不跨调用保留。推荐把"搜索→打开查看器→捕获 URL→下载"整个流程放进**一个** `js` 调用执行；若必须拆成多次调用，每次都要重新 bootstrap + `agent.browsers.getForUrl(...)`。
2. **先复用已有标签页，避免堆积**：多次执行会在内置浏览器堆积同站点标签页。执行前先 `await browser.tabs.list()`，有 `osta.org.cn` 旧标签页就 `tabs.get(id)` 复用并 `goto()` 刷新，没有再 `tabs.new()`。
3. **Cookies 可能为空，属于正常情况**：2026-09-09 验证 `document.cookie` 返回空字符串，带空 `Cookie` 头发请求照样下载成功。不要因为 cookies 为空就重试。

### 为什么不能直接下载？

OSTA 网站的 PDF 查看器有以下保护：
- 查看器 CSS 类名 `no-download-print` 禁用了下载和打印按钮
- PDF URL 通过 JavaScript 动态生成，不在 DOM 中暴露
- 直接访问 PDF URL 会被重定向到网站首页（防盗链）

### 为什么需要 Cookies？

服务器通过 cookies 验证请求来源。没有正确的 cookies，请求会被重定向到首页。

### 为什么用 `performance.getEntriesByType`？

PDF 查看器使用 PDF.js 加载文件，加载后会在 performance 资源列表中留下记录。这是获取真实 PDF URL 的最可靠方式。

---

## 常见问题

### Q: Playwright `click()` 反复超时（"Timeout waiting for locator ... button"）怎么办？
2026-09-09 矿井通风工案例中，`fill` 正常但 `click` 连续超时。按以下顺序处理：

1. **超时的点击可能滞后执行**：报超时后先查页面实际状态（`tabs.list()` + `domSnapshot()`），不要立即重试同一 locator——滞后的点击可能已经把流程推进了几步（本次案例里弹窗都已打开）。
2. **换成组合操作**：输入用 Playwright `fill`（真实事件，框架状态同步），点击改用 evaluate DOM click：
   ```javascript
   await tab.evaluate(function() {
     var btns = document.querySelectorAll('button');
     for (var b of btns) {
       if (b.textContent.trim() === '搜索') { b.click(); return true; }
     }
     return false;
   });
   ```
   点击走 DOM 事件没有框架同步问题，此组合稳定可用。
3. **别被隐藏弹窗误导**：DOM 里残留的 `.arco-modal-mask` 若 `getComputedStyle(mask).display === 'none'`，是 PDF 查看器的隐藏残骸，不拦截指针事件，不是点击失败的原因。可无视或 `tab.reload()` 清掉。

### Q: 搜索返回"暂无数据"或 0 条怎么办？
先改用方式一（Playwright `fill` + `click`）重试——evaluate 方式因框架状态同步问题会假 0 条。仍无结果时：
1. 改用**职业编码精确搜索**（如 `6-16-01-13`），编码见结果表或人社部发文；
2. 缩短关键词重试（如"矿山生产集控员"→"集控员"）排除名称细微差异；
3. 确认该职业标准确已颁布（2022 版标准陆续上线，最新清单共 700+ 条）。

### Q: 搜索后有多行结果怎么办？
```javascript
// 遍历所有结果，找到匹配的职业名称
var rows = await tab.playwright.locator('.arco-table-tr').all();
for (var row of rows) {
  var text = await row.textContent();
  if (text.includes('{职业名称}')) {
    await row.locator('a.arco-link').click();
    break;
  }
}
```

### Q: PDF URL 过期了怎么办？
PDF URL 中的 `fileName` 参数是加密的，可能包含时间戳。如果下载失败，需要重新点击"点击查看"获取新的 URL。

### Q: 下载的文件不是 PDF？
检查 `result.header === '%PDF'`。如果不是，说明 URL 已过期或被重定向。

---

## 完整自动化脚本

以下是完整的可复用代码，只需替换 `{职业名称}` 即可：

```javascript
// === OSTA PDF 下载器 ===
// 用法：替换 OCCUPATION_NAME 后执行

var OCCUPATION_NAME = '{职业名称}';  // ← 替换这里
var OUTPUT_DIR = 'D:/workspace/qianlvguoyu_saas/outputs/osta-standards';

// 1. 初始化浏览器
if (globalThis.browser == null) {
  globalThis.browser = await agent.browsers.getForUrl("https://www.osta.org.cn/skillStandard");
}
var tab = await browser.tabs.new();
await tab.goto('https://www.osta.org.cn/skillStandard');
await tab.playwright.waitForTimeout(3000);

// 2. 搜索
await tab.evaluate(function(name) {
  var input = document.querySelector('input[placeholder*="标准名称"]');
  if (input) {
    var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;
    setter.call(input, name);
    input.dispatchEvent(new Event('input', { bubbles: true }));
  }
  var btns = document.querySelectorAll('button');
  for (var b of btns) {
    if (b.textContent.trim() === '搜索') { b.click(); break; }
  }
}, OCCUPATION_NAME);
await tab.playwright.waitForTimeout(3000);

// 3. 打开 PDF 查看器
await tab.evaluate(function() {
  var links = document.querySelectorAll('a.arco-link');
  for (var l of links) {
    if (l.textContent.trim() === '点击查看') { l.click(); break; }
  }
});
await tab.playwright.waitForTimeout(5000);

// 4. 获取 PDF URL
await tab.evaluate(function() { performance.clearResourceTimings(); });
// 重新点击以触发新的加载
await tab.evaluate(function() {
  var links = document.querySelectorAll('a.arco-link');
  for (var l of links) {
    if (l.textContent.trim() === '点击查看') { l.click(); break; }
  }
});
await tab.playwright.waitForTimeout(5000);

var resources = await tab.evaluate(function() {
  return performance.getEntriesByType('resource').map(function(r) { return r.name; });
});

var pdfUrl = null;
for (var r of resources) {
  if (r.indexOf('.pdf') !== -1) { pdfUrl = r; break; }
}

if (!pdfUrl) {
  nodeRepl.write('ERROR: 未找到 PDF URL');
} else {
  nodeRepl.write('PDF URL: ' + pdfUrl);
  
  // 5. 获取 cookies
  var cookies = await tab.evaluate(function() { return document.cookie; });
  
  // 6. 下载 PDF
  var https = await import('https');
  var fs = await import('fs');
  
  var result = await new Promise(function(resolve) {
    var parsedUrl = new URL(pdfUrl);
    var req = https.request({
      hostname: parsedUrl.hostname,
      port: 443,
      path: parsedUrl.pathname + parsedUrl.search,
      method: 'GET',
      headers: {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36',
        'Accept': 'application/pdf,application/octet-stream,*/*',
        'Referer': 'https://www.osta.org.cn/skillStandard',
        'Cookie': cookies,
        'Accept-Encoding': 'gzip, deflate, br',
      },
      rejectUnauthorized: false
    }, function(res) {
      if (res.statusCode === 301 || res.statusCode === 302) {
        res.resume();
        resolve({ redirected: true, location: res.headers.location });
        return;
      }
      var chunks = [];
      res.on('data', function(c) { chunks.push(c); });
      res.on('end', function() {
        var buf = Buffer.concat(chunks);
        resolve({ ok: true, size: buf.length, header: buf.slice(0, 4).toString(), buffer: buf });
      });
    });
    req.on('error', function(e) { resolve({ ok: false, error: e.message }); });
    req.setTimeout(15000, function() { req.destroy(); resolve({ ok: false, error: 'timeout' }); });
    req.end();
  });
  
  if (result.ok && result.header === '%PDF') {
    var outputPath = OUTPUT_DIR + '/' + OCCUPATION_NAME + '.pdf';
    fs.writeFileSync(outputPath, result.buffer);
    nodeRepl.write('SUCCESS: ' + outputPath + ' (' + result.size + ' bytes)');
  } else {
    nodeRepl.write('ERROR: ' + JSON.stringify(result));
  }
}
```

---

## 已验证可下载的职业标准

| # | 职业名称 | 职业编码 | 文件大小 | 下载时间 |
|---|---------|---------|---------|---------|
| 1 | 重冶湿法冶炼工 | 6-17-05-04 | 376 KB | 2026-08-02 |
| 2 | 铝电解工 | - | 937 KB | 2026-08-02 |
| 3 | 氧化铝制取工 | - | 977 KB | 2026-08-02 |
| 4 | 金属轧制工 | - | 362 KB | 2026-08-02 |
| 5 | 井下支护工 | - | 580 KB | 2026-08-02 |
| 6 | 矿山救护工 | - | 659 KB | 2026-08-02 |
| 7 | 重冶火法冶炼工 | - | 381 KB | 2026-08-02 |
| 8 | 金属挤压工 | - | 808 KB | 2026-08-02 |
| 9 | 矿山生产集控员 | 6-16-01-13 | 362 KB | 2026-09-09 |
| 10 | 重冶备料工 | - | 1.48 MB（11 页，页码含汇编页眉 85，属正常） | 2026-09-09 |
| 11 | 矿井通风工 | 6-16-01-09 | 3.16 MB（22 页，**无文本层**：文字为矢量轮廓，PyMuPDF 提取不到文字，需渲染成图片目检） | 2026-09-09 |

---

## 批量下载

当需要下载多个职业标准时，可以：

1. **逐个搜索 + 打开查看器** → 捕获所有 PDF URL（存储为 `globalThis.pdfUrl1` ~ `globalThis.pdfUrlN`）
2. **一次性获取 cookies**
3. **循环下载所有 PDF**

```javascript
var pdfList = [
  { name: '铝电解工', url: globalThis.pdfUrl1 },
  { name: '氧化铝制取工', url: globalThis.pdfUrl2 },
  // ... 更多职业
];

for (var item of pdfList) {
  // 使用上述下载代码
}
```

**注意**：每下载完一个 PDF 后，关闭查看器再搜索下一个职业。

---

## 注意事项

1. **URL 编码**：PDF URL 中的 `fileName` 参数是 URL 编码的，不需要解码
2. **Cookies 时效**：Cookies 可能过期，如果下载失败尝试刷新页面重新获取
3. **网络超时**：设置了 15 秒超时，如果网络慢可以调整
4. **输出目录**：确保 `outputs/osta-standards/` 目录存在
