import { Dropdown, type MenuProps } from 'antd';
import { useEffect } from 'react';
import { createPortal } from 'react-dom';
import type { CompetitionStageContentType } from '../../../types/competition';

export const STAGE_CONTEXT_MENU_ITEMS = [
  {
    key: 'theory',
    label: '理论配置',
    children: [
      { key: 'theory:paper', label: '试卷配置' },
      { key: 'theory:candidate', label: '选手配置' },
    ],
  },
  { key: 'practical', label: '实操配置', children: null },
  { key: 'simulation', label: '仿真设置', children: null },
] as const;

function buildMenuItems(
  enabledContentTypes: readonly CompetitionStageContentType[] = [],
  candidateEnabled = true,
  onContentTypeClick?: (contentType: CompetitionStageContentType) => void,
): MenuProps['items'] {
  const enabled = new Set(enabledContentTypes);
  return STAGE_CONTEXT_MENU_ITEMS.map((item) => {
    const contentEnabled = enabled.has(item.key);
    return {
      key: item.key,
      label: item.label,
      disabled: !contentEnabled,
      // 理论配置是分组菜单，点击父级只负责展开，不再触发跳转。
      onTitleClick: item.key === 'theory'
        ? undefined
        : contentEnabled
          ? () => onContentTypeClick?.(item.key)
          : undefined,
      children: item.key === 'theory'
        ? [
          {
            key: 'theory:paper',
            label: '试卷配置',
            disabled: !contentEnabled,
          },
          {
            key: 'theory:candidate',
            label: '选手配置',
            disabled: !contentEnabled || !candidateEnabled,
          },
        ]
        : [{
          key: `${item.key}:empty`,
          label: '暂无配置项',
          disabled: true,
        }],
    };
  });
}

interface StageContextMenuProps {
  position: { x: number; y: number };
  enabledContentTypes?: readonly CompetitionStageContentType[];
  candidateEnabled?: boolean;
  onClose: () => void;
  onContentTypeClick?: (contentType: CompetitionStageContentType) => void;
  onCandidateClick?: () => void;
}

export function StageContextMenu({
  position,
  enabledContentTypes,
  candidateEnabled,
  onClose,
  onContentTypeClick,
  onCandidateClick,
}: StageContextMenuProps) {
  useEffect(() => {
    const closeOnPointerDown = (event: PointerEvent) => {
      const target = event.target;
      // antd renders submenu popups in a sibling portal, so they are not
      // descendants of the root popup carrying stage-context-menu-popup.
      if (target instanceof Element && target.closest('.stage-context-menu-popup, .ant-dropdown-menu-submenu-popup')) return;
      onClose();
    };
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose();
    };

    document.addEventListener('pointerdown', closeOnPointerDown, true);
    document.addEventListener('keydown', closeOnEscape, true);
    return () => {
      document.removeEventListener('pointerdown', closeOnPointerDown, true);
      document.removeEventListener('keydown', closeOnEscape, true);
    };
  }, [onClose]);

  return createPortal(
    <Dropdown
      open
      trigger={[]}
      menu={{
        items: buildMenuItems(enabledContentTypes, candidateEnabled, onContentTypeClick),
        onClick: ({ key }) => {
          if (key === 'theory:paper') onContentTypeClick?.('theory');
          if (key === 'theory:candidate') onCandidateClick?.();
        },
      }}
      overlayClassName="stage-context-menu-popup"
      getPopupContainer={() => document.body}
    >
      <span
        aria-hidden="true"
        className="stage-context-menu-anchor"
        style={{ position: 'fixed', left: position.x, top: position.y, width: 1, height: 1 }}
      />
    </Dropdown>,
    document.body,
  );
}
