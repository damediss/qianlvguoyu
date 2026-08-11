import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { describe, expect, it, vi } from 'vitest';
import { StageContextMenu } from '../src/pages/competition/stage-planning/StageContextMenu';
import { StagePlanningCanvas } from '../src/pages/competition/stage-planning/StagePlanningCanvas';
import type { CompetitionStageContentType } from '../src/types/competition';
import type { StageNode } from '../src/types/competitionStagePlanning';

const preliminaryNode: StageNode = {
  id: 'stage:preliminary',
  type: 'stage',
  position: { x: 120, y: 120 },
  data: {
    kind: 'stage',
    stageType: 'preliminary',
    stageId: 5001,
    name: '初赛',
    sortOrder: 20,
    status: 'draft',
    contents: [{ contentType: 'theory', weight: 100 }],
  },
};

function renderCanvas(
  nodes: StageNode[],
  onStageContextMenu = vi.fn(),
  stageContentTypesByNodeId: Readonly<Record<string, readonly CompetitionStageContentType[]>> = {},
) {
  const onStageContentTypeClick = vi.fn();
  render(
    <div style={{ width: 900, height: 700 }}>
      <StagePlanningCanvas
        nodes={nodes}
        edges={[]}
        editable
        stagePlanConfirmed={false}
        onNodesChange={vi.fn()}
        onEdgesChange={vi.fn()}
        onConnect={vi.fn()}
        onSelectionChange={vi.fn()}
        onAddStage={vi.fn()}
        onStageContextMenu={onStageContextMenu}
        onStageContentTypeClick={onStageContentTypeClick}
        stageContentTypesByNodeId={stageContentTypesByNodeId}
        onFitViewReady={vi.fn()}
      />
    </div>,
  );
  return { onStageContextMenu, onStageContentTypeClick };
}

function ClosableContextMenu() {
  const [open, setOpen] = useState(true);
  return open ? (
    <StageContextMenu
      position={{ x: 120, y: 160 }}
      enabledContentTypes={['theory']}
      onClose={() => setOpen(false)}
    />
  ) : null;
}

describe('stage context menu', () => {
  it('opens from the React Flow node context-menu handler', async () => {
    const { onStageContextMenu } = renderCanvas([preliminaryNode]);

    fireEvent.contextMenu(await screen.findByTestId('stage-node-preliminary'), {
      clientX: 240,
      clientY: 280,
    });

    expect(await screen.findByText('理论配置')).toBeInTheDocument();
    expect(screen.getByText('实操配置')).toBeInTheDocument();
    expect(screen.getByText('仿真设置')).toBeInTheDocument();
    expect(onStageContextMenu).toHaveBeenCalledWith('stage:preliminary');
  });

  it('uses the selected stage workspace contents when the node has no local draft contents', async () => {
    const nodeWithoutLocalContents: StageNode = {
      ...preliminaryNode,
      data: { ...preliminaryNode.data, contents: undefined },
    };
    renderCanvas([nodeWithoutLocalContents], vi.fn(), { 'stage:preliminary': ['theory'] });

    fireEvent.contextMenu(await screen.findByTestId('stage-node-preliminary'), {
      clientX: 240,
      clientY: 280,
    });

    expect(await screen.findByText('理论配置')).toBeInTheDocument();
    expect(screen.getByText('理论配置').closest('li')).not.toHaveClass('ant-dropdown-menu-submenu-disabled');
    expect(screen.getByText('实操配置').closest('li')).toHaveClass('ant-dropdown-menu-submenu-disabled');
    expect(screen.getByText('仿真设置').closest('li')).toHaveClass('ant-dropdown-menu-submenu-disabled');
  });

  it('does not open for a marker node', async () => {
    const onStageContextMenu = vi.fn();
    renderCanvas([{
      id: 'marker:start',
      type: 'marker',
      position: { x: 0, y: 120 },
      data: { kind: 'marker', marker: 'start', label: '赛事开始' },
    }], onStageContextMenu);

    fireEvent.contextMenu(await screen.findByTestId('stage-marker-start'), {
      clientX: 120,
      clientY: 160,
    });

    expect(screen.queryByText('理论配置')).not.toBeInTheDocument();
    expect(onStageContextMenu).not.toHaveBeenCalled();
  });

  it('shows the disabled empty placeholder when an enabled entry is hovered', async () => {
    render(
      <StageContextMenu
        position={{ x: 120, y: 160 }}
        enabledContentTypes={['theory']}
        onClose={vi.fn()}
      />,
    );

    const user = userEvent.setup();
    const theory = await screen.findByText('理论配置');
    await user.hover(theory.closest('.ant-dropdown-menu-submenu-title') || theory);

    const placeholder = await screen.findByText('暂无配置项');
    expect(placeholder.closest('li')).toHaveAttribute('aria-disabled', 'true');
  });

  it('disables entries whose content is not selected for the stage', async () => {
    render(
      <StageContextMenu
        position={{ x: 120, y: 160 }}
        enabledContentTypes={['theory']}
        onClose={vi.fn()}
      />,
    );

    await screen.findByText('理论配置');
    expect(screen.getByText('理论配置').closest('li')).not.toHaveClass('ant-dropdown-menu-submenu-disabled');
    expect(screen.getByText('实操配置').closest('li')).toHaveClass('ant-dropdown-menu-submenu-disabled');
    expect(screen.getByText('仿真设置').closest('li')).toHaveClass('ant-dropdown-menu-submenu-disabled');
  });

  it('notifies the selected content action from the enabled submenu title', async () => {
    const onContentTypeClick = vi.fn();
    render(
      <StageContextMenu
        position={{ x: 120, y: 160 }}
        enabledContentTypes={['theory']}
        onClose={vi.fn()}
        onContentTypeClick={onContentTypeClick}
      />,
    );

    await screen.findByRole('menu');
    const theory = screen.getByRole('menu').querySelector('[data-menu-id$="-theory"]');
    if (!theory) throw new Error('theory menu item is missing');
    fireEvent.click(theory);

    expect(onContentTypeClick).toHaveBeenCalledWith('theory');
  });

  it('closes on Escape and outside pointer down', async () => {
    const { rerender } = render(<ClosableContextMenu />);
    expect(await screen.findByText('理论配置')).toBeInTheDocument();

    fireEvent.keyDown(document, { key: 'Escape' });
    await waitFor(() => expect(screen.queryByText('理论配置')).not.toBeInTheDocument());

    rerender(<ClosableContextMenu key="second" />);
    expect(await screen.findByText('理论配置')).toBeInTheDocument();
    fireEvent.pointerDown(document.body);
    await waitFor(() => expect(screen.queryByText('理论配置')).not.toBeInTheDocument());
  });
});
