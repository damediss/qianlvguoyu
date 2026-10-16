# -*- coding: utf-8 -*-
"""
部门制度 markdown -> 公文格式 docx 转换器（千铝工投云平台与智能体开发部）
公文格式：标题黑体，正文仿宋_GB2312 三号，行距固定28磅，A4 版心。
用法：
  python gen_dept_regulation_docx.py <input.md> <output.docx>
  python gen_dept_regulation_docx.py --dir <indir> <outdir>
"""
import os
import re
import sys

from docx import Document
from docx.shared import Pt, Cm
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_LINE_SPACING
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml.ns import qn

FONT_TITLE = "黑体"
FONT_HEI = "黑体"
FONT_FANG = "仿宋"
FONT_KAI = "楷体"


def set_cjk(run, font):
    run.font.name = font
    rpr = run._element.get_or_add_rPr()
    rfonts = rpr.get_or_add_rFonts()
    rfonts.set(qn("w:eastAsia"), font)
    rfonts.set(qn("w:ascii"), font)
    rfonts.set(qn("w:hAnsi"), font)


def add_runs_with_bold(par, text):
    parts = re.split(r"(\*\*.*?\*\*)", text)
    for p in parts:
        if not p:
            continue
        if p.startswith("**") and p.endswith("**"):
            r = par.add_run(p[2:-2])
            set_cjk(r, FONT_HEI)
            r.bold = True
        else:
            r = par.add_run(p)
            set_cjk(r, FONT_FANG)


def style_doc(doc):
    sec = doc.sections[0]
    sec.page_height = Cm(29.7)
    sec.page_width = Cm(21.0)
    sec.top_margin = Cm(3.7)
    sec.bottom_margin = Cm(3.5)
    sec.left_margin = Cm(2.8)
    sec.right_margin = Cm(2.6)
    st = doc.styles["Normal"]
    st.font.name = FONT_FANG
    st.font.size = Pt(16)
    rpr = st.element.get_or_add_rPr()
    rfonts = rpr.get_or_add_rFonts()
    rfonts.set(qn("w:eastAsia"), FONT_FANG)


def make_title(doc, text):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.line_spacing_rule = WD_LINE_SPACING.EXACTLY
    p.paragraph_format.line_spacing = Pt(28)
    p.paragraph_format.space_after = Pt(6)
    r = p.add_run(text)
    set_cjk(r, FONT_TITLE)
    r.bold = True
    r.font.size = Pt(22)


def make_meta(doc, text):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.line_spacing_rule = WD_LINE_SPACING.EXACTLY
    p.paragraph_format.line_spacing = Pt(28)
    p.paragraph_format.space_after = Pt(12)
    r = p.add_run(text)
    set_cjk(r, FONT_FANG)
    r.font.size = Pt(15)


def make_chapter(doc, text):
    p = doc.add_paragraph()
    p.paragraph_format.line_spacing_rule = WD_LINE_SPACING.EXACTLY
    p.paragraph_format.line_spacing = Pt(28)
    p.paragraph_format.space_before = Pt(10)
    p.paragraph_format.space_after = Pt(4)
    r = p.add_run(text)
    set_cjk(r, FONT_HEI)
    r.bold = True
    r.font.size = Pt(16)


def make_article(doc, raw):
    p = doc.add_paragraph()
    p.paragraph_format.line_spacing_rule = WD_LINE_SPACING.EXACTLY
    p.paragraph_format.line_spacing = Pt(28)
    p.paragraph_format.first_line_indent = Pt(32)
    p.paragraph_format.space_after = Pt(2)
    text = raw
    m = re.match(r"^\*\*(第.+?条)\*\*\s*(.*)$", text)
    if m:
        r1 = p.add_run(m.group(1))
        set_cjk(r1, FONT_HEI)
        r1.bold = True
        r1.font.size = Pt(16)
        add_runs_with_bold(p, m.group(2))
    else:
        add_runs_with_bold(p, text)
    for r in p.runs:
        if r.font.size is None:
            r.font.size = Pt(16)
    return p


