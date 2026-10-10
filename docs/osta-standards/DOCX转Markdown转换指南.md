# DOCX 转 Markdown（带完整目录）转换指南

> **用途**：将 Word (.docx) 文件转换为 Markdown (.md) 格式，并自动生成带锚点链接的完整目录
> **适用对象**：任何 AI 模型或开发者，按本文档步骤即可完成转换
> **验证时间**：2026-08-02
> **验证样例**：重冶湿法冶炼工国家职业技能标准.docx（36 个标题 + 完整目录）

---

## 一、快速上手（1 条命令）

### 方法 A：使用现成脚本（推荐）

脚本文件：`docx2md.py`（与本文档同目录，纯 Python 标准库实现，无需安装任何依赖）

```bash
# 基本用法
python docx2md.py 输入.docx

# 指定输出文件
python docx2md.py 输入.docx 输出.md
```

### 方法 B：手动分步操作（无脚本时）

```bash
# 1. DOCX 本质是 ZIP 文件，直接解压
unzip 输入.docx -d docx_extract
# Windows PowerShell 需要先重命名（Expand-Archive 不支持 .docx 后缀）
# copy 输入.docx 输入.zip && powershell Expand-Archive 输入.zip -DestinationPath docx_extract

# 2. 正文内容在 word/document.xml
# 3. 按本文档第三节的规则解析 XML 并生成 Markdown
```

---

## 二、实现原理

```
输入.docx (ZIP)
   └── word/document.xml  ← 正文内容
         ├── <w:p>   段落
         ├── <w:tbl> 表格
         └── <w:t>   文本片段（run）
              ↓ 解析
Markdown 输出
   ├── # / ## / ### 标题
   ├── | 表格 |
   └── ## 目录（带锚点链接）
```

---

## 三、核心实现步骤

### 第 1 步：读取并解析 DOCX

```python
import zipfile
import xml.etree.ElementTree as ET

ns = 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'

with zipfile.ZipFile('输入.docx') as zf:
    with zf.open('word/document.xml') as f:
        doc_xml = f.read().decode('utf-8')

root = ET.fromstring(doc_xml)
body = root.find(f'{{{ns}}}body')

# 按文档顺序遍历段落和表格
for child in body:
    tag = child.tag.split('}')[-1]  # 去掉命名空间前缀
    if tag == 'p':   # 段落
        pass
    elif tag == 'tbl':  # 表格
        pass
```

### 第 2 步：提取段落文本

```python
def extract_para_text(para_elem):
    texts = []
    for t in para_elem.iter(f'{{{ns}}}t'):
        if t.text:
            texts.append(t.text)
    return ''.join(texts)
```

### 第 3 步：表格转 Markdown

```python
def extract_table_md(tbl_elem):
    rows = []
    for tr in tbl_elem.iter(f'{{{ns}}}tr'):       # 遍历行
        cells = []
        for tc in tr.iter(f'{{{ns}}}tc'):         # 遍历单元格
            cell_texts = [extract_para_text(p) for p in tc.iter(f'{{{ns}}}p')]
            cells.append(' '.join(cell_texts).strip())
        rows.append(cells)

    if not rows:
        return ''
    md = '| ' + ' | '.join(rows[0]) + ' |\n'       # 表头
    md += '| ' + ' | '.join(['---'] * len(rows[0])) + ' |\n'  # 分隔线
    for row in rows[1:]:
        md += '| ' + ' | '.join(row) + ' |\n'      # 数据行
    return md
```

### 第 4 步：标题层级检测（关键！）

**规则**：根据段落开头的编号格式判断标题级别

| 编号格式 | 示例 | 标题级别 |
|---------|------|---------|
| `X.` + 中文 | `1. 职业概况` | H1 (`#`) |
| `X. Y` + 中文 | `1. 1 职业名称` | H2 (`##`) |
| `X. Y. Z` + 中文 | `1. 9. 1 申报条件` | H3 (`###`) |

```python
import re

def detect_heading(text):
    text = text.strip()
    if not text:
        return 0
    # 排除明显不是标题的段落
    if text.startswith('（') or text.startswith('('):
        return 0
    if text == '续表':      # 表格续页标记，不是标题
        return 0
    if text in ('∗', '*', '注:'):
        return 0
    if re.sub(r'\s+', '', text) == '说明':
        return 1

    # 关键：删除所有空格后匹配数字编号
    # 编号后必须跟中文字符，避免误判 "6-17-05-04" 这类内容
    cleaned = re.sub(r'\s+', '', text)
    m = re.match(r'^(\d+)(?:\.(\d+))?(?:\.(\d+))?\.?([\u4e00-\u9fff].*)$', cleaned)
    if m:
        count = sum(1 for g in m.groups()[:3] if g)
        return min(count, 3)
    return 0
```

### 第 5 步：生成目录（TOC）

