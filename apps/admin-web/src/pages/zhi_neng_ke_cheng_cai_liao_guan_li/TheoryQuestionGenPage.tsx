/**
 * 理论知识试题生成管理页面 —— 方案 v1.2 §T5
 * 工作台重构（2026-09-09）：原"生成任务 / 试题草稿 / 入库与导出"三页签合并为「生成工作台」，
 * 批次选择不再用下拉，由总览表行内直达操作。本文件只保留"细目表导入"页签与容器。
 */
import React, { useMemo, useState } from 'react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import dayjs from 'dayjs';
import {
  Button, message, Modal, Popconfirm, Popover, Progress, Select, Space, Table, Tabs, Tag,
  Tooltip, Upload,
} from 'antd';
import { UploadOutlined } from '@ant-design/icons';
import { treeWorkTypes } from '../../api/exam';
import type { ExamWorkType } from '../../types/exam';
import {
  deleteBlueprint, downloadBlueprintSource, importBlueprint, listBlueprints,
} from './theoryGenApi';
import type { TheoryBlueprint } from './theoryGenApi';
import GenWorkbenchPage from './workbench/GenWorkbenchPage';
import { LEVEL_NAME, bpOptionLabel, parseCheckReport } from './workbench/constants';
import { usePaneActive, usePaneActiveRef } from '../../layouts/TabKeepAlive';
import './XiMuBiaoGuanLiPage.css';

const LEVEL_OPTIONS = [
  { value: 5, label: '五级/初级工' },
  { value: 4, label: '四级/中级工' },
  { value: 3, label: '三级/高级工' },
  { value: 2, label: '二级/技师' },
  { value: 1, label: '一级/高级技师' },
];

