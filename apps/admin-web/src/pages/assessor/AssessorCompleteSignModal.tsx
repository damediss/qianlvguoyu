import { CheckCircleOutlined, DeleteOutlined } from '@ant-design/icons';
import { Button, Modal, Space, Typography, message } from 'antd';
import { useCallback, useEffect, useRef, useState } from 'react';
import { uploadPlatformFile } from '../../api/files';
import { submitMyPracticalCompleteSign } from '../../api/practicalScoring';
import {
  hasAndroidSignatureBridge,
  isSignatureCancelled,
  openNativeSignaturePad,
} from '../../services/signature/AndroidSignatureAdapter';
import { sha256Blob } from '../../utils/hash';
import type { PracticalCompleteSign } from '../../types/practicalScoring';
import '../../components/certification/collection/SignaturePad.css';
import './AssessorCompleteSignModal.css';

const MIN_CANVAS_WIDTH = 640;
const MIN_CANVAS_HEIGHT = 360;
const MAX_CANVAS_HEIGHT = 640;

function resolveCanvasHeight(): number {
  return Math.max(
    MIN_CANVAS_HEIGHT,
    Math.min(Math.floor(window.innerHeight * 0.58), MAX_CANVAS_HEIGHT),
  );
}

interface Point {
  x: number;
  y: number;
}

interface Props {
  open: boolean;
  taskId?: string;
  signerName: string;
  signedCount: number;
  requiredCount: number;
  onSigned: (view: PracticalCompleteSign) => void;
}

function getContext(canvas: HTMLCanvasElement): CanvasRenderingContext2D {
  const ctx = canvas.getContext('2d');
  if (!ctx) {
    throw new Error('当前浏览器不支持 Canvas 签名');
  }
  return ctx;
}

function getPointerPoint(canvas: HTMLCanvasElement, event: React.PointerEvent<HTMLCanvasElement>): Point {
  const rect = canvas.getBoundingClientRect();
  return {
    x: event.clientX - rect.left,
    y: event.clientY - rect.top,
  };
}

function canvasToPngBlob(canvas: HTMLCanvasElement): Promise<Blob> {
  return new Promise((resolve, reject) => {
    canvas.toBlob((blob) => {
      if (!blob) {
        reject(new Error('签名图片生成失败，请重试'));
        return;
      }
      resolve(blob);
    }, 'image/png');
  });
}

function buildDeviceInfo() {
  return JSON.stringify({
    userAgent: navigator.userAgent,
    screenWidth: window.screen?.width,
    screenHeight: window.screen?.height,
    viewportWidth: window.innerWidth,
    viewportHeight: window.innerHeight,
    platform: navigator.platform,
    maxTouchPoints: navigator.maxTouchPoints,
    devicePixelRatio: window.devicePixelRatio || 1,
  });
}

