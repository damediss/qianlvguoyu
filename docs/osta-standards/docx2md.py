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
