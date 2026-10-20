import { jsxs as _jsxs, jsx as _jsx, Fragment as _Fragment } from "react/jsx-runtime";
import { CheckCircleOutlined, DeleteOutlined } from '@ant-design/icons';
import { Button, Modal, Space, Typography, message } from 'antd';
import { useCallback, useEffect, useRef, useState } from 'react';
import { uploadPlatformFile } from '../../api/files';
import { submitMyPracticalCompleteSign } from '../../api/practicalScoring';
import { hasAndroidSignatureBridge, isSignatureCancelled, openNativeSignaturePad, } from '../../services/signature/AndroidSignatureAdapter';
import { sha256Blob } from '../../utils/hash';
import '../../components/certification/collection/SignaturePad.css';
import './AssessorCompleteSignModal.css';
const MIN_CANVAS_WIDTH = 640;
const MIN_CANVAS_HEIGHT = 360;
const MAX_CANVAS_HEIGHT = 640;
function resolveCanvasHeight() {
    return Math.max(MIN_CANVAS_HEIGHT, Math.min(Math.floor(window.innerHeight * 0.58), MAX_CANVAS_HEIGHT));
}
function getContext(canvas) {
    const ctx = canvas.getContext('2d');
    if (!ctx) {
        throw new Error('当前浏览器不支持 Canvas 签名');
    }
    return ctx;
}
function getPointerPoint(canvas, event) {
    const rect = canvas.getBoundingClientRect();
    return {
        x: event.clientX - rect.left,
        y: event.clientY - rect.top,
    };
}
function canvasToPngBlob(canvas) {
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
export function AssessorCompleteSignModal({ open, taskId, signerName, signedCount, requiredCount, onSigned, }) {
    const canvasRef = useRef(null);
    const canvasHostRef = useRef(null);
    const drawingRef = useRef(false);
    const lastPointRef = useRef(null);
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
    const drawPoint = (point) => {
        const canvas = canvasRef.current;
        if (!canvas)
            return;
        const ctx = getContext(canvas);
        ctx.beginPath();
        ctx.arc(point.x, point.y, 1.2, 0, Math.PI * 2);
        ctx.fillStyle = '#111827';
        ctx.fill();
        hasSignatureRef.current = true;
        setHasSignature(true);
    };
    const drawLine = (from, to) => {
        const canvas = canvasRef.current;
        if (!canvas)
            return;
        const ctx = getContext(canvas);
        ctx.beginPath();
        ctx.moveTo(from.x, from.y);
        ctx.lineTo(to.x, to.y);
        ctx.stroke();
        hasSignatureRef.current = true;
        setHasSignature(true);
    };
    const handlePointerDown = (event) => {
        if (submitting)
            return;
        const canvas = canvasRef.current;
        if (!canvas)
            return;
        event.preventDefault();
        canvas.setPointerCapture(event.pointerId);
        const point = getPointerPoint(canvas, event);
        drawingRef.current = true;
        lastPointRef.current = point;
        drawPoint(point);
    };
    const handlePointerMove = (event) => {
        if (!drawingRef.current || submitting)
            return;
        const canvas = canvasRef.current;
        const previous = lastPointRef.current;
        if (!canvas || !previous)
            return;
        event.preventDefault();
        const point = getPointerPoint(canvas, event);
        drawLine(previous, point);
        lastPointRef.current = point;
    };
    const finishDrawing = (event) => {
        if (!drawingRef.current)
            return;
        event.preventDefault();
        const canvas = canvasRef.current;
        if (canvas?.hasPointerCapture(event.pointerId)) {
            canvas.releasePointerCapture(event.pointerId);
        }
        drawingRef.current = false;
        lastPointRef.current = null;
    };
    async function submitSignature(signatureFileId, signatureHash, deviceInfo) {
        if (!taskId) {
            throw new Error('缺少认定任务');
        }
        return submitMyPracticalCompleteSign(taskId, { signatureFileId, signatureHash, deviceInfo });
    }
    const handleCanvasConfirm = async () => {
        if (submitting)
            return;
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
        }
        catch (error) {
            message.error(error instanceof Error ? error.message : '签字提交失败，请重试');
        }
        finally {
            setSubmitting(false);
        }
    };
    const handleNativeConfirm = async () => {
        if (submitting || !taskId)
            return;
        setSubmitting(true);
        try {
            const result = await openNativeSignaturePad({
                taskId,
                signerType: 'EVALUATOR',
                signerName,
            });
            const view = await submitSignature(result.signatureFileId, result.signatureHash, JSON.stringify(result.deviceInfo || { platform: 'android' }));
            message.success('签字已提交');
            onSigned(view);
        }
        catch (error) {
            if (isSignatureCancelled(error)) {
                message.info('已取消签名');
                return;
            }
            message.error(error instanceof Error ? error.message : '原生签字失败，请重试');
        }
        finally {
            setSubmitting(false);
        }
    };
    return (_jsx(Modal, { title: "\u4E09\u540D\u8003\u8BC4\u5458\u7B7E\u5B57\u786E\u8BA4", open: open, closable: false, maskClosable: false, keyboard: false, footer: null, centered: true, width: "92vw", wrapClassName: "assessor-complete-sign-modal", destroyOnClose: true, children: _jsxs(Space, { direction: "vertical", size: 16, style: { width: '100%' }, children: [_jsxs(Typography.Text, { className: "assessor-complete-sign-intro", children: ["\u7BA1\u7406\u7AEF\u5DF2\u53D1\u8D77\u5B8C\u6210\u4EFB\u52A1\u786E\u8BA4\u3002\u8BF7 ", signerName || '考评员', " \u5728\u672C\u673A\u5B8C\u6210\u7535\u5B50\u7B7E\u540D\uFF08", signedCount, "/", requiredCount, "\uFF09\u3002"] }), _jsx("div", { className: "signature-pad", children: isNativeSignature ? (_jsx("div", { className: "signature-pad-native-panel", children: _jsx(Button, { type: "primary", size: "large", icon: _jsx(CheckCircleOutlined, {}), loading: submitting, onClick: handleNativeConfirm, children: "\u6253\u5F00\u7535\u5B50\u7B7E\u540D" }) })) : (_jsxs(_Fragment, { children: [_jsxs("div", { className: "signature-pad-canvas-host", ref: canvasHostRef, children: [_jsx("canvas", { ref: canvasRef, className: "signature-pad-canvas", "aria-label": "\u8003\u8BC4\u5458\u7535\u5B50\u7B7E\u540D\u753B\u5E03", onPointerDown: handlePointerDown, onPointerMove: handlePointerMove, onPointerUp: finishDrawing, onPointerCancel: finishDrawing }), !hasSignature ? _jsx("div", { className: "signature-pad-hint", children: "\u8BF7\u5728\u6B64\u533A\u57DF\u7B7E\u5B57" }) : null] }), _jsxs("div", { className: "signature-pad-actions", children: [_jsx(Button, { icon: _jsx(DeleteOutlined, {}), disabled: submitting, onClick: resetCanvas, children: "\u91CD\u7B7E" }), _jsx(Button, { type: "primary", icon: _jsx(CheckCircleOutlined, {}), loading: submitting, onClick: handleCanvasConfirm, children: "\u63D0\u4EA4\u7B7E\u5B57" })] })] })) })] }) }));
}
