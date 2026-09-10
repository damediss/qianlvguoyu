# -*- coding: utf-8 -*-
import re
import zipfile
from pathlib import Path

p = Path(
    r"D:/phpApp/qianlvguoyu_saas/services/platform-api/src/main/resources/templates/subsidy/personal_skill_upgrade_claim_2025.docx"
)
xml = zipfile.ZipFile(p).read("word/document.xml").decode("utf-8")
text = re.sub(r"<[^>]+>", "", xml)
text = text.replace("\u2610", "[ ]").replace("\u2611", "[x]")
out = Path(r"D:/phpApp/qianlvguoyu_saas/scripts/_claim_text.txt")
out.write_text(text, encoding="utf-8")
print("text_len", len(text))
print("has_S000", "S000" in xml, "S000" in text)
print("has_cert_ph", "${certificateNo}" in xml)
# find any leftover sample values
for s in ["S000045132003263000046", "邓兴山", "电焊工", "焊工"]:
    print("leftover", s, s in xml, s in text)
# show snippet around certificateNo area / level
idx = text.find("${level}")
print("around level:", repr(text[max(0, idx - 80) : idx + 120]))
idx2 = text.find("${issuingAuthority}")
print("around issuing:", repr(text[max(0, idx2 - 120) : idx2 + 80]))
# checkboxes
for c in [
    "企业在职职工",
    "领取失业保险金",
    "国家职业资格",
    "国家技能等级",
    "自行支付培训费用",
]:
    i = text.find(c)
    print("check", c, repr(text[max(0, i - 3) : i + len(c) + 2]))
