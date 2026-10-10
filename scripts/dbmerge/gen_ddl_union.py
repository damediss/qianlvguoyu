# -*- coding: utf-8 -*-
"""
gen_ddl_union.py — 生成 02_ddl.sql（结构并集版）

结构来源规则（对应方案 §3 + 2026-08-24 结构并集修正）：
  - SaaS 独有 / D 类 / B 类（SaaS⊇PHP）/ A_sup 未发散     → SaaS 结构
  - PHP 独有                                                → PHP 结构（8.4 转换）
  - A / C / A_sup 发散（platform_user 等）                  → PHP 结构为基 + SaaS 独有列（可空）补并集
D 类（sys_user/sys_tenant/sys_dict/sys_index_swiper）SaaS 为准，PHP 数据归档不迁移。
MANUAL（cms_article*/train_dev_organization）不在本脚本，待裁决。
"""
import json, re, os

T = r"C:\Users\admin\AppData\Local\Temp"
ROOT = os.path.dirname(os.path.abspath(__file__))
ANALYSIS = os.path.join(ROOT, "../../outputs/dbmerge-analysis")

def parse_create_tables(sql):
    """从 dump 文本解析 {表名: (columns有序dict名->定义, 其余尾部选项)}"""
    tables = {}
    for m in re.finditer(r'CREATE TABLE `([^`]+)` \((.*?)\n\) ([^;]*);', sql, re.S):
        name, body, tail = m.group(1), m.group(2), m.group(3)
        cols = {}
        # 逐行切分列定义（列行以 `  `col`` 开头）
        for line in body.split('\n'):
            lm = re.match(r'\s*`([^`]+)`\s+(.*)', line)
            if lm:
                cols[lm.group(1)] = lm.group(2).rstrip(',')
        tables[name] = (cols, tail, m.group(0))
    return tables

def load(path):
    return open(path, encoding='utf-8', errors='replace').read()

def transform_php(sql):
    sql = re.sub(r'\b(tinyint|smallint|mediumint|int|bigint)\(\d+\)', r'\1', sql)
    sql = sql.replace('ENGINE=MyISAM', 'ENGINE=InnoDB')
    return sql

saas_tables = parse_create_tables(load(T + r'\saas_ddl_raw.sql'))
php_tables = parse_create_tables(transform_php(load(T + r'\php_ddl_raw.sql')))
gx_tables = parse_create_tables(transform_php(load(T + r'\gx_ddl_raw.sql')))

# SaaS 全量列定义（用于给 PHP 基表补 SaaS 独有列；A 类表不在 saas_ddl_raw 里）
def load_col_defs(path):
    d = {}
    for line in open(path, encoding='utf-8'):
        p = line.rstrip('\n').split('\t')
        if len(p) >= 3:
            d.setdefault(p[0], {})[p[1]] = p[2]  # table -> col -> type
    return d
SAAS_COLS = load_col_defs(os.path.join(ANALYSIS, 'saas_columns.tsv'))
PHP_COLS = load_col_defs(os.path.join(ANALYSIS, 'php_columns.tsv'))

cats = json.load(open(os.path.join(ANALYSIS, 'table_categories.json'), encoding='utf-8'))

D = set(cats['D'])
B = set(cats['B'])
A_sup = set(cats['A_sup'])
C = set(cats['C'])
A = set(cats['A'])

def make_nullable(coldef):
    """把列定义改为可空无默认（PHP 插入不感知这些 SaaS 新增列）"""
    d = coldef
    d = re.sub(r'\s+NOT NULL', '', d)
    d = re.sub(r"\s+DEFAULT\s+('[^']*'|[^ ,]+)", '', d, flags=re.I)
    d = re.sub(r'\s+ON UPDATE [^ ,]+', '', d, flags=re.I)
    d = re.sub(r'\s+AUTO_INCREMENT', '', d, flags=re.I)
    d = re.sub(r'\s+COMMENT\s+\'[^\']*\'', '', d)
    d = d.strip()
    # 数值/文本可空即可；时间戳可空
    return d + ' NULL DEFAULT NULL'

def strip_dclass_fk(create_sql):
    lines = create_sql.split('\n')
    out = [ln for ln in lines if not re.search(
        r'CONSTRAINT `[^`]+` FOREIGN KEY \([^)]*\) REFERENCES `(sys_tenant|sys_user|sys_dict|sys_index_swiper)`', ln)]
    for i in range(len(out) - 1, -1, -1):
        if out[i].strip().startswith(') '):
            j = i - 1
            while j >= 0 and out[j].strip() == '':
                j -= 1
            if j >= 0 and out[j].rstrip().endswith(','):
                out[j] = out[j].rstrip()[:-1]
            break
    return '\n'.join(out)