def make_plain(doc, text, kai=False):
    p = doc.add_paragraph()
    p.paragraph_format.line_spacing_rule = WD_LINE_SPACING.EXACTLY
    p.paragraph_format.line_spacing = Pt(28)
    p.paragraph_format.space_after = Pt(2)
    r = p.add_run(text)
    set_cjk(r, FONT_KAI if kai else FONT_FANG)
    r.font.size = Pt(16)
    return p


def make_table(doc, rows):
    ncol = max(len(r) for r in rows)
    tbl = doc.add_table(rows=0, cols=ncol)
    tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    tbl.style = "Table Grid"
    for i, row in enumerate(rows):
        cells = tbl.add_row().cells
        for j in range(ncol):
            val = row[j] if j < len(row) else ""
            cell = cells[j]
            cell.text = ""
            p = cell.paragraphs[0]
            p.paragraph_format.line_spacing_rule = WD_LINE_SPACING.EXACTLY
            p.paragraph_format.line_spacing = Pt(20)
            r = p.add_run(val)
            set_cjk(r, FONT_HEI if i == 0 else FONT_FANG)
            r.bold = (i == 0)
            r.font.size = Pt(12)
    sp = doc.add_paragraph()
    sp.paragraph_format.line_spacing = Pt(28)
    sp.paragraph_format.space_after = Pt(2)
    return tbl


def convert(md_text):
    doc = Document()
    style_doc(doc)
    lines = md_text.splitlines()
    i = 0
    title_done = False
    meta_done = False
    rows_buffer = []
    in_table = False

    def flush_table():
        nonlocal rows_buffer, in_table
        if rows_buffer:
            data = [r for r in rows_buffer if not re.match(r"^\s*\|[\s\-\|:]+\|\s*$", r)]
            parsed = []
            for r in data:
                core = r.strip()
                if core.startswith("|"):
                    core = core[1:]
                if core.endswith("|"):
                    core = core[:-1]
                parsed.append([c.strip() for c in core.split("|")])
            make_table(doc, parsed)
        rows_buffer = []
        in_table = False

    while i < len(lines):
        line = lines[i]
        stripped = line.strip()

        if stripped.startswith("|"):
            rows_buffer.append(stripped)
            in_table = True
            i += 1
            continue
        if in_table:
            flush_table()

        if stripped == "":
            i += 1
            continue

        if stripped.startswith("# "):
            if not title_done:
                make_title(doc, stripped[2:].strip())
                title_done = True
            else:
                make_chapter(doc, stripped[2:].strip())
            i += 1
            continue

        if title_done and not meta_done and not stripped.startswith("## ") \
                and not stripped.startswith(">") and ("制度编号" in stripped or i == 1):
            make_meta(doc, stripped)
            meta_done = True
            i += 1
            continue

        if stripped.startswith("## "):
            make_chapter(doc, stripped[3:].strip())
            i += 1
            continue

        if stripped.startswith(">"):
            make_plain(doc, stripped[1:].strip())
            i += 1
            continue

        if stripped.startswith("修订记录") or stripped.startswith("千铝工投") \
                or "公司主要负责人（签发）" in stripped or "公章" in stripped \
                or re.match(r"^2026年", stripped) or "____" in stripped:
            make_plain(doc, stripped, kai=True)
            i += 1
            continue

        make_article(doc, stripped)
        i += 1

    flush_table()
    return doc


def main():
    args = sys.argv[1:]
    if "--dir" in args:
        idx = args.index("--dir")
        indir = args[idx + 1]
        outdir = args[idx + 2]
        os.makedirs(outdir, exist_ok=True)
        for fn in sorted(os.listdir(indir)):
            if fn.endswith(".md"):
                with open(os.path.join(indir, fn), encoding="utf-8") as f:
                    txt = f.read()
                doc = convert(txt)
                out = os.path.join(outdir, fn[:-3] + ".docx")
                doc.save(out)
                print("OK", out)
    else:
        inp, outp = args[0], args[1]
        with open(inp, encoding="utf-8") as f:
            txt = f.read()
        doc = convert(txt)
        doc.save(outp)
        print("OK", outp)


if __name__ == "__main__":
    main()
