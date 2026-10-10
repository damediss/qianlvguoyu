# -*- coding: utf-8 -*-
"""Fix certificateNo placeholder and verify checkboxes in claim template."""
from __future__ import annotations

import re
import zipfile
from io import BytesIO
from pathlib import Path

SRC = Path(
    r"D:/phpApp/qianlvguoyu_saas/services/platform-api/src/main/resources/templates/subsidy/personal_skill_upgrade_claim_2025.docx"
)
CERT_NO = "S000045132003263000046"


def replace_split_text(xml: str, plain: str, placeholder: str) -> str:
    """Replace plain text that may be split by XML tags with a contiguous placeholder."""
    if plain in xml:
        return xml.replace(plain, placeholder, 1)
    # Build pattern that allows tags between each char
    parts = []
    for i, ch in enumerate(plain):
        if i > 0:
            parts.append(r"(?:<[^>]+>)*")
        parts.append(re.escape(ch))
    pat = re.compile("".join(parts))
    m = pat.search(xml)
    if not m:
        raise SystemExit(f"cannot find split text: {plain}")
    return xml[: m.start()] + placeholder + xml[m.end() :]


def main() -> None:
    z = zipfile.ZipFile(SRC)
    xml = z.read("word/document.xml").decode("utf-8")
    if "${certificateNo}" not in xml:
        xml = replace_split_text(xml, CERT_NO, "${certificateNo}")

    # Force required checkbox states (sample may already match)
    # Use plain-text-aware approach for ☑/□ near labels if needed
    text = re.sub(r"<[^>]+>", "", xml)

    def ensure_checked(label: str, checked: bool) -> None:
        nonlocal xml, text
        mark = "\u2611" if checked else "\u2610"
        other = "\u2610" if checked else "\u2611"
        # find in plain text
        idx = text.find(label)
        if idx < 0:
            print("WARN missing label", label.encode("unicode_escape"))
            return
        # look back a few chars for checkbox
        window = text[max(0, idx - 4) : idx]
        if mark in window:
            print("OK already", label.encode("unicode_escape"), mark.encode("unicode_escape"))
            return
        if other in window:
            # replace in XML: need to find the checkbox before label
            # simple global: replace first occurrence of other+label with mark+label if contiguous
            # but checkbox and label may be split — try contiguous first
            if (other + label) in xml:
                xml = xml.replace(other + label, mark + label, 1)
            else:
                # replace nearest other checkbox before label in plain via split pattern:
                # find other checkbox then optional tags then label — too hard; char-level
                pattern = re.compile(
                    re.escape(other)
                    + r"(?:<[^>]+>)*"
                    + "".join(
                        (r"(?:<[^>]+>)*" if i else "") + re.escape(c)
                        for i, c in enumerate(label)
                    )
                )
                m = pattern.search(xml)
                if m:
                    # rebuild with mark + original middle keeping tags but change first char
                    # simpler: replace only the checkbox char at m.start()
                    xml = xml[: m.start()] + mark + xml[m.start() + len(other) :]
                else:
                    print("WARN cannot flip", label.encode("unicode_escape"))
            text = re.sub(r"<[^>]+>", "", xml)
            print("flipped", label.encode("unicode_escape"), "->", mark.encode("unicode_escape"))
        else:
            print("WARN no checkbox near", label.encode("unicode_escape"), repr(window))

    ensure_checked("企业在职职工", True)
    ensure_checked("领取失业保险金人员", False)
    ensure_checked("国家职业资格证书", False)
    ensure_checked("国家技能等级证书", True)
    ensure_checked("已参加职业技能培训自行支付培训费用", True)
    ensure_checked("未参加职业技能培训", False)
    # second option may share prefix
    ensure_checked("已参加职业技能培训个人未支付培训费用", False)

    out_buf = BytesIO()
    with zipfile.ZipFile(out_buf, "w", zipfile.ZIP_DEFLATED) as zout:
        for info in z.infolist():
            data = z.read(info.filename)
            if info.filename == "word/document.xml":
                data = xml.encode("utf-8")
            zout.writestr(info, data)
    SRC.write_bytes(out_buf.getvalue())

    xml2 = zipfile.ZipFile(SRC).read("word/document.xml").decode("utf-8")
    text2 = re.sub(r"<[^>]+>", "", xml2)
    Path(r"D:/phpApp/qianlvguoyu_saas/scripts/_claim_text.txt").write_text(text2, encoding="utf-8")
    keys = [
        "name",
        "idCard",
        "phone",
        "address",
        "occupation",
        "workType",
        "jobPost",
        "level",
        "certificateNo",
        "issuingAuthority",
        "issueDate",
    ]
    for k in keys:
        ok = ("${" + k + "}") in xml2
        print("ph", k, ok)
    print("leftover_cert", CERT_NO in text2)
    # dump checkbox lines
    for label in [
        "企业在职职工",
        "领取失业保险金人员",
        "国家职业资格证书",
        "国家技能等级证书",
        "已参加职业技能培训自行支付培训费用",
        "未参加职业技能培训",
        "已参加职业技能培训个人未支付培训费用",
    ]:
        i = text2.find(label)
        print("ctx", repr(text2[max(0, i - 2) : i + len(label)]))


if __name__ == "__main__":
    main()
