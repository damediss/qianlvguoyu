from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor


OUT = "/Users/macmain/Documents/qianlvguoyu/资源需求与预算计划.docx"


def cn_font(run, size=None, bold=None, color=None):
    run.font.name = "Microsoft YaHei"
    run._element.rPr.rFonts.set(qn("w:eastAsia"), "Microsoft YaHei")
    if size:
        run.font.size = Pt(size)
    if bold is not None:
        run.bold = bold
    if color:
        run.font.color.rgb = RGBColor(*color)


def set_shading(cell, fill):
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:fill"), fill)
    tc_pr.append(shd)


def set_margins(cell, top=80, start=80, bottom=80, end=80):
    tc_pr = cell._tc.get_or_add_tcPr()
    tc_mar = tc_pr.first_child_found_in("w:tcMar")
    if tc_mar is None:
        tc_mar = OxmlElement("w:tcMar")
        tc_pr.append(tc_mar)
    for name, value in {"top": top, "start": start, "bottom": bottom, "end": end}.items():
        child = tc_mar.find(qn(f"w:{name}"))
        if child is None:
            child = OxmlElement(f"w:{name}")
            tc_mar.append(child)
        child.set(qn("w:w"), str(value))
        child.set(qn("w:type"), "dxa")


def repeat_header(row):
    tr_pr = row._tr.get_or_add_trPr()
    header = OxmlElement("w:tblHeader")
    header.set(qn("w:val"), "true")
    tr_pr.append(header)


def set_width(cell, cm):
    tc_pr = cell._tc.get_or_add_tcPr()
    tc_w = tc_pr.first_child_found_in("w:tcW")
    if tc_w is None:
        tc_w = OxmlElement("w:tcW")
        tc_pr.append(tc_w)
    tc_w.set(qn("w:w"), str(int(cm * 567)))
    tc_w.set(qn("w:type"), "dxa")


def heading(doc, text, level=1):
    p = doc.add_paragraph()
    p.style = f"Heading {level}"
    r = p.add_run(text)
    cn_font(r, 15 if level == 1 else 12.5, True, (31, 78, 121))
    return p


def para(doc, text, bold_prefix=None):
    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(6)
    if bold_prefix and text.startswith(bold_prefix):
        r = p.add_run(bold_prefix)
        cn_font(r, 10.5, True)
        r = p.add_run(text[len(bold_prefix):])
        cn_font(r, 10.5)
    else:
        r = p.add_run(text)
        cn_font(r, 10.5)
    return p


def table(doc, headers, rows, widths, font_size=8.7):
    t = doc.add_table(rows=1, cols=len(headers))
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    t.style = "Table Grid"
    for i, h in enumerate(headers):
        t.rows[0].cells[i].text = h
    for row in rows:
        cells = t.add_row().cells
        for i, value in enumerate(row):
            cells[i].text = str(value)

    for ri, row in enumerate(t.rows):
        for ci, cell in enumerate(row.cells):
            cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
            set_margins(cell)
            if ci < len(widths):
                set_width(cell, widths[ci])
            if ri == 0:
                set_shading(cell, "2F5597")
            elif ri % 2 == 0:
                set_shading(cell, "F7F9FC")
            for p in cell.paragraphs:
                p.paragraph_format.space_after = Pt(0)
                p.paragraph_format.line_spacing = 1.05
                if ri == 0 or ci in (0, 2, 3, 4):
                    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
                for r in p.runs:
                    cn_font(r, font_size, ri == 0, (255, 255, 255) if ri == 0 else None)
    repeat_header(t.rows[0])
    doc.add_paragraph()
    return t


doc = Document()
section = doc.sections[0]
section.top_margin = Cm(1.6)
section.bottom_margin = Cm(1.6)
section.left_margin = Cm(1.6)
section.right_margin = Cm(1.6)

styles = doc.styles
styles["Normal"].font.name = "Microsoft YaHei"
styles["Normal"]._element.rPr.rFonts.set(qn("w:eastAsia"), "Microsoft YaHei")
styles["Normal"].font.size = Pt(10.5)

