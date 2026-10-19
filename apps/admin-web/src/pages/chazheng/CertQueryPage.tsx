import {
  CloudUploadOutlined,
  DownloadOutlined,
  ReloadOutlined,
  SearchOutlined,
  IdcardOutlined,
  DashboardOutlined,
  CheckCircleOutlined,
  RedoOutlined,
} from "@ant-design/icons";
import {
  Button,
  Input,
  Select,
  Space,
  Table,
  Tag,
  Tooltip,
  Typography,
  Descriptions,
  Upload,
  Modal,
  message,
  Progress,
} from "antd";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { FileTextOutlined } from "@ant-design/icons";
import { FilePreviewModal, type FilePreviewTarget } from "../../components/FilePreviewModal";
import { getPlatformFile } from "../../api/files";
import { useState, useCallback, useRef, useEffect } from "react";
import {
  pageUsers,
  getYmdList,
  importUsers,
  triggerCertCheckByYmd,
  certCheck,
  exportUsers,
  getEnterprises,
  downloadTemplate,
  getCertProgress,
  getCertBatchProgress,
} from "../../api/chazheng";
import type { ChazhengUser, ChazhengCert, CertCheckBatchProgress } from "../../types/chazheng";
import { usePaneActive, usePaneActiveRef } from "../../layouts/TabKeepAlive";