```python
# 在遍历时收集标题
toc = []  # [(level, title), ...]

# 遍历结束后生成目录
toc_md = '## 目录\n\n'
for level, title in toc:
    indent = '  ' * (level - 1)   # 层级缩进
    # 生成锚点：去除空格和标点
    anchor = re.sub(r'[\s.]+', '-', title)
    anchor = re.sub(r'[^\w\u4e00-\u9fff-]', '', anchor).rstrip('-')
    toc_md += f'{indent}- [{title}](#{anchor})\n'

# 插入目录（放在正文第一个章节之前）
insert_pos = markdown.find('# 说明')
if insert_pos > 0:
    markdown = markdown[:insert_pos] + toc_md + '\n' + markdown[insert_pos:]
```

---

## 四、必须避开的坑（踩坑记录）

### 坑 1：PowerShell 无法直接解压 .docx ⚠️
`Expand-Archive` 只支持 `.zip` 后缀，直接对 `.docx` 操作会报错：
```
Expand-Archive : .docx 不受支持的存档文件格式，只有 .zip 受支持
```
**解决**：先重命名为 `.zip` 再解压，或用 `zipfile` 库直接处理。

### 坑 2：清理空格时 `\s+` 会吃掉换行符 ⚠️⚠️⚠️（最坑）
```python
# ❌ 错误：\s 包含换行符，会把标题和正文合并成一行！
markdown = re.sub(r'([\u4e00-\u9fff])\s+([\u4e00-\u9fff])', r'\1\2', markdown)
# "## 1. 1 职业名称\n\n重冶湿法冶炼工①" 会变成 "## 1. 1 职业名称重冶湿法冶炼工①"

# ✅ 正确：只处理空格和 Tab，不碰换行符
markdown = re.sub(r'([\u4e00-\u9fff])[ \t]+([\u4e00-\u9fff])', r'\1\2', markdown)
```

### 坑 3：`6-17-05-04` 之类的职业编码会被误判为标题
```python
# ❌ 错误：\D 匹配了 "-"，导致 "6-17-05-04" 被识别为 H1
m = re.match(r'^(\d+)(?:\.(\d+))?(?:\.(\d+))?(\D.+)$', cleaned)

# ✅ 正确：编号后必须跟中文字符
m = re.match(r'^(\d+)(?:\.(\d+))?(?:\.(\d+))?\.?([\u4e00-\u9fff].*)$', cleaned)
```

### 坑 4：标题词带全角空格（如 `说    明`）
```python
# 直接用 text == '说明' 判断会失败，必须先去除空格
if re.sub(r'\s+', '', text) == '说明':
    return 1
```

### 坑 5：表格内的段落不要当作标题
表格内的 `<w:p>` 如果编号以数字开头（如 `5.1.1`），会干扰标题检测。
**解决**：遍历时优先处理 `<w:tbl>`，表格内的内容走表格转换逻辑，不进入标题检测。

### 坑 6：DOCX 可能用自定义样式名（如 "Body Text"）代替 Heading 样式
不要依赖 `w:pStyle` 判断标题。本方案用**编号格式**判断，不依赖样式名，更通用。

---

## 五、完整可运行脚本

