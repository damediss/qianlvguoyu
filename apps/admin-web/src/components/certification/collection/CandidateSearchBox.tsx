import { SearchOutlined, ScanOutlined } from '@ant-design/icons';
import { Button, Empty, Input, List, Space, Typography, message } from 'antd';
import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { matchByToken, searchCandidate } from '../../../api/certificationPracticalCollection';
import { getScanAdapter } from '../../../services/scan/ScanAdapterFactory';
import type { CandidateMatchVO, SnowflakeId } from '../../../types/certificationPracticalCollection';

interface CandidateSearchBoxProps {
  taskId: SnowflakeId;
  examSessionId?: SnowflakeId;
  onCandidateMatched: (candidate: CandidateMatchVO) => void;
  onCandidatesFound: (candidates: CandidateMatchVO[]) => void;
  onAfterStatusChanged?: () => void;
}

export function CandidateSearchBox({
  taskId,
  examSessionId,
  onCandidateMatched,
  onCandidatesFound,
  onAfterStatusChanged,
}: CandidateSearchBoxProps) {
  const [token, setToken] = useState('');
  const [keyword, setKeyword] = useState('');
  const [candidates, setCandidates] = useState<CandidateMatchVO[]>([]);

  const tokenMutation = useMutation({
    mutationFn: (collectToken: string) => matchByToken({ taskId, examSessionId, collectToken }),
    onSuccess: (candidate) => {
      message.success(candidate.currentStatus === 'COMPLETED' ? '该考生已完成采集' : '已匹配考生');
      setCandidates([candidate]);
      onCandidateMatched(candidate);
      onAfterStatusChanged?.();
    },
  });

  const searchMutation = useMutation({
    mutationFn: (value: string) => searchCandidate({ taskId, examSessionId, keyword: value }),
    onSuccess: (items) => {
      setCandidates(items);
      onCandidatesFound(items);
      if (items.length === 1) {
        onCandidateMatched(items[0]);
      } else if (items.length === 0) {
        message.info('未找到匹配考生，请更换 token、后四位、姓名或准考证号');
      }
    },
  });

  const scanMutation = useMutation({
    mutationFn: async () => getScanAdapter().startScan(),
    onSuccess: (result) => {
      if (!result.success || !result.token) {
        message.warning(result.message || '未获取到扫码 token');
        return;
      }
      setToken(result.token);
      tokenMutation.mutate(result.token);
    },
  });

  const handleTokenMatch = () => {
    const value = token.trim();
    if (!value) {
      message.warning('请输入采集二维码 token');
      return;
    }
    tokenMutation.mutate(value);
  };

  const handleSearch = () => {
    const value = keyword.trim();
    if (!value) {
      message.warning('请输入身份证后四位、姓名或准考证号');
      return;
    }
    searchMutation.mutate(value);
  };

  const loading = tokenMutation.isPending || searchMutation.isPending || scanMutation.isPending;

  return (
    <div className="candidate-search-box">
      <div className="candidate-search-actions">
        <Button
          type="primary"
          icon={<ScanOutlined />}
          loading={scanMutation.isPending || tokenMutation.isPending}
          onClick={() => scanMutation.mutate()}
        >
          扫码匹配
        </Button>
        <Input.Search
          allowClear
          value={token}
          size="large"
          placeholder="手动输入二维码 token"
          enterButton="匹配 token"
          onChange={(event) => setToken(event.target.value)}
          onSearch={handleTokenMatch}
          loading={tokenMutation.isPending}
        />
        <Input.Search
          allowClear
          value={keyword}
          size="large"
          prefix={<SearchOutlined />}
          placeholder="身份证后四位 / 姓名 / 姓名+后四位 / 准考证号"
          enterButton="搜索考生"
          onChange={(event) => setKeyword(event.target.value)}
          onSearch={handleSearch}
          loading={searchMutation.isPending}
        />
      </div>

      {candidates.length > 1 ? (
        <div className="candidate-result-list">
          <Typography.Text type="secondary">找到 {candidates.length} 名候选，请选择确认</Typography.Text>
          <List
            dataSource={candidates}
            loading={loading}
            locale={{ emptyText: <Empty description="暂无候选" /> }}
            renderItem={(candidate) => (
              <List.Item
                className="candidate-result-item"
                onClick={() => onCandidateMatched(candidate)}
                actions={[
                  <Button key="select" type="link" onClick={() => onCandidateMatched(candidate)}>
                    确认
                  </Button>,
                ]}
              >
                <Space size={16}>
                  <strong>{candidate.name}</strong>
                  <span>后四位 {candidate.idCardLast4 || '-'}</span>
                  <span>{candidate.unitName || '-'}</span>
                  <span>{candidate.admissionNo || '-'}</span>
                </Space>
              </List.Item>
            )}
          />
        </div>
      ) : null}
    </div>
  );
}