def base_create(tables_dict, name):
    return tables_dict[name][2]

out = []
out.append('-- ===========================================================')
out.append('-- 02_ddl.sql  合并建表（gjjddbpro + gxwebsoft_core）—— 结构并集版')
out.append('-- 2026-08-24 修正：共有发散表采用「PHP 结构为基 + SaaS 独有可空列」并集，')
out.append('--   避免合并后 SaaS 侧报 Unknown column（BaseEntity/审计列）或 PHP 丢专属列（device_* 等）')
out.append('-- 8.4 转换：去整数显示宽度；MyISAM→InnoDB ×7；字符集不变；FOREIGN_KEY_CHECKS=0 规避顺序')
out.append('-- 删除 8 个引用 sys_tenant(tenant_id) 的失效外键（sys_tenant 采用 SaaS 版无该列）')
out.append('-- MANUAL 表（cms_article*/train_dev_organization）不在本脚本，待裁决')
out.append('-- ===========================================================')
out.append('SET NAMES utf8mb4;')
out.append('SET FOREIGN_KEY_CHECKS=0;')
out.append('')
out.append('USE gjjddbpro;')
out.append('')

alter_statements = []
n_saas = n_php = n_union = 0

def emit_saas(t):
    global n_saas
    out.append(base_create(saas_tables, t))
    n_saas += 1

def emit_php_union(t):
    """PHP 结构为基；SaaS 独有列生成 ALTER ADD（可空，定义取自 saas_columns.tsv）。"""
    global n_php, n_union
    out.append(strip_dclass_fk(base_create(php_tables, t)))
    n_php += 1
    saas_cols = set(SAAS_COLS.get(t, {}))
    php_cols = set(PHP_COLS.get(t, {}))
    extra = [c for c in SAAS_COLS.get(t, {}) if c not in php_cols]
    if extra:
        adds = ['ADD COLUMN `%s` %s NULL DEFAULT NULL' % (c, SAAS_COLS[t][c]) for c in extra]
        alter_statements.append('ALTER TABLE `%s`\n  %s;' % (t, ',\n  '.join(adds)))
        n_union += 1

# SaaS 独有 + D + B(SaaS⊇PHP) + A_sup 未发散 → SaaS 结构
for t in cats['SAAS_ONLY']:
    emit_saas(t)
for t in cats['D']:
    emit_saas(t)
for t in cats['B']:
    emit_saas(t)
for t in cats['A_sup']:
    # 发散（PHP 有 SaaS 无的列）→ 并集；否则 SaaS 为准
    saas_cols = set(saas_tables.get(t, ({}, '', ''))[0])
    php_cols = set(php_tables.get(t, ({}, '', ''))[0])
    if php_cols - saas_cols:
        emit_php_union(t)
    else:
        emit_saas(t)
# A + C → PHP 基 + SaaS 独有列并集
for t in cats['A'] + cats['C']:
    emit_php_union(t)

# PHP 独有 → PHP 结构（8.4 转换，纯 PHP 使用）
for t in cats['PHP_ONLY']:
    emit_php_union(t)

# 并集 ALTER 汇总（建表后统一补列，空表上执行）
if alter_statements:
    out.append('')
    out.append('-- ===== 结构并集：SaaS 独有可空列（PHP 代码无感知） =====')
    out += alter_statements

out.append('')
out.append('USE gxwebsoft_core;')
out.append('')
for name, (cols, tail, full) in gx_tables.items():
    out.append(strip_dclass_fk(full))
out.append('')
out.append('SET FOREIGN_KEY_CHECKS=1;')

dest = os.path.join(ROOT, '02_ddl.sql')
open(dest, 'w', encoding='utf-8', newline='\n').write('\n'.join(out))
total = '\n'.join(out)
print('written', dest)
print('SaaS 结构表:', n_saas, ' PHP 基表:', n_php, ' 其中并集补列:', n_union, ' gx:', len(gx_tables))
print('CREATE TABLE:', total.count('CREATE TABLE'), ' ALTER:', total.count('ALTER TABLE'))
print('残留 int(N):', len(re.findall(r'\bint\(\d+\)', total)), ' MyISAM:', total.count('ENGINE=MyISAM'))
