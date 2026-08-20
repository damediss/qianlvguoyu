import { CheckCircleOutlined, DeleteOutlined } from '@ant-design/icons';
import { Button, Typography, message } from 'antd';
import { forwardRef, useCallback, useEffect, useImperativeHandle, useRef, useState } from 'react';
import type { PointerEvent as ReactPointerEvent } from 'react';
import { uploadPlatformFile } from '../../api/files';
import {
  hasAndroidSignatureBridge,
  isSignatureCancelled,
  openNativeSignaturePad,
} from '../../services/signature/AndroidSignatureAdapter';
import { sha256Blob } from '../../utils/hash';

interface Point {
  x: number;
  y: number;
}

export interface DefenseSignatureValue {
  fileId: string;
  hash: string;
}

export interface DefenseExpertSignatureHandle {
  ensureSignature: () => Promise<DefenseSignatureValue>;
}

interface Props {
  taskId?: string;
  signerName?: string;
  disabled?: boolean;
  previewUrl?: string;
  value?: DefenseSignatureValue | null;
  onChange?: (value: DefenseSignatureValue | null) => void;
}

function getContext(canvas: HTMLCanvasElement) {
  const ctx = canvas.getContext('2d');
  if (!ctx) {
    throw new Error('当前浏览器不支持 Canvas 签名');
  }
  return ctx;
}

function pointerPoint(canvas: HTMLCanvasElement, event: ReactPointerEvent<HTMLCanvasElement>): Point {
  const rect = canvas.getBoundingClientRect();
  return { x: event.clientX - rect.left, y: event.clientY - rect.top };
}

function canvasToPngBlob(canvas: HTMLCanvasElement) {
  return new Promise<Blob>((resolve, reject) => {
    canvas.toBlob((blob) => {
      if (!blob) {
        reject(new Error('签名图片生成失败，请重试'));
        return;
      }
      resolve(blob);
    }, 'image/png');
  });
}