const TheoryQuestionGenPage: React.FC = () => {
  const [tab, setTab] = useState('import');
  const paneActive = usePaneActive(); // 标签页不可见时暂停导入进度 tick（§3.6）
  const paneActiveRef = usePaneActiveRef(paneActive); // 长生命周期定时器闭包读 ref

  // ---------------- 页签1：细目表导入 ----------------
  const [workTypeId, setWorkTypeId] = useState<number | undefined>();
  const [level, setLevel] = useState<number | undefined>(5);
  const [bpPage, setBpPage] = useState(1);
  const [bpPageSize, setBpPageSize] = useState(20);
  // 导入进度（同步接口无服务端进度回调，模拟进度渐近 95%、完成跳 100%）
  const [importing, setImporting] = useState(false);
  const [importPercent, setImportPercent] = useState(0);
  const importTimerRef = React.useRef<number | null>(null);
  // 源细目表下载（按批次 id 标记 loading）
  const [downloadingBpId, setDownloadingBpId] = useState<string | undefined>();
  // 行内"生成"带入工作台的批次
  const [presetBp, setPresetBp] = useState<TheoryBlueprint | undefined>();
  const qc = useQueryClient();

  const wtTree = useQuery({
    queryKey: ['theorygen-worktypes'],
    queryFn: treeWorkTypes,
  });

  const bpQuery = useQuery({
    queryKey: ['theorygen-blueprints', bpPage, bpPageSize],
    queryFn: () => listBlueprints({ page: bpPage, pageSize: bpPageSize }),
  });

  // 同名工种消歧（A 方案）：库里存在"顶级职业节点 + 同名子工种节点"的历史建模
  // （如矿山救护工 53/107、氧化铝制取工 22/117）。仅同名组显示父链路径，其余保持纯名称。
  const workTypeOptions = useMemo(() => {
    const rows: Array<{ id: number; title: string; path: string }> = [];
    const walk = (list: ExamWorkType[], prefix: string) => {
      for (const w of list ?? []) {
        const path = prefix ? `${prefix}/${w.title}` : w.title;
        rows.push({ id: w.id, title: w.title, path });
        if (w.children?.length) walk(w.children, path);
      }
    };
    walk(wtTree.data ?? [], '');
    const titleCount = new Map<string, number>();
    for (const r of rows) {
      titleCount.set(r.title, (titleCount.get(r.title) ?? 0) + 1);
    }
    return rows.map((r) => ({
      value: r.id,
      label: (titleCount.get(r.title) ?? 0) > 1 ? r.path : r.title,
    }));
  }, [wtTree.data]);

  const doImport = async (file: File) => {
    if (!workTypeId || !level) {
      message.warning('请先选择工种与等级');
      return false;
    }
    setImporting(true);
    setImportPercent(0);
    if (importTimerRef.current !== null) window.clearInterval(importTimerRef.current);
    importTimerRef.current = window.setInterval(() => {
      // 标签不可见时计时继续、跳过 setState（§3.6 乙组口径：不中断任务）
      if (paneActiveRef.current) {
        setImportPercent((p) =>
          p >= 95 ? p : Math.min(95, p + Math.max(1, Math.round((95 - p) / 10))),
        );
      }
    }, 300);
    try {
      const bp = await importBlueprint(file, workTypeId, level);
      const errs = parseCheckReport(bp.checkReport);
      if (errs.length) {
        // 校验提示不阻塞使用，但必须让用户当场看到差在哪（如一级权重合计≠100%）
        Modal.warning({
          title: `导入完成，有 ${errs.length} 条校验提示`,
          width: 520,
          content: (
            <div>
              <div style={{ marginBottom: 8, color: '#888' }}>
                以下内容请对照源文件核对，修正后可重新导入：
              </div>
              <ul style={{ margin: 0, paddingLeft: 18 }}>
                {errs.map((e, i) => <li key={i}>{e}</li>)}
              </ul>
            </div>
          ),
        });
      } else {
        message.success(`导入成功：考评点 ${bp.pointCount} 个，X:Y:Z=${bp.xyzRatio ?? '-'}`);
      }
      qc.invalidateQueries({ queryKey: ['theorygen-blueprints'] });
    } catch {
      // 错误提示由拦截器统一处理
    } finally {
      if (importTimerRef.current !== null) {
        window.clearInterval(importTimerRef.current);
        importTimerRef.current = null;
      }
      setImportPercent(100);
      window.setTimeout(() => {
        setImporting(false);
        setImportPercent(0);
      }, 500);
    }
    return false;
  };

  /** 下载导入时的源细目表，供对照核对（文件名用导入时的原始文件名） */
  const doDownloadBlueprint = async (r: TheoryBlueprint) => {
    if (!r.sourceFileId) {
      return;
    }
    setDownloadingBpId(r.id);
    try {
      const blob = await downloadBlueprintSource(r.sourceFileId);
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = r.sourceFileName || `细目表-${r.title}.xlsx`;
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      // 错误提示由拦截器统一处理
    } finally {
      setDownloadingBpId(undefined);
    }
  };

  const bpColumns = [
    { title: '标题', dataIndex: 'title', key: 'title', ellipsis: true },
    { title: '等级', dataIndex: 'level', key: 'level', width: 90,
      render: (v: number) => LEVEL_NAME[v] ?? v },
    { title: '考评点', dataIndex: 'pointCount', key: 'pointCount', width: 80 },
    { title: 'X:Y:Z', dataIndex: 'xyzRatio', key: 'xyzRatio', width: 110 },
    { title: '权重校验', dataIndex: 'weightCheck', key: 'weightCheck', width: 90,
      render: (v: number, r: TheoryBlueprint) => {
        const tag = v === 1 ? <Tag color="success">通过</Tag> : <Tag color="warning">未通过</Tag>;
        const errs = parseCheckReport(r.checkReport);
        // 有校验记录时点击标签查看明细（如"一级权重合计 97.00% ≠ 100%"）
        if (!errs.length) {
          return tag;
        }
        return (
          <Popover trigger="click" title="导入校验详情" overlayStyle={{ width: 460 }}
            content={
              <ul style={{ margin: 0, paddingLeft: 18 }}>
                {errs.map((e, i) => <li key={i}>{e}</li>)}
              </ul>
            }>
            <span style={{ cursor: 'pointer' }}>{tag}</span>
          </Popover>
        );
      } },
    { title: '细目表下载', key: 'srcFile', width: 100,
      render: (_: unknown, r: TheoryBlueprint) => r.sourceFileId ? (
        <Button size="small" loading={downloadingBpId === r.id}
          onClick={() => { void doDownloadBlueprint(r); }}>下载</Button>
      ) : (
        <Tooltip title="该批次导入时未保留源文件，重新导入后即可下载">
          <Button size="small" disabled>下载</Button>
        </Tooltip>
      ) },
    { title: '导入时间', dataIndex: 'createdAt', key: 'createdAt', width: 150,
      render: (v: string) => (v ? dayjs(v).format('YYYY-MM-DD HH:mm') : '-') },
    { title: '状态', dataIndex: 'status', key: 'status', width: 100,
      render: (v: string) => (v === 'done' ? <Tag color="success">已完成</Tag>
        : v === 'generating' ? <Tag color="processing">生成中</Tag> : <Tag>已导入</Tag>) },
    { title: '操作', key: 'op', width: 160, render: (_: unknown, r: TheoryBlueprint) => (
      <Space>
        <Button size="small" onClick={() => { setPresetBp(r); setTab('workbench'); }}>生成</Button>
        <Popconfirm title="确认删除该批次？" onConfirm={async () => {
          await deleteBlueprint(r.id);
          message.success('已删除');
          qc.invalidateQueries({ queryKey: ['theorygen-blueprints'] });
        }}>
          <Button size="small" danger>删除</Button>
        </Popconfirm>
      </Space>
    ) },
  ];

  return (
    <div className="ximu-page">
      <Tabs activeKey={tab} onChange={setTab} items={[
        {
          key: 'import', label: '细目表导入',
          children: (
            <>
              <Space style={{ marginBottom: 16 }} wrap>
                <Select style={{ width: 220 }} placeholder="选择工种（可输入搜索）"
                  showSearch
                  optionFilterProp="label"
                  options={workTypeOptions}
                  value={workTypeId}
                  onChange={(v) => setWorkTypeId(v)} />
                <Select style={{ width: 160 }} placeholder="等级" options={LEVEL_OPTIONS}
                  value={level} onChange={(v) => setLevel(v)} />
                <Upload accept=".xlsx,.xls" showUploadList={false} beforeUpload={doImport}
                  disabled={importing}>
                  <Button type="primary" icon={<UploadOutlined />} loading={importing}>
                    {importing ? '导入中…' : '导入细目表 xlsx'}
                  </Button>
                </Upload>
                {importing && (
                  <>
                    <Progress percent={importPercent} size="small" status="active"
                      style={{ width: 160, margin: 0 }} />
                    <span style={{ color: '#888', fontSize: 12 }}>正在解析细目表，请勿关闭页面…</span>
                  </>
                )}
              </Space>
              <Table rowKey="id" size="small"
                loading={bpQuery.isLoading}
                dataSource={bpQuery.data?.items ?? []}
                columns={bpColumns}
                pagination={{
                  current: bpPage,
                  pageSize: bpPageSize,
                  total: bpQuery.data?.total,
                  showSizeChanger: true,
                  pageSizeOptions: [20, 30, 40, 50],
                  showTotal: (t: number) => `共 ${t} 条`,
                  onChange: (p, s) => {
                    setBpPage(s !== bpPageSize ? 1 : p);
                    setBpPageSize(s);
                  },
                }} />
            </>
          ),
        },
        {
          key: 'workbench', label: '生成工作台',
          // forceRender：停在其他页签时也保持挂载，进行中批次轮询与"完成提示"不中断
          forceRender: true,
          children: <GenWorkbenchPage workTypeOptions={workTypeOptions} preset={presetBp} />,
        },
      ]} />
    </div>
  );
};

export default TheoryQuestionGenPage;