title = doc.add_paragraph()
title.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = title.add_run("资源需求与预算计划")
cn_font(r, 24, True, (31, 78, 121))

subtitle = doc.add_paragraph()
subtitle.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = subtitle.add_run("公司数字化平台、AI 智能体与本地知识库建设")
cn_font(r, 12, False, (89, 89, 89))

meta = doc.add_paragraph()
meta.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = meta.add_run("版本：完整修正版｜日期：2026年4月")
cn_font(r, 10, False, (127, 127, 127))

heading(doc, "一、建设目标")
table(
    doc,
    ["目标", "说明"],
    [
        ["保护公司数字资产", "集中保存代码、课程、题库、模型、文档、外包成果"],
        ["支撑内部开发", "配置编程电脑、测试演示服务器、本地测试环境、Git 管理"],
        ["支撑 AI 建设", "新增配置本地 AI 电脑服务器，并配套 ChatGPT Pro 20x"],
        ["支撑 ChatGPT 使用", "配置 VPN 团队套餐，保障 AI 工具稳定访问"],
        ["降低重复采购", "云服务器、对象存储、文档系统、测试服务器不重复采购"],
        ["逐步收回外包", "后续开发转为内部主导，外包成果统一交接"],
    ],
    [4.5, 12.5],
    9.5,
)

heading(doc, "二、资源需求总表")
para(doc, "说明：为避免 Word 大表分页不稳定，本节按类别拆分为多个资源表，内容与总表一致。")

heading(doc, "2.1 本地存储与电力保障", 2)
table(
    doc,
    ["类别", "项目", "数量/规格", "预算", "优先级", "说明"],
    [
        ["本地存储", "NAS 存储服务器主机", "1套", "0.5万-1万元", "P0", "建议 8 盘位或以上，不含硬盘；用于公司本地资料、代码备份、课程、题库、模型、外包成果集中管理"],
        ["本地存储", "企业级硬盘、备份盘", "1-2块", "0.3万-0.7万元", "P0", "先满足基础归档和备份，后续按容量需求扩展；16TB 硬盘按约3000-3500元/块估算"],
        ["电力保障", "UPS 不间断电源", "1套", "3000-1万元", "P2", "不重要，可后续视环境补充；用于防断电、防数据损坏，保障 NAS 和关键设备安全关机"],
    ],
    [2.4, 4.0, 2.7, 3.0, 1.7, 6.8],
)

heading(doc, "2.1.1 硬盘采购测算")
para(doc, "京东相关 16TB 硬盘参考价：希捷酷狼 16TB NAS 盘历史京东自营价约 2546-2599元，部分公开报价页面记录约 3399元。企业级/服务器盘价格波动较大，预算按 3000-3500元/块更稳妥。")
table(
    doc,
    ["方案", "配置", "单价参考", "小计"],
    [
        ["基础方案", "16TB 企业级/NAS 硬盘 × 1块", "3000-3500元/块", "0.3万-0.35万元"],
        ["标准方案", "16TB 企业级/NAS 硬盘 × 2块", "3000-3500元/块", "0.6万-0.7万元"],
    ],
    [3.5, 7.2, 4.0, 4.0],
    9.2,
)
para(doc, "建议：企业级硬盘、备份盘预算按 0.3万-0.7万元保留，先满足基础归档和备份，后续按容量需求扩展。", "建议：")

heading(doc, "2.2 AI 工具与配套网络", 2)
table(
    doc,
    ["类别", "项目", "数量/规格", "预算", "优先级", "说明"],
    [
        ["AI 工具", "ChatGPT Pro 20x", "1套", "2400美元/年，不含税，约17,280元/年", "P0", "课程开发、智能体、资料整理、代码辅助；不是20个账号，是20x使用额度/套餐"],
        ["AI 配套网络", "VPN 团队套餐", "1套，320元/月", "3,840元/年", "P0", "保障 ChatGPT、AI 工具和相关海外服务稳定访问；作为 ChatGPT Pro 20x 配套网络资源"],
    ],
    [2.4, 4.0, 2.7, 3.0, 1.7, 6.8],
)

