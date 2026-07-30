import { useState } from 'react';
import { Input, Popover, theme } from 'antd';
import { CURATED_ICONS, iconFromName } from '../utils/icons';
import * as Icons from '@ant-design/icons';

interface IconPickerProps {
  value?: string;
  onChange?: (value?: string) => void;
  placeholder?: string;
}

export function IconPicker({ value, onChange, placeholder }: IconPickerProps) {
  const [open, setOpen] = useState(false);
  const [filter, setFilter] = useState('');
  const { token } = theme.useToken();

  const filteredIcons = CURATED_ICONS.filter((name) =>
    name.toLowerCase().includes(filter.toLowerCase()),
  );

  const handleSelect = (iconName: string) => {
    onChange?.(iconName);
    setOpen(false);
    setFilter('');
  };

  const content = (
    <div style={{ width: 280 }}>
      <Input
        placeholder="搜索图标..."
        value={filter}
        onChange={(e) => setFilter(e.target.value)}
        allowClear
        style={{ marginBottom: 12 }}
      />
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(6, 1fr)',
          gap: 8,
          maxHeight: 280,
          overflowY: 'auto',
        }}
      >
        {filteredIcons.map((iconName) => {
          const isSelected = value === iconName;
          return (
            <div
              key={iconName}
              onClick={() => handleSelect(iconName)}
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                padding: 8,
                cursor: 'pointer',
                borderRadius: 6,
                border: `2px solid ${isSelected ? token.colorPrimary : 'transparent'}`,
                backgroundColor: isSelected ? token.colorPrimaryBg : 'transparent',
                transition: 'all 0.2s',
              }}
              onMouseEnter={(e) => {
                if (!isSelected) {
                  e.currentTarget.style.backgroundColor = token.colorBgTextHover;
                }
              }}
              onMouseLeave={(e) => {
                if (!isSelected) {
                  e.currentTarget.style.backgroundColor = 'transparent';
                }
              }}
            >
              <span style={{ fontSize: 20 }}>
                {iconFromName(iconName)}
              </span>
              <span
                style={{
                  fontSize: 10,
                  marginTop: 4,
                  textAlign: 'center',
                  wordBreak: 'break-all',
                  lineHeight: 1.2,
                }}
              >
                {iconName.replace('Outlined', '')}
              </span>
            </div>
          );
        })}
      </div>
      {filteredIcons.length === 0 && (
        <div style={{ textAlign: 'center', padding: 16, color: token.colorTextSecondary }}>
          未找到匹配的图标
        </div>
      )}
    </div>
  );

  return (
      <Popover
        content={content}
        trigger="click"
        open={open}
        onOpenChange={setOpen}
        placement="bottomLeft"
      >
        <Input
          readOnly
          value={value || ''}
          placeholder={placeholder}
          prefix={value ? iconFromName(value) : null}
          suffix={
            value ? (
              <span
                onClick={(e) => {
                  e.stopPropagation();
                  onChange?.(undefined);
                }}
                style={{ cursor: 'pointer', display: 'inline-flex', alignItems: 'center' }}
              >
                <Icons.CloseCircleOutlined />
              </span>
            ) : null
          }
          style={{ cursor: 'pointer' }}
        />
      </Popover>
  );
}
