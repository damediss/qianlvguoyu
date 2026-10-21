import { jsx as _jsx, jsxs as _jsxs } from "react/jsx-runtime";
import { CheckCircleOutlined, DeleteOutlined } from '@ant-design/icons';
import { Button, Typography, message } from 'antd';
import { forwardRef, useCallback, useEffect, useImperativeHandle, useRef, useState } from 'react';
import { uploadPlatformFile } from '../../api/files';
import { hasAndroidSignatureBridge, isSignatureCancelled, openNativeSignaturePad, } from '../../services/signature/AndroidSignatureAdapter';
import { sha256Blob } from '../../utils/hash';
function getContext(canvas) {
    const ctx = canvas.getContext('2d');
    if (!ctx) {
        throw new Error('当前浏览器不支持 Canvas 签名');
    }
    return ctx;
}
function pointerPoint(canvas, event) {
    const rect = canvas.getBoundingClientRect();
    return { x: event.clientX - rect.left, y: event.clientY - rect.top };
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
export const DefenseExpertSignature = forwardRef(function DefenseExpertSignature({ taskId, signerName, disabled, previewUrl, value, onChange }, ref) {
    const canvasRef = useRef(null);
    const hostRef = useRef(null);
    const drawingRef = useRef(false);
    const lastPointRef = useRef(null);
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
    const handlePointerDown = (event) => {
        if (disabled)
            return;
        const canvas = canvasRef.current;
        if (!canvas)
            return;
        canvas.setPointerCapture(event.pointerId);
        drawingRef.current = true;
        lastPointRef.current = pointerPoint(canvas, event);
    };
    const handlePointerMove = (event) => {
        if (disabled || !drawingRef.current)
            return;
        const canvas = canvasRef.current;
        const last = lastPointRef.current;
        if (!canvas || !last)
            return;
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
    const uploadBlob = async (blob) => {
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
        }
        catch (error) {
            if (isSignatureCancelled(error)) {
                message.info('已取消签名');
                return;
            }
            message.error(error instanceof Error ? error.message : '原生签字失败，请重试');
        }
    };
    if (previewUrl && !dirty) {
        return (_jsxs("div", { className: "defense-sign-preview", children: [_jsx("img", { src: previewUrl, alt: "\u8BC4\u5BA1\u4E13\u5BB6\u7535\u5B50\u7B7E\u540D" }), _jsx(Typography.Text, { type: "secondary", children: signerName || '评审专家' }), disabled ? null : (_jsx(Button, { size: "small", icon: _jsx(DeleteOutlined, {}), onClick: () => onChange?.(null), children: "\u91CD\u7B7E" }))] }));
    }
    if (isNative) {
        return (_jsxs("div", { className: "defense-sign-native", children: [value?.fileId ? _jsx(Typography.Text, { type: "success", children: "\u5DF2\u5B8C\u6210\u7535\u5B50\u7B7E\u540D" }) : (_jsx(Typography.Text, { type: "secondary", children: "\u8BF7\u70B9\u51FB\u4E0B\u65B9\u6309\u94AE\u5B8C\u6210\u7B7E\u5B57" })), _jsx(Button, { type: "primary", icon: _jsx(CheckCircleOutlined, {}), disabled: disabled, onClick: () => void handleNativeSign(), children: value?.fileId ? '重签' : '打开电子签名' })] }));
    }
    return (_jsxs("div", { className: "defense-sign-pad", children: [_jsxs("div", { className: "defense-sign-canvas-host", ref: hostRef, children: [_jsx("canvas", { ref: canvasRef, className: "defense-sign-canvas", "aria-label": "\u8BC4\u5BA1\u4E13\u5BB6\u7535\u5B50\u7B7E\u540D", onPointerDown: handlePointerDown, onPointerMove: handlePointerMove, onPointerUp: finishDrawing, onPointerCancel: finishDrawing }), !dirty ? _jsx("div", { className: "defense-sign-hint", children: "\u8BF7\u5728\u6B64\u7B7E\u540D" }) : null] }), _jsx("div", { className: "defense-sign-actions", children: _jsx(Button, { size: "small", icon: _jsx(DeleteOutlined, {}), disabled: disabled, onClick: resetCanvas, children: "\u91CD\u7B7E" }) })] }));
});