export function AssessorCompleteSignModal({
  open,
  taskId,
  signerName,
  signedCount,
  requiredCount,
  onSigned,
}: Props) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const canvasHostRef = useRef<HTMLDivElement | null>(null);
  const drawingRef = useRef(false);
  const lastPointRef = useRef<Point | null>(null);
  const hasSignatureRef = useRef(false);
  const [hasSignature, setHasSignature] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const isNativeSignature = hasAndroidSignatureBridge();

  const resetCanvas = useCallback(() => {
    const canvas = canvasRef.current;
    const host = canvasHostRef.current;
    if (!canvas || !host) {
      return;
    }
    const dpr = Math.max(window.devicePixelRatio || 1, 1);
    const width = Math.max(Math.floor(host.clientWidth || MIN_CANVAS_WIDTH), MIN_CANVAS_WIDTH);
    const height = resolveCanvasHeight();
    host.style.minHeight = `${height}px`;
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
    ctx.lineWidth = 3;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    hasSignatureRef.current = false;
    setHasSignature(false);
  }, []);

  useEffect(() => {
    if (!open || isNativeSignature) {
      return;
    }
    const frame = window.requestAnimationFrame(() => resetCanvas());
    const handleResize = () => {
      if (hasSignatureRef.current) {
        return;
      }
      resetCanvas();
    };
    window.addEventListener('resize', handleResize);
    return () => {
      window.cancelAnimationFrame(frame);
      window.removeEventListener('resize', handleResize);
    };
  }, [isNativeSignature, open, resetCanvas]);

  const drawPoint = (point: Point) => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = getContext(canvas);
    ctx.beginPath();
    ctx.arc(point.x, point.y, 1.2, 0, Math.PI * 2);
    ctx.fillStyle = '#111827';
    ctx.fill();
    hasSignatureRef.current = true;
    setHasSignature(true);
  };

  const drawLine = (from: Point, to: Point) => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = getContext(canvas);
    ctx.beginPath();
    ctx.moveTo(from.x, from.y);
    ctx.lineTo(to.x, to.y);
    ctx.stroke();
    hasSignatureRef.current = true;
    setHasSignature(true);
  };

  const handlePointerDown = (event: React.PointerEvent<HTMLCanvasElement>) => {
    if (submitting) return;
    const canvas = canvasRef.current;
    if (!canvas) return;
    event.preventDefault();
    canvas.setPointerCapture(event.pointerId);
    const point = getPointerPoint(canvas, event);
    drawingRef.current = true;
    lastPointRef.current = point;
    drawPoint(point);
  };

  const handlePointerMove = (event: React.PointerEvent<HTMLCanvasElement>) => {
    if (!drawingRef.current || submitting) return;
    const canvas = canvasRef.current;
    const previous = lastPointRef.current;
    if (!canvas || !previous) return;
    event.preventDefault();
    const point = getPointerPoint(canvas, event);
    drawLine(previous, point);
    lastPointRef.current = point;
  };

  const finishDrawing = (event: React.PointerEvent<HTMLCanvasElement>) => {
    if (!drawingRef.current) return;
    event.preventDefault();
    const canvas = canvasRef.current;
    if (canvas?.hasPointerCapture(event.pointerId)) {
      canvas.releasePointerCapture(event.pointerId);
    }
    drawingRef.current = false;
    lastPointRef.current = null;
  };

  async function submitSignature(signatureFileId: string | number, signatureHash: string, deviceInfo?: string) {
    if (!taskId) {
      throw new Error('缺少认定任务');
    }
    return submitMyPracticalCompleteSign(taskId, { signatureFileId, signatureHash, deviceInfo });
  }

  const handleCanvasConfirm = async () => {
    if (submitting) return;
    if (!hasSignatureRef.current || !hasSignature) {
      message.warning('请在签名区域完成签字后再提交');
      return;
    }
    const canvas = canvasRef.current;
    if (!canvas) {
      message.error('签名画布未就绪，请刷新后重试');
      return;
    }
    setSubmitting(true);
    try {
      const blob = await canvasToPngBlob(canvas);
      const signatureHash = await sha256Blob(blob);
      const file = new File([blob], `assessor-complete-sign-${taskId}-${Date.now()}.png`, { type: 'image/png' });
      const uploaded = await uploadPlatformFile(file, 'private');
      if (!uploaded.id) {
        throw new Error('签名图片上传成功但未返回文件 ID');
      }
      const view = await submitSignature(uploaded.id, signatureHash, buildDeviceInfo());
      message.success('签字已提交');
      onSigned(view);
    } catch (error) {
      message.error(error instanceof Error ? error.message : '签字提交失败，请重试');
    } finally {
      setSubmitting(false);
    }
  };

  const handleNativeConfirm = async () => {
    if (submitting || !taskId) return;
    setSubmitting(true);
    try {
      const result = await openNativeSignaturePad({
        taskId,
        signerType: 'EVALUATOR',
        signerName,
      });
      const view = await submitSignature(
        result.signatureFileId,
        result.signatureHash,
        JSON.stringify(result.deviceInfo || { platform: 'android' }),
      );
      message.success('签字已提交');
      onSigned(view);
    } catch (error) {
      if (isSignatureCancelled(error)) {
        message.info('已取消签名');
        return;
      }
      message.error(error instanceof Error ? error.message : '原生签字失败，请重试');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      title="三名考评员签字确认"
      open={open}
      closable={false}
      maskClosable={false}
      keyboard={false}
      footer={null}
      centered
      width="92vw"
      wrapClassName="assessor-complete-sign-modal"
      destroyOnClose
    >
      <Space direction="vertical" size={16} style={{ width: '100%' }}>
        <Typography.Text className="assessor-complete-sign-intro">
          管理端已发起完成任务确认。请 {signerName || '考评员'} 在本机完成电子签名（{signedCount}/{requiredCount}）。
        </Typography.Text>
        <div className="signature-pad">
          {isNativeSignature ? (
            <div className="signature-pad-native-panel">
              <Button
                type="primary"
                size="large"
                icon={<CheckCircleOutlined />}
                loading={submitting}
                onClick={handleNativeConfirm}
              >
                打开电子签名
              </Button>
            </div>
          ) : (
            <>
              <div className="signature-pad-canvas-host" ref={canvasHostRef}>
                <canvas
                  ref={canvasRef}
                  className="signature-pad-canvas"
                  aria-label="考评员电子签名画布"
                  onPointerDown={handlePointerDown}
                  onPointerMove={handlePointerMove}
                  onPointerUp={finishDrawing}
                  onPointerCancel={finishDrawing}
                />
                {!hasSignature ? <div className="signature-pad-hint">请在此区域签字</div> : null}
              </div>
              <div className="signature-pad-actions">
                <Button icon={<DeleteOutlined />} disabled={submitting} onClick={resetCanvas}>
                  重签
                </Button>
                <Button
                  type="primary"
                  icon={<CheckCircleOutlined />}
                  loading={submitting}
                  onClick={handleCanvasConfirm}
                >
                  提交签字
                </Button>
              </div>
            </>
          )}
        </div>
      </Space>
    </Modal>
  );
}
