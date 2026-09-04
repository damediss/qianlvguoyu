# -*- coding: utf-8 -*-
"""Build personal skill-upgrade claim form template with ${placeholders}."""
from __future__ import annotations

import zipfile
from io import BytesIO
from pathlib import Path

SRC = Path(
    r"D:/phpApp/qianlvguoyu_saas/services/platform-api/src/main/resources/templates/subsidy/personal_skill_upgrade_claim_2025.docx"
)


def main() -> None:
    z = zipfile.ZipFile(SRC)
    xml = z.read("word/document.xml").decode("utf-8")

    # Longer / more specific strings first. 电焊工 before 焊工.
    parts = xml.split("电焊工")
    if len(parts) >= 3:
        xml = parts[0] + "${workType}" + parts[1] + "${jobPost}" + "电焊工".join(parts[2:])
    elif len(parts) == 2:
        xml = parts[0] + "${workType}" + parts[1]

    replacements = [
        ("邓兴山", "${name}"),
        ("452622199611100316", "${idCard}"),
        ("18587668341", "${phone}"),
        ("广西百色市田阳区那坡镇濑旺村邓屋屯3号", "${address}"),
        ("S000045132003263000046", "${certificateNo}"),
        ("广西百色工业投资发展集团有限公司", "${issuingAuthority}"),
        ("20260827", "${issueDate}"),
        ("三级/高级工", "${level}"),
        ("焊工", "${occupation}"),
    ]
    for old, new in replacements:
        xml = xml.replace(old, new)

    # Fixed checkbox selections per product requirement
    xml = xml.replace("□企业在职职工", "☑企业在职职工")
    xml = xml.replace("☑领取失业保险金人员", "□领取失业保险金人员")
    xml = xml.replace("☑国家职业资格证书", "□国家职业资格证书")
    xml = xml.replace("□国家技能等级证书", "☑国家技能等级证书")
    xml = xml.replace("□已参加职业技能培训自行支付培训费用", "☑已参加职业技能培训自行支付培训费用")
    xml = xml.replace("☑未参加职业技能培训", "□未参加职业技能培训")
    xml = xml.replace("☑已参加职业技能培训个人未支付培训费用", "□已参加职业技能培训个人未支付培训费用")

    out_buf = BytesIO()
    with zipfile.ZipFile(out_buf, "w", zipfile.ZIP_DEFLATED) as zout:
        for info in z.infolist():
            data = z.read(info.filename)
            if info.filename == "word/document.xml":
                data = xml.encode("utf-8")
            zout.writestr(info, data)
    SRC.write_bytes(out_buf.getvalue())
    print("written", SRC, SRC.stat().st_size)

    xml3 = zipfile.ZipFile(SRC).read("word/document.xml").decode("utf-8")
    for key in [
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
    ]:
        print(key, ("${" + key + "}") in xml3)

    for check in [
        "☑企业在职职工",
        "□领取失业保险金人员",
        "□国家职业资格证书",
        "☑国家技能等级证书",
        "☑已参加职业技能培训自行支付培训费用",
    ]:
        print(check, "OK" if check in xml3 else "MISSING")


if __name__ == "__main__":
    main()