export function CertQueryPage() {
  const queryClient = useQueryClient();
  const paneActiveRef = usePaneActiveRef(usePaneActive()); // 标签不可见时暂停 2s 查证轮询（§3.6）
  const [query, setQuery] = useState({ page: 1, pageSize: 20, keyword: "", ymd: "", enterpriseId: undefined as number | undefined });
  const [importing, setImporting] = useState(false);
  const [exportOpen, setExportOpen] = useState(false);
  const [exportYmd, setExportYmd] = useState<string | undefined>(undefined);
  const [progressOpen, setProgressOpen] = useState(false);
  const [progressData, setProgressData] = useState({ total: 0, checked: 0 });
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null);
  // 「重新查证」：本批次进度弹窗（按返回的查证 batchId 轮询）
  const [retryStarting, setRetryStarting] = useState(false);
  const [batchProgressOpen, setBatchProgressOpen] = useState(false);
  const [batchProgress, setBatchProgress] = useState<CertCheckBatchProgress | null>(null);
  const [batchProgressYmd, setBatchProgressYmd] = useState<string>("");
  const [certPreview, setCertPreview] = useState<FilePreviewTarget | null>(null);

  // 证书文件可能是 PDF（上游 licensefile 实测为 %PDF base64），
  // 按文件元数据走 FilePreviewModal 自适应预览（pdf/图片均可）
  const openCertPreview = useCallback(async (licenseFileId: number | string) => {
    const fileId = String(licenseFileId);
    let fileName = "证书.pdf";
    try {
      const vo = await getPlatformFile(fileId);
      if (vo?.originalName) {
        fileName = vo.originalName;
      }
    } catch {
      // 元数据拿不到也照常预览（按下载流嗅探）
    }
    setCertPreview({ fileId, fileName });
  }, []);
  const batchPollRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const listQuery = useQuery({
    queryKey: ["chazheng-certs", query],
    queryFn: () => pageUsers(query),
  });

  const ymdQuery = useQuery({
    queryKey: ["chazheng-ymd-list"],
    queryFn: getYmdList,
  });

  const enterpriseQuery = useQuery({
    queryKey: ["chazheng-enterprises"],
    queryFn: getEnterprises,
  });

  const certMutation = useMutation({
    mutationFn: certCheck,
    onSuccess: (data) => {
      message.success("查证完成：" + data.userName);
      queryClient.invalidateQueries({ queryKey: ["chazheng-certs"] });
    },
    onError: (err: Error) => {
      message.error(err.message || "查证失败");
    },
  });

  const openProgress = useCallback(() => {
    setProgressOpen(true);
    setProgressData({ total: 0, checked: 0 });
    const poll = async () => {
      if (!paneActiveRef.current) return; // 标签不可见：跳过本轮请求，服务端查证照常进行
      try {
        const data = await getCertProgress();
        setProgressData(data);
        if (data.total > 0 && data.checked >= data.total) {
          if (pollRef.current) clearInterval(pollRef.current);
          pollRef.current = null;
          message.success("查证全部完成");
          queryClient.invalidateQueries({ queryKey: ["chazheng-certs"] });
        }
      } catch {
        // ignore poll errors
      }
    };
    poll();
    pollRef.current = setInterval(poll, 2000);
  }, [queryClient]);

  /** 本批次查证进度轮询：终态（completed/aborted）停止并刷新列表 */
  const startBatchPoll = useCallback(
    (batchId: number, ymd: string) => {
      if (batchPollRef.current) clearInterval(batchPollRef.current);
      setBatchProgressYmd(ymd);
      setBatchProgress(null);
      setBatchProgressOpen(true);
      const poll = async () => {
        if (!paneActiveRef.current) return; // 标签不可见：跳过本轮请求，服务端查证照常进行
        try {
          const data = await getCertBatchProgress(batchId);
          setBatchProgress(data);
          if (data.status !== "processing") {
            if (batchPollRef.current) clearInterval(batchPollRef.current);
            batchPollRef.current = null;
            queryClient.invalidateQueries({ queryKey: ["chazheng-certs"] });
            queryClient.invalidateQueries({ queryKey: ["chazheng-ymd-list"] });
            if (data.failed > 0) {
              message.warning(
                `重新查证结束：成功 ${data.success + data.successNoCert} 人，失败 ${data.failed} 人`,
              );
            } else {
              message.success(`重新查证完成：成功 ${data.success + data.successNoCert} 人`);
            }
          }
        } catch {
          // 轮询失败不打断，下一轮继续
        }
      };
      poll();
      batchPollRef.current = setInterval(poll, 2000);
    },
    [queryClient],
  );
  const handleImport = useCallback(
    async (file: File) => {
      setImporting(true);
      try {
        const result = await importUsers(file);
        message.success("导入成功：" + result.imported + " 条记录，批次 " + result.ymd);
        queryClient.invalidateQueries({ queryKey: ["chazheng-certs"] });
        queryClient.invalidateQueries({ queryKey: ["chazheng-ymd-list"] });

        try {
          const batch = await triggerCertCheckByYmd(result.ymd, file.name);
          message.success("批量查证已启动：" + batch.total + " 条");
          // 按本批 batchId 轮询（勿用全库 isChecked 进度，失败用户永远到不了 100%）
          startBatchPoll(batch.batchId, result.ymd);
          setQuery((prev) => ({ ...prev, ymd: result.ymd, page: 1 }));
        } catch (err: any) {
          message.error(err?.message || "批量查证启动失败");
        }
      } catch (err: any) {
        message.error(err?.message || "导入失败");
      } finally {
        setImporting(false);
      }
      return false;
    },
    [queryClient, startBatchPoll],
  );

  const handleDownloadTemplate = useCallback(async () => {
    try {
      const response = await downloadTemplate();
      const blob = new Blob([response.data], {
        type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "import_user_example.xlsx";
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
    } catch (err: any) {
      message.error("下载模板失败");
    }
  }, []);

  const closeProgress = useCallback(() => {
    setProgressOpen(false);
    if (pollRef.current) {
      clearInterval(pollRef.current);
      pollRef.current = null;
    }
  }, []);

  useEffect(() => {
    return () => {
      if (pollRef.current) clearInterval(pollRef.current);
      if (batchPollRef.current) clearInterval(batchPollRef.current);
    };
  }, []);

  const handleExport = useCallback(async () => {
    if (!exportYmd) {
      message.warning("请先选择要导出的批次");
      return;
    }
    try {
      const response = await exportUsers(exportYmd);
      const blob = new Blob([response.data], {
        type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "查证用户_" + exportYmd + ".xlsx";
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
      message.success("导出成功");
      setExportOpen(false);
    } catch (err: any) {
      message.error(err?.message || "导出失败");
    }
  }, [exportYmd]);

  const handleCert = useCallback(
    (record: ChazhengUser) => {
      Modal.confirm({
        title: "确认查证「" + record.userName + "」？",
        content: "将调用外部接口查验证书信息，可能耗时数秒。",
        okText: "确认查证",
        cancelText: "取消",
        onOk: () => certMutation.mutate(record.id),
      });
    },
    [certMutation],
  );


  const closeBatchProgress = useCallback(() => {
    setBatchProgressOpen(false);
    if (batchPollRef.current) {
      clearInterval(batchPollRef.current);
      batchPollRef.current = null;
    }
  }, []);

  /** 重新查证：仅重查所选批次中未成功（未查证/查证失败）的人员，确认后按本批进度轮询 */
  const handleRetryBatch = useCallback(async () => {
    const ymd = query.ymd;
    if (!ymd) {
      message.warning("请先选择批次");
      return;
    }
    setRetryStarting(true);
    try {
      const pending = await pageUsers({ ymd, isChecked: 0, page: 1, pageSize: 1 });
      const count = pending.total;
      if (!count) {
        message.info("该批次没有未成功的人员，无需重新查证");
        return;
      }
      Modal.confirm({
        title: "确认重新查证？",
        content: `批次 ${ymd}，共 ${count} 人未查证成功，将重新调用外部接口查证并覆盖其原有查证结果。`,
        okText: "开始重查",
        cancelText: "取消",
        onOk: async () => {
          try {
            const batch = await triggerCertCheckByYmd(ymd, undefined, true);
            message.success("重新查证已启动：" + batch.total + " 条");
            startBatchPoll(batch.batchId, ymd);
          } catch (err: any) {
            message.error(err?.message || "重新查证启动失败");
            throw err; // 保持弹窗，便于直接重试
          }
        },
      });
    } catch (err: any) {
      message.error(err?.message || "重查人数统计失败");
    } finally {
      setRetryStarting(false);
    }
  }, [query.ymd, startBatchPoll]);

  const renderCertContent = (certs: ChazhengCert[]) => {
    if (!certs || certs.length === 0)
      return <Typography.Text type="secondary">无证书</Typography.Text>;
    return (
      <Descriptions size="small" column={1} style={{ maxWidth: 400 }}>
        {certs.map((cert, idx) => (
          <Descriptions.Item key={idx} label={"证书 " + (idx + 1)}>
            编号: {cert.certNo || "-"}
            <br />
            名称: {cert.certName || "-"}
            <br />
            发证日期: {cert.certDate || "-"}
            {cert.licenseFileId ? (
              <>
                <br />
                <Button
                  type="link"
                  size="small"
                  style={{ padding: 0 }}
                  icon={<FileTextOutlined />}
                  onClick={() => void openCertPreview(cert.licenseFileId!)}
                >
                  查看证书
                </Button>
              </>
            ) : null}
          </Descriptions.Item>
        ))}
      </Descriptions>
    );
  };

  const items = listQuery.data?.items || [];
  const total = listQuery.data?.total || 0;
  const pct = progressData.total > 0 ? Math.round((progressData.checked / progressData.total) * 100) : 0;

  const columns = [
    {
      title: "姓名",
      dataIndex: "userName",
      width: 100,
      render: (text: string) => <Typography.Text strong>{text}</Typography.Text>,
    },
    { title: "身份证号", dataIndex: "idCard", width: 180 },
    { title: "企业ID", dataIndex: "enterpriseId", width: 90 },
    {
      title: "证书数量",
      dataIndex: "certs",
      width: 100,
      render: (certs: ChazhengCert[]) => {
        const count = certs?.length || 0;
        return count > 0 ? <Tag color="blue">{count} 个</Tag> : <Tag>无</Tag>;
      },
    },
    {
      title: "证书编号",
      dataIndex: "certs",
      width: 160,
      render: (certs: ChazhengCert[]) => {
        if (!certs || certs.length === 0) return "-";
        return (
          <Space size={2} wrap>
            {certs.map((c, i) => (
              <span key={i} style={{ display: "inline-flex", alignItems: "center", gap: 2 }}>
                <Tag color="geekblue">{c.certNo || "-"}</Tag>
                {c.licenseFileId ? (
                  <Tooltip title="查看证书文件（完整编号以证书为准）">
                    <Button
                      type="link"
                      size="small"
                      style={{ padding: 0, height: "auto" }}
                      icon={<FileTextOutlined />}
                      onClick={() => void openCertPreview(c.licenseFileId!)}
                    />
                  </Tooltip>
                ) : null}
              </span>
            ))}
          </Space>
        );
      },
    },
    {
      title: "证书名称",
      dataIndex: "certs",
      width: 200,
      render: (certs: ChazhengCert[]) => {
        if (!certs || certs.length === 0) return "-";
        return (
          <Space size={2} wrap>
            {certs.map((c, i) => (
              <Tag key={i} color="purple">{c.certName || "-"}</Tag>
            ))}
          </Space>
        );
      },
    },
    {
      title: "发证日期",
      dataIndex: "certs",
      width: 120,
      render: (certs: ChazhengCert[]) => {
        if (!certs || certs.length === 0) return "-";
        return (
          <Space size={2} direction="vertical">
            {certs.map((c, i) => (
              <Typography.Text key={i}>{c.certDate || "-"}</Typography.Text>
            ))}
          </Space>
        );
      },
    },
    {
      title: "查证状态",
      dataIndex: "isChecked",
      width: 100,
      render: (v: number) =>
        v === 1 ? <Tag color="processing">已查证</Tag> : <Tag>未查证</Tag>,
    },
    { title: "批次", dataIndex: "ymd", width: 150 },
    {
      title: "操作",
      width: 120,
      render: (_: any, record: ChazhengUser) => (
        <Button
          type="link"
          size="small"
          icon={<CheckCircleOutlined />}
          loading={certMutation.isPending}
          onClick={() => handleCert(record)}
        >
          查证
        </Button>
      ),
    },
  ];

  return (
    <div className="knowledge-page">
      <section className="page-heading knowledge-heading">
        <div>
          <div className="page-title-line">
            <span className="page-title-icon"><IdcardOutlined /></span>
            <Typography.Title level={1}>证书查询</Typography.Title>
          </div>
          <Typography.Text>导入查证用户，查看和管理证书信息，支持批量查证和导出。</Typography.Text>
        </div>
        <Space wrap>
          <Button
            icon={<ReloadOutlined />}
            onClick={() => {
              queryClient.invalidateQueries({ queryKey: ["chazheng-certs"] });
            }}
          >
            刷新
          </Button>
          <Upload
            accept=".xlsx,.xls"
            showUploadList={false}
            beforeUpload={handleImport}
            disabled={importing}
          >
            <Button icon={<CloudUploadOutlined />} loading={importing}>
              导入 Excel
            </Button>
          </Upload>
          <Button type="link" size="small" onClick={handleDownloadTemplate}>
            下载模板
          </Button>
          <Button icon={<DownloadOutlined />} onClick={() => setExportOpen(true)}>
            导出
          </Button>
          <Button icon={<DashboardOutlined />} onClick={openProgress}>
            查证进度
          </Button>
        </Space>
      </section>

      <div style={{ padding: "0 24px" }}>
        <Space style={{ marginBottom: 16 }} wrap>
          <Input
            prefix={<SearchOutlined />}
            placeholder="搜索姓名、身份证号"
            allowClear
            style={{ width: 260 }}
            value={query.keyword}
            onChange={(e) =>
              setQuery((prev) => ({ ...prev, keyword: e.target.value, page: 1 }))
            }
            onPressEnter={() =>
              queryClient.invalidateQueries({ queryKey: ["chazheng-certs"] })
            }
          />
          <Select
            placeholder="选择企业"
            allowClear
            style={{ width: 150 }}
            value={query.enterpriseId}
            onChange={(value: number | undefined) =>
              setQuery((prev) => ({ ...prev, enterpriseId: value, page: 1 }))
            }
            options={(enterpriseQuery.data || []).map((id: number) => ({
              label: "企业 " + id,
              value: id,
            }))}
          />
          <Select
            placeholder="选择批次"
            allowClear
            style={{ width: 180 }}
            value={query.ymd || undefined}
            onChange={(value: string | undefined) =>
              setQuery((prev) => ({ ...prev, ymd: value || "", page: 1 }))
            }
            options={(ymdQuery.data || []).map((ymd: string) => ({
              label: ymd,
              value: ymd,
            }))}
          />
          <Tooltip title={query.ymd ? undefined : "请先选择批次"}>
            <Button
              icon={<RedoOutlined />}
              loading={retryStarting}
              disabled={!query.ymd}
              onClick={handleRetryBatch}
            >
              重新查证
            </Button>
          </Tooltip>
          <Button
            icon={<SearchOutlined />}
            onClick={() =>
              queryClient.invalidateQueries({ queryKey: ["chazheng-certs"] })
            }
          >
            搜索
          </Button>
        </Space>
        <Table
          rowKey="id"
          size="middle"
          loading={listQuery.isLoading}
          dataSource={items}
          columns={columns}
          expandable={{
            expandedRowRender: (record: ChazhengUser) => renderCertContent(record.certs),
            rowExpandable: (record: ChazhengUser) =>
              record.certs && record.certs.length > 0,
          }}
          pagination={{
            current: query.page,
            pageSize: query.pageSize,
            total,
            showSizeChanger: true,
            showTotal: (t) => "共 " + t + " 条",
            onChange: (page, pageSize) =>
              setQuery((prev) => ({ ...prev, page, pageSize })),
          }}
        />
      </div>

      <FilePreviewModal
        open={!!certPreview}
        target={certPreview}
        onClose={() => setCertPreview(null)}
      />

      <Modal
        title="导出证书查询"
        open={exportOpen}
        onOk={handleExport}
        onCancel={() => setExportOpen(false)}
        okText="导出"
        cancelText="取消"
      >
        <Select
          placeholder="选择批次"
          allowClear
          style={{ width: "100%" }}
          value={exportYmd}
          onChange={(value: string | undefined) => setExportYmd(value)}
          options={(ymdQuery.data || []).map((ymd: string) => ({
            label: ymd,
            value: ymd,
          }))}
        />
      </Modal>

      <Modal
        title="查证进度"
        open={progressOpen}
        onCancel={closeProgress}
        footer={null}
      >
        <Progress percent={pct} format={() => progressData.checked + " / " + progressData.total} />
        {pct === 100 && <Typography.Text type="success">查证全部完成</Typography.Text>}
      </Modal>

      <Modal
        title={"重新查证进度" + (batchProgressYmd ? "（批次 " + batchProgressYmd + "）" : "")}
        open={batchProgressOpen}
        onCancel={closeBatchProgress}
        footer={null}
      >
        <Progress
          percent={batchProgress?.percent ?? 0}
          status={batchProgress && batchProgress.failed > 0 ? "exception" : undefined}
          format={() =>
            (batchProgress?.completed ?? 0) + " / " + (batchProgress?.total ?? 0)
          }
        />
        {batchProgress ? (
          <Space direction="vertical" size={2}>
            <Typography.Text>
              查证成功（有证）{batchProgress.success} 人 · 无证书{" "}
              {batchProgress.successNoCert} 人 · 失败 {batchProgress.failed} 人
            </Typography.Text>
            <Typography.Text type="secondary">
              已用时 {batchProgress.elapsed}
              {batchProgress.status === "processing"
                ? ` · 预计剩余 ${batchProgress.estimateRemaining}`
                : ""}
            </Typography.Text>
            {batchProgress.status === "aborted" && (
              <Typography.Text type="danger">
                批次因接口连续故障已中止，未完成的人员可再次点击「重新查证」续查。
              </Typography.Text>
            )}
          </Space>
        ) : (
          <Typography.Text type="secondary">正在获取进度…</Typography.Text>
        )}
      </Modal>
    </div>
  );
}