heading(doc, "2.3 AI 电脑/本地 AI 电脑服务器", 2)
table(
    doc,
    ["类别", "项目", "数量/规格", "预算", "优先级", "说明"],
    [
        ["AI 电脑", "本地 AI 电脑服务器/GPU 工作站", "1套", "10万-30万元", "P1", "当前没有，需要新增采购；用于本地模型、知识库、智能体、课程 AI 工作流"],
    ],
    [2.4, 4.0, 2.7, 3.0, 1.7, 6.8],
)

heading(doc, "2.4 编程电脑及配套设备", 2)
table(
    doc,
    ["类别", "项目", "数量/规格", "预算", "优先级", "说明"],
    [
        ["编程电脑", "Mac mini M4 16GB/512GB", "4台", "23,996元", "P2", "已有原有主机，可后续补充采购；用于开发、测试、AI 配置、资料整理"],
        ["显示设备", "Mac mini 配套显示器", "4台", "4,000-6,000元", "P2", "不急，可先使用现有 PC 配套显示设备；建议 27英寸 2K/4K 显示器"],
        ["输入设备", "鼠标键盘套装", "4套", "1,200-2,400元", "P2", "不急，可先使用现有 PC 配套设备；办公/编程套装"],
        ["移动编程电脑", "MacBook Pro 14英寸 M5 起步款", "1台", "13,499元起", "P2", "不急，可先使用现有 PC；后续按实际采购调整"],
    ],
    [2.4, 4.0, 2.7, 3.0, 1.7, 6.8],
)

heading(doc, "2.5 平台运行与代码文档管理", 2)
table(
    doc,
    ["类别", "项目", "数量/规格", "预算", "优先级", "说明"],
    [
        ["测试环境", "本地电脑兼任测试服务器", "现有或新增电脑承担", "0元", "P0", "初期不单独采购测试服务器；用于开发测试、演示、联调"],
        ["测试演示", "测试演示服务器", "1套", "0.5万-2万元", "P1", "用于系统测试、客户/内部演示、外部访问联调和阶段验收"],
        ["代码管理", "Git 代码管理", "1套", "0元", "P0", "Java、PHP、前端、AI 代码统一管理；不单独采购私有代码仓库"],
        ["文档管理", "文档资料归档", "纳入 NAS", "0元", "P0", "需求、流程、会议纪要、接口、验收文档统一保存；不单独采购文档系统"],
        ["正式运行", "云服务器/生产服务器", "已有", "不单独计算", "P0", "SaaS 平台、竞赛系统正式运行；已有资源，保留在清单"],
        ["线上文件", "对象存储/OSS", "已有", "按实际用量计费，不单独预估", "P0", "用户上传材料、课件、证书、图片、视频；已有资源，保留在清单"],
    ],
    [2.4, 4.0, 2.7, 3.0, 1.7, 6.8],
)

heading(doc, "2.6 管理、整理与培训", 2)
table(
    doc,
    ["类别", "项目", "数量/规格", "预算", "优先级", "说明"],
    [
        ["安全运维", "权限、日志审计、备份、安全策略", "1套", "2万-10万元/年", "P2", "账号安全、操作留痕、数据保护；可结合现有服务器和 NAS 配置"],
        ["外包管理", "外包成果交接与收回", "按现有项目", "0元", "P2", "收回代码、数据库、文档、部署资料、账号权限；后续开发逐步转为内部主导"],
        ["数据整理", "课程、题库、标准、政策、历史资料结构化", "按批次", "0元", "P2", "AI 知识库、精准学、课程开发工作流基础；由公司其他部门人员兼任"],
        ["内部培训", "AI 工具培训、平台操作培训", "按部门", "1万-5万元", "P2", "推动各部门实际使用系统和智能体；可分批组织"],
    ],
    [2.4, 4.0, 2.7, 3.0, 1.7, 6.8],
)

