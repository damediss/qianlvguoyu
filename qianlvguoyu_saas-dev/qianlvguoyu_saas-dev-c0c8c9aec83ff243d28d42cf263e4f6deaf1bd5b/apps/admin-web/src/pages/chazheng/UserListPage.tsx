import {
  DownloadOutlined,
  ReloadOutlined,
  SafetyCertificateOutlined,
  SearchOutlined,
  CheckCircleOutlined,
} from "@ant-design/icons";
import {
  Button,
  Input,
  Select,
  Space,
  Table,
  Tag,
  Typography,
  message,
  Modal,
} from "antd";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState, useCallback } from "react";
import {
  pageUsers,
  getYmdList,
  certCheck,
  exportUsers,
  getEnterprises,
} from "../../api/chazheng";
import type { ChazhengUser } from "../../types/chazheng";

const GENDER_MAP: Record<number, string> = { 0: "男", 1: "女" };
const GENDER_COLOR: Record<number, string> = { 0: "blue", 1: "pink" };

export function UserListPage() {
  const queryClient = useQueryClient();
  const [query, setQuery] = useState({ page: 1, pageSize: 20, keyword: "", ymd: "", enterpriseId: undefined as number | undefined });
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([]);

  const listQuery = useQuery({
    queryKey: ["chazheng-users", query],
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
      queryClient.invalidateQueries({ queryKey: ["chazheng-users"] });
    },
    onError: (err: Error) => {
      message.error(err.message || "查证失败");
    },
  });

  const handleExport = useCallback(async () => {
    try {
      const response = await exportUsers(query.ymd || undefined);
      const blob = new Blob([response.data], {
        type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "查证用户_" + (query.ymd || "全部") + ".xlsx";
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
      message.success("导出成功");
    } catch (err: any) {
      message.error(err?.message || "导出失败");
    }
  }, [query.ymd]);

  const handleExportSelected = useCallback(async () => {
    if (selectedRowKeys.length === 0) {
      message.warning("请先选择要导出的用户");
      return;
    }
    try {
      const ids = selectedRowKeys.map(Number);
      const response = await exportUsers(undefined, ids);
      const blob = new Blob([response.data], {
        type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
      });
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = "查证用户_已选.xlsx";
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
      message.success("导出成功");
      setSelectedRowKeys([]);
    } catch (err: any) {
      message.error(err?.message || "导出失败");
    }
  }, [selectedRowKeys]);

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

  const items = listQuery.data?.items || [];
  const total = listQuery.data?.total || 0;

  const columns = [
    {
      title: "姓名",
      dataIndex: "userName",
      render: (text: string) => <Typography.Text strong>{text}</Typography.Text>,
    },
    { title: "身份证号", dataIndex: "idCard", width: 180 },
    { title: "企业ID", dataIndex: "enterpriseId", width: 90 },
    {
      title: "性别",
      dataIndex: "gender",
      width: 70,
      render: (v: number | null) => {
        if (v == null) return "-";
        return <Tag color={GENDER_COLOR[v] || "default"}>{GENDER_MAP[v] || v}</Tag>;
      },
    },
    {
      title: "证书状态",
      dataIndex: "isCert",
      width: 90,
      render: (v: number) =>
        v === 1 ? <Tag color="success">有证书</Tag> : <Tag>无证书</Tag>,
    },
    {
      title: "查证状态",
      dataIndex: "isChecked",
      width: 90,
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
            <span className="page-title-icon"><SafetyCertificateOutlined /></span>
            <Typography.Title level={1}>用户列表</Typography.Title>
          </div>
          <Typography.Text>查询和管理用户信息，支持按姓名、身份证号、企业筛选。</Typography.Text>
        </div>
        <Space wrap>
          <Button
            icon={<ReloadOutlined />}
            onClick={() => {
              queryClient.invalidateQueries({ queryKey: ["chazheng-users"] });
            }}
          >
            刷新
          </Button>
          <Button icon={<DownloadOutlined />} onClick={handleExport}>
            导出
          </Button>
          <Button
            icon={<DownloadOutlined />}
            disabled={selectedRowKeys.length === 0}
            onClick={handleExportSelected}
          >
            导出选中 ({selectedRowKeys.length})
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
              queryClient.invalidateQueries({ queryKey: ["chazheng-users"] })
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
          <Button
            icon={<SearchOutlined />}
            onClick={() =>
              queryClient.invalidateQueries({ queryKey: ["chazheng-users"] })
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
          rowSelection={{
            selectedRowKeys,
            onChange: (keys) => setSelectedRowKeys(keys),
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
    </div>
  );
}