export const DefenseExpertSignature = forwardRef<DefenseExpertSignatureHandle, Props>(function DefenseExpertSignature(
  { taskId, signerName, disabled, previewUrl, value, onChange },
  ref,
) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const hostRef = useRef<HTMLDivElement | null>(null);
  const drawingRef = useRef(false);
  const lastPointRef = useRef<Point | null>(null);
  const dirtyRef = useRef(false);
  const [dirty, setDirty] = useState(false);
  const isNative = hasAndroidSignatureBridge();

  const resetCanvas = useCallback(() => {
    const canvas = canvasRef.current;
    const host = hostRef.current;
    if (!canvas || !host) {
      return;
    }
    const dpr = Math.max(window.devicePixelRatio || 1, 1);
    const width = Math.max(Math.floor(host.clientWidth || 240), 240);
    const height = Math.max(Math.floor(host.clientHeight || 200), 200);
    canvas.style.width = `${width}px`;
    canvas.style.height = `${height}px`;
    canvas.width = Math.floor(width * dpr);
    canvas.height = Math.floor(height * dpr);
    const ctx = getContext(canvas);
    ctx.setTransform(1, 0, 0, 1, 0, 0);
    ctx.scale(dpr, dpr);
    ctx.fillStyle = '#ffffff';
    ctx.fillRect(0, 0, width, height);
    ctx.strokeStyle = '#111827';
    ctx.lineWidth = 2.4;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    dirtyRef.current = false;
    setDirty(false);
  }, []);

  useEffect(() => {
    if (disabled || isNative) {
      return;
    }
    resetCanvas();
    const handleResize = () => resetCanvas();
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, [disabled, isNative, resetCanvas]);

  const finishDrawing = () => {
    drawingRef.current = false;
    lastPointRef.current = null;
  };

  const handlePointerDown = (event: ReactPointerEvent<HTMLCanvasElement>) => {
    if (disabled) return;
    const canvas = canvasRef.current;
    if (!canvas) return;
    canvas.setPointerCapture(event.pointerId);
    drawingRef.current = true;
    lastPointRef.current = pointerPoint(canvas, event);
  };

  const handlePointerMove = (event: ReactPointerEvent<HTMLCanvasElement>) => {
    if (disabled || !drawingRef.current) return;
    const canvas = canvasRef.current;
    const last = lastPointRef.current;
    if (!canvas || !last) return;
    const next = pointerPoint(canvas, event);
    const ctx = getContext(canvas);
    ctx.beginPath();
    ctx.moveTo(last.x, last.y);
    ctx.lineTo(next.x, next.y);
    ctx.stroke();
    lastPointRef.current = next;
    if (!dirtyRef.current) {
      dirtyRef.current = true;
      setDirty(true);
      onChange?.(null);
    }
  };

  const uploadBlob = async (blob: Blob) => {
    const hash = await sha256Blob(blob);
    const file = new File([blob], `defense-expert-sign-${Date.now()}.png`, { type: 'image/png' });
    const uploaded = await uploadPlatformFile(file, 'private');
    if (!uploaded.id) {
      throw new Error('签名图片上传成功但未返回文件 ID');
    }
    const next = { fileId: String(uploaded.id), hash };
    onChange?.(next);
    return next;
  };

  useImperativeHandle(ref, () => ({
    async ensureSignature() {
      if (disabled && value?.fileId && value.hash) {
        return value;
      }
      if (value?.fileId && value.hash && value.hash.length === 64 && !dirtyRef.current) {
        return value;
      }
      if (isNative) {
        throw new Error('请先完成评审专家电子签名');
      }
      if (!dirtyRef.current || !canvasRef.current) {
        throw new Error('请先完成评审专家电子签名');
      }
      const blob = await canvasToPngBlob(canvasRef.current);
      return uploadBlob(blob);
    },
  }), [disabled, isNative, onChange, value]);

  const handleNativeSign = async () => {
    try {
      const result = await openNativeSignaturePad({
        taskId: taskId || 'defense',
        signerType: 'EVALUATOR',
        signerName: signerName || '评审专家',
      });
      const next = {
        fileId: String(result.signatureFileId),
        hash: result.signatureHash,
      };
      onChange?.(next);
      message.success('电子签名已完成');
    } catch (error) {
      if (isSignatureCancelled(error)) {
        message.info('已取消签名');
        return;
      }
      message.error(error instanceof Error ? error.message : '原生签字失败，请重试');
    }
  };

  if (previewUrl && !dirty) {
    return (
      <div className="defense-sign-preview">
        <img src={previewUrl} alt="评审专家电子签名" />
        <Typography.Text type="secondary">{signerName || '评审专家'}</Typography.Text>
        {disabled ? null : (
          <Button size="small" icon={<DeleteOutlined />} onClick={() => onChange?.(null)}>
            重签
          </Button>
        )}
      </div>
    );
  }

  if (isNative) {
    return (
      <div className="defense-sign-native">
        {value?.fileId ? <Typography.Text type="success">已完成电子签名</Typography.Text> : (
          <Typography.Text type="secondary">请点击下方按钮完成签字</Typography.Text>
        )}
        <Button type="primary" icon={<CheckCircleOutlined />} disabled={disabled} onClick={() => void handleNativeSign()}>
          {value?.fileId ? '重签' : '打开电子签名'}
        </Button>
      </div>
    );
  }

  return (
    <div className="defense-sign-pad">
      <div className="defense-sign-canvas-host" ref={hostRef}>
        <canvas
          ref={canvasRef}
          className="defense-sign-canvas"
          aria-label="评审专家电子签名"
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={finishDrawing}
          onPointerCancel={finishDrawing}
        />
        {!dirty ? <div className="defense-sign-hint">请在此签名</div> : null}
      </div>
      <div className="defense-sign-actions">
        <Button size="small" icon={<DeleteOutlined />} disabled={disabled} onClick={resetCanvas}>重签</Button>
      </div>
    </div>
  );
});