```python
"""DOCX 转 Markdown（带完整目录）转换脚本 - 纯标准库实现"""
import sys, zipfile, re, os
import xml.etree.ElementTree as ET

NS = 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'


def extract_para_text(para_elem):
    return ''.join(t.text for t in para_elem.iter(f'{{{NS}}}t') if t.text)


def extract_table_md(tbl_elem):
    rows = []
    for tr in tbl_elem.iter(f'{{{NS}}}tr'):
        cells = []
        for tc in tr.iter(f'{{{NS}}}tc'):
            text = ' '.join(extract_para_text(p).strip() for p in tc.iter(f'{{{NS}}}p'))
            cells.append(text)
        rows.append(cells)
    if not rows:
        return ''
    md = '| ' + ' | '.join(rows[0]) + ' |\n'
    md += '| ' + ' | '.join(['---'] * len(rows[0])) + ' |\n'
    for row in rows[1:]:
        md += '| ' + ' | '.join(row) + ' |\n'
    return md


def detect_heading(text):
    text = text.strip()
    if not text:
        return 0
    if text.startswith('（') or text.startswith('('):
        return 0
    if text == '续表' or text in ('∗', '*', '注:'):
        return 0
    if re.sub(r'\s+', '', text) == '说明':
        return 1
    cleaned = re.sub(r'\s+', '', text)
    m = re.match(r'^(\d+)(?:\.(\d+))?(?:\.(\d+))?\.?([\u4e00-\u9fff].*)$', cleaned)
    if m:
        count = sum(1 for g in m.groups()[:3] if g)
        return min(count, 3)
    return 0


def clean_spaces(markdown):
    # 只处理空格/Tab，绝不处理换行符
    markdown = re.sub(r'([\u4e00-\u9fff])[ \t]+([\u4e00-\u9fff])', r'\1\2', markdown)
    markdown = re.sub(r'([\u4e00-\u9fff])[ \t]+([，。、；：！？,.;:!?])', r'\1\2', markdown)
    markdown = re.sub(r'([，。、；：！？,.;:!?])[ \t]+([\u4e00-\u9fff])', r'\1\2', markdown)
    markdown = re.sub(r'([（(])[ \t]+', r'\1', markdown)
    markdown = re.sub(r'[ \t]+([）)])', r'\1', markdown)
    return markdown


def docx_to_markdown(docx_path, md_path=None):
    with zipfile.ZipFile(docx_path) as zf:
        doc_xml = zf.read('word/document.xml').decode('utf-8')

    root = ET.fromstring(doc_xml)
    body = root.find(f'{{{NS}}}body')

    markdown = ''
    toc = []
    in_title_page = True

    for child in body:
        tag = child.tag.split('}')[-1]

        if tag == 'tbl':
            markdown += extract_table_md(child) + '\n'
            continue
        if tag != 'p':
            continue

        text = extract_para_text(child).strip()
        if not text:
            continue

        if in_title_page:
            if re.sub(r'\s+', '', text) == '说明':
                in_title_page = False
                markdown += '# 说明\n\n'
                toc.append((1, '说明'))
                continue
            if '国家职业技能标准' in text:
                markdown += '# ' + text + '\n\n'
            else:
                markdown += text + '\n\n'
            continue

        h_level = detect_heading(text)
        if h_level > 0:
            markdown += '#' * h_level + ' ' + text + '\n\n'
            toc.append((h_level, text))
        else:
            markdown += text + '\n\n'

    markdown = clean_spaces(markdown)

    toc_md = '## 目录\n\n'
    for level, title in toc:
        indent = '  ' * (level - 1)
        anchor = re.sub(r'[^\w\u4e00-\u9fff-]', '', re.sub(r'[\s.]+', '-', title)).rstrip('-')
        toc_md += f'{indent}- [{title}](#{anchor})\n'

    insert_pos = markdown.find('# 说明')
    if insert_pos > 0:
        markdown = markdown[:insert_pos] + toc_md + '\n' + markdown[insert_pos:]

    if md_path is None:
        md_path = os.path.splitext(docx_path)[0] + '.md'
    with open(md_path, 'w', encoding='utf-8') as f:
        f.write(markdown)

    print(f'转换完成: {md_path}')
    print(f'标题数量: {len(toc)}')
    return md_path


if __name__ == '__main__':
    if len(sys.argv) < 2:
        print('用法: python docx2md.py 输入.docx [输出.md]')
        sys.exit(1)
    docx_to_markdown(sys.argv[1], sys.argv[2] if len(sys.argv) > 2 else None)
```

---

## 六、验证清单（转换完成后检查）

| # | 检查项 | 通过标准 |
|---|--------|---------|
| 1 | 目录存在 | 文件开头有 `## 目录` 且包含所有章节 |
| 2 | 目录锚点可点击 | 目录链接的 `#锚点` 与正文标题一致 |
| 3 | 标题层级正确 | H1=主章节，H2=子章节，H3=细分子章节 |
| 4 | 标题与正文分离 | 标题行只有标题文本，正文在下一行 |
| 5 | 表格完整 | 所有 `w:tbl` 都转成了 Markdown 表格 |
| 6 | 无多余空格 | 中文字符之间无空格，中文标点前无空格 |
| 7 | 无乱码 | 中文正常显示（UTF-8 编码） |
| 8 | 特殊段落排除 | `续表`、`∗`、`注:` 不是标题 |
| 9 | 职业编码不误判 | 如 `6-17-05-04` 是正文不是标题 |

---

## 七、常见问题

### Q1: 没有 Python 环境怎么办？
用任何支持 XML 的语言实现即可（Node.js、Java、Go...）。核心逻辑只有三步：
1. 解压 ZIP 读 `word/document.xml`
2. 按 `w:p` / `w:tbl` 遍历
3. 用编号正则判断标题

### Q2: 文档没有数字编号标题怎么办？
退化策略：检查 `w:pStyle` 是否为 `Heading1`~`Heading9`，或检查字号（`w:sz`，如 28+ = 大标题）。本方案针对"职业标准"类文档优化，编号规则已覆盖。

### Q3: 转换后中文间有空格？
DOCX 中文本被拆成多个 run，每个 run 带 `xml:space="preserve"` 保留空格。用 `clean_spaces()` 处理即可（注意坑 2：别用 `\s`）。

### Q4: 表格跨页有"续表"行？
`续表` 是表格续页的单独段落，已自动排除为标题。如需要可手动删除。

---

## 八、已验证样例

| 输入 | 输出 | 标题数 | 备注 |
|------|------|--------|------|
| 重冶湿法冶炼工.docx (186 KB) | 重冶湿法冶炼工_docx.md (41 KB) | 36 | 5 个 H1 + 22 个 H2 + 9 个 H3 |