heading(doc, "三、新增采购预算汇总")
table(
    doc,
    ["项目", "预算"],
    [
        ["NAS 存储服务器主机", "0.5万-1万元"],
        ["企业级硬盘、备份盘", "0.3万-0.7万元"],
        ["UPS 不间断电源", "3000-1万元"],
        ["Mac mini 编程电脑 4台", "23,996元"],
        ["配套显示器 4台", "4,000-6,000元"],
        ["鼠标键盘 4套", "1,200-2,400元"],
        ["MacBook Pro 14英寸 M5", "13,499元起"],
        ["测试演示服务器", "0.5万-2万元"],
        ["本地 AI 电脑服务器/GPU 工作站 1套", "10万-30万元"],
        ["ChatGPT Pro 20x", "约17,280元/年，不含税"],
        ["VPN 团队套餐", "3,840元/年"],
        ["安全运维", "2万-10万元/年"],
        ["内部培训", "1万-5万元"],
    ],
    [10.0, 7.0],
    9.5,
)

heading(doc, "四、预算合计")
table(
    doc,
    ["预算类别", "金额"],
    [
        ["本地存储与电力保障", "1.1万-2.7万元"],
        ["AI 工具与配套网络", "约21,120元/年，不含 ChatGPT 税费"],
        ["本地 AI 电脑服务器/GPU 工作站", "10万-30万元"],
        ["编程电脑及配套设备", "42,695-45,895元"],
        ["测试演示服务器", "0.5万-2万元"],
        ["安全运维", "2万-10万元/年"],
        ["内部培训", "1万-5万元"],
        ["合计", "约19.0万-56.4万元"],
    ],
    [10.0, 7.0],
    9.5,
)

heading(doc, "五、不单独计入新增预算的资源")
table(
    doc,
    ["项目", "处理方式"],
    [
        ["Git 代码管理", "使用 Git，不单独采购私有代码仓库"],
        ["文档系统", "统一纳入 NAS，不单独采购企业知识库"],
        ["测试服务器", "由本地编程电脑兼任"],
        ["云服务器/生产服务器", "已有，不单独计算"],
        ["对象存储/OSS", "已有，按实际用量计费，不单独预估"],
        ["外包开发", "不新增大额预算，逐步收回"],
        ["数据整理", "由公司其他部门人员兼任，不单独列预算"],
    ],
    [6.0, 11.0],
    9.5,
)

heading(doc, "六、资源使用原则")
table(
    doc,
    ["原则", "说明"],
    [
        ["资产统一归档", "代码、文档、课程、题库、模型、合同、外包成果统一进入 NAS"],
        ["代码统一管理", "Java、PHP、前端、AI 代码统一使用 Git 管理"],
        ["文档统一保存", "需求、流程、会议纪要、接口、验收文档统一归档到 NAS"],
        ["线上与本地区分", "对象存储用于线上系统附件，NAS 用于本地资产沉淀和备份"],
        ["AI 优先落地", "先用 ChatGPT Pro 20x 提效，同时采购本地 AI 电脑服务器"],
        ["敏感资料本地优先", "题库、课程母版、客户资料、内部经营资料优先本地保存"],
        ["外包逐步收回", "收回代码、数据库、接口文档、部署说明、账号权限"],
        ["避免重复采购", "已有云服务器、对象存储、Git、本地测试环境继续使用；测试演示服务器仅用于测试、演示和联调"],
    ],
    [5.5, 11.5],
    9.5,
)

heading(doc, "七、结论")
para(doc, "本次资源需求重点投入 NAS、基础硬盘、ChatGPT Pro 20x、VPN 团队套餐、本地 AI 电脑服务器，以及必要的安全运维和内部培训。")
para(doc, "云服务器、对象存储、Git、文档系统、测试服务器、外包开发和数据整理不作为新增预算项。")
para(doc, "整体新增预算建议按约 19.0万-56.4万元预留。该预算能够支撑公司完成数字资产集中管理、AI 工具使用、本地模型部署、智能体建设、测试演示联调、课程 AI 工作流建设和外包成果逐步收回。")

doc.save(OUT)
print(OUT)
