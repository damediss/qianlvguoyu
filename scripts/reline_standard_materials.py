#!/usr/bin/env python3
"""细目表 staging 节点「相关知识/技能要求」逐行重排存量数据整理。

背景：早期解析器把 docx 单元格多段落直接拼接成单行（条目靠编号正则切分），
导致页面与导出 Excel 中每个知识小点没有换行。新数据已在解析时逐行存储，
本脚本把 exam_standard_node 中仍是单行的 related_knowledge / skill_requirement
按编号锚点重拆为逐行（与服务端 DocxTextUtil.relineItems 同规则）。

用法：
  set MYSQL_HOST=43.138.163.7 & set MYSQL_PORT=3308 & set MYSQL_USERNAME=gjjd & set MYSQL_PASSWORD=***
  python scripts/reline_standard_materials.py            # 实际执行
  python scripts/reline_standard_materials.py --dry-run  # 只统计不写入
"""
import os
import re
import sys

import pymysql

# 与 DocxTextUtil 相同的锚点优先级：点号编号 > （1） > 1） > 1、
DOTTED = re.compile(r"(\d{1,2}(?:\s*\.\s*\d{1,2}){1,2})\s*\.?\s*")
PAREN = re.compile(r"[（(]\s*(\d{1,2})\s*[)）]\s*")
CLOSE_PAREN = re.compile(r"(?<![\d.])\s*(\d{1,2})[)）]\s*")
DUNHAO = re.compile(r"(?<!\d)(\d{1,2})、")


def reline(text):
    """单行串联文本按编号条目重拆为逐行；已含换行或无锚点的文本原样返回。"""
    if not text or not text.strip():
        return text
    if "\n" in text:
        return text
    flat = text.replace("\r", " ").strip()
    anchors = None
    for pat in (DOTTED, PAREN, CLOSE_PAREN, DUNHAO):
        found = [(m.start(), m.end()) for m in pat.finditer(flat)]
        if found:
            anchors = found
            break
    if not anchors:
        return text
    lines = []
    lead = flat[: anchors[0][0]].strip()
    if lead:
        lines.append(lead)
    for i, (start, _end) in enumerate(anchors):
        end = anchors[i + 1][0] if i + 1 < len(anchors) else len(flat)
        line = flat[start:end].strip()
        if line:
            lines.append(line)
    return "\n".join(lines)


def main():
    dry_run = "--dry-run" in sys.argv
    conn = pymysql.connect(
        host=os.environ.get("MYSQL_HOST", "127.0.0.1"),
        port=int(os.environ.get("MYSQL_PORT", "3306")),
        user=os.environ.get("MYSQL_USERNAME", "root"),
        password=os.environ["MYSQL_PASSWORD"],
        database=os.environ.get("MYSQL_DATABASE", "gjjddb2026"),
        charset="utf8mb4",
    )
    scanned = changed = 0
    with conn.cursor() as cur:
        cur.execute(
            "SELECT id, related_knowledge, skill_requirement FROM exam_standard_node "
            "WHERE deleted = 0 AND node_level = 2"
        )
        rows = cur.fetchall()
        updates = []
        for node_id, knowledge, skill in rows:
            scanned += 1
            new_knowledge = reline(knowledge) if knowledge else knowledge
            new_skill = reline(skill) if skill else skill
            if new_knowledge != knowledge or new_skill != skill:
                changed += 1
                updates.append((new_knowledge, new_skill, node_id))
        print(f"扫描 L2 节点 {scanned} 个，需要重排 {changed} 个")
        if dry_run or not updates:
            return
        with conn.cursor() as wcur:
            wcur.executemany(
                "UPDATE exam_standard_node SET related_knowledge = %s, skill_requirement = %s "
                "WHERE id = %s",
                updates,
            )
        conn.commit()
        print(f"已更新 {len(updates)} 行")


if __name__ == "__main__":
    main()
