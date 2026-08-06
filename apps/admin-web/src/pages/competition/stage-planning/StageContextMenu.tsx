import { Dropdown, type MenuProps } from 'antd';
import { useEffect } from 'react';
import { createPortal } from 'react-dom';
import type { CompetitionStageContentType } from '../../../types/competition';

export const STAGE_CONTEXT_MENU_ITEMS = [
  { key: 'theory', label: '理论配置', children: null },
  { key: 'practical', label: '实操配置', children: null },
  { key: 'simulation', label: '仿真设置', children: null },
] as const;

function buildMenuItems(
  enabledContentTypes: readonly CompetitionStageContentType[] = [],
  onContentTypeClick?: (contentType: CompetitionStageContentType) => void,
): MenuProps['items'] {
  const enabled = new Set(enabledContentTypes);
  return STAGE_CONTEXT_MENU_ITEMS.map((item) => ({
    key: item.key,
    label: item.label,
    disabled: !enabled.has(item.key),
    onTitleClick: enabled.has(item.key)
      ? () => onContentTypeClick?.(item.key)
      : undefined,
    children: item.children ?? [{
      key: `${item.key}:empty`,
      label: '暂无配置项',
      disabled: true,
    }],
  }));
}

interface StageContextMenuProps {
  position: { x: number; y: number };
  enabledContentTypes?: readonly CompetitionStageContentType[];
  onClose: () => void;
  onContentTypeClick?: (contentType: CompetitionStageContentType) => void;
}

export function StageContextMenu({
  position,
  enabledContentTypes,
  onClose,
  onContentTypeClick,
}: StageContextMenuProps) {
  useEffect(() => {
    const closeOnPointerDown = (event: PointerEvent) => {
      const target = event.target;
      if (target instanceof Element && target.closest('.stage-context-menu-popup')) return;
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
      menu={{ items: buildMenuItems(enabledContentTypes, onContentTypeClick) }}
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
