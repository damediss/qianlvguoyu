import { jsx as _jsx, jsxs as _jsxs, Fragment as _Fragment } from "react/jsx-runtime";
import { CheckCircleOutlined, DeleteOutlined } from '@ant-design/icons';
import { Alert, Button, Space, Tag, Typography, message } from 'antd';
import { useCallback, useEffect, useRef, useState } from 'react';
import { uploadPlatformFile, fetchPlatformFileBlob } from '../../../api/files';
import { submitSignature } from '../../../api/certificationPracticalCollection';
import { getSignatureAdapter, hasAndroidSignatureBridge, } from '../../../services/signature/SignatureAdapterFactory';
import { isSignatureCancelled } from '../../../services/signature/AndroidSignatureAdapter';
import { formatBackendDateTime } from '../../../utils/dateTime';
import { sha256Blob } from '../../../utils/hash';
import './SignaturePad.css';
const MIN_CANVAS_WIDTH = 320;
const MIN_CANVAS_HEIGHT = 420;
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
function formatSignedAt(value) {
    if (!value) {
        return '-';
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return value;
    }
    return date.toLocaleString();
}
function shortHash(hash) {
    if (!hash) {
        return '-';
    }
    if (hash.length <= 16) {
        return hash;
    }
    return `${hash.slice(0, 10)}...${hash.slice(-6)}`;
}
export function SignaturePad({ taskId, candidate, disabled, disabledReason, onSubmitted, }) {
    const canvasRef = useRef(null);
    const canvasHostRef = useRef(null);
    const drawingRef = useRef(false);
    const lastPointRef = useRef(null);
    const hasSignatureRef = useRef(false);
    const thumbnailUrlRef = useRef(null);
    const [hasSignature, setHasSignature] = useState(false);
    const [submitting, setSubmitting] = useState(false);
    const [submittedRecord, setSubmittedRecord] = useState(null);
    const [thumbnailUrl, setThumbnailUrl] = useState();
    const latestRecord = submittedRecord || candidate.latestSignature || null;
    const isAlreadySigned = candidate.signatureStatus === 'DONE' || Boolean(candidate.latestSignature || submittedRecord);
    const isExceptionReleased = candidate.signatureStatus === 'EXCEPTION_RELEASED';
    const isLocked = Boolean(disabled || isAlreadySigned || isExceptionReleased || submitting);
    const isNativeSignature = hasAndroidSignatureBridge();
    const replaceThumbnailUrl = useCallback((url) => {
        if (thumbnailUrlRef.current) {
            URL.revokeObjectURL(thumbnailUrlRef.current);
        }
        thumbnailUrlRef.current = url || null;
        setThumbnailUrl(url);
    }, []);
    const resetCanvas = useCallback((markEmpty = true) => {
        const canvas = canvasRef.current;
        const host = canvasHostRef.current;
        if (!canvas || !host) {
            return;
        }
        const dpr = Math.max(window.devicePixelRatio || 1, 1);
        const width = Math.max(Math.floor(host.clientWidth || MIN_CANVAS_WIDTH), MIN_CANVAS_WIDTH);
        const height = Math.max(Math.floor(host.clientHeight || MIN_CANVAS_HEIGHT), MIN_CANVAS_HEIGHT);
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
        // 座次签到表会把签名缩进单格，线宽过细时复杂笔画易断、难辨认。
        ctx.lineWidth = 5.5;
        ctx.lineCap = 'round';
        ctx.lineJoin = 'round';
        if (markEmpty) {
            drawingRef.current = false;
            lastPointRef.current = null;
            hasSignatureRef.current = false;
            setHasSignature(false);
        }
    }, []);
    useEffect(() => {
        resetCanvas(true);
        const host = canvasHostRef.current;
        if (!host || typeof ResizeObserver === 'undefined') {
            const handleResize = () => resetCanvas(true);
            window.addEventListener('resize', handleResize);
            return () => window.removeEventListener('resize', handleResize);
        }
        const observer = new ResizeObserver(() => resetCanvas(true));
        observer.observe(host);
        return () => observer.disconnect();
    }, [resetCanvas]);
    useEffect(() => {
        setSubmittedRecord(null);
        replaceThumbnailUrl(undefined);
        resetCanvas(true);
    }, [candidate.candidateId, replaceThumbnailUrl, resetCanvas]);
    useEffect(() => {
        const signatureFileId = candidate.latestSignature?.signatureFileId;
        if (!signatureFileId) {
            return;
        }
        let cancelled = false;
        void fetchPlatformFileBlob(String(signatureFileId))
            .then((blob) => {
            if (cancelled) {
                return;
            }
            replaceThumbnailUrl(URL.createObjectURL(blob));
        })
            .catch(() => {
            if (!cancelled) {
                replaceThumbnailUrl(undefined);
            }
        });
        return () => {
            cancelled = true;
        };
    }, [candidate.latestSignature?.signatureFileId, replaceThumbnailUrl]);
    useEffect(() => () => {
        if (thumbnailUrlRef.current) {
            URL.revokeObjectURL(thumbnailUrlRef.current);
        }
    }, []);
    const markHasSignature = useCallback(() => {
        if (!hasSignatureRef.current) {
            hasSignatureRef.current = true;
            setHasSignature(true);
        }
    }, []);
    const drawPoint = useCallback((point) => {
        const canvas = canvasRef.current;
        if (!canvas) {
            return;
        }
        const ctx = getContext(canvas);
        ctx.beginPath();
        ctx.arc(point.x, point.y, 2.6, 0, Math.PI * 2);
        ctx.fillStyle = '#111827';
        ctx.fill();
        markHasSignature();
    }, [markHasSignature]);
    const drawLine = useCallback((from, to) => {
        const canvas = canvasRef.current;
        if (!canvas) {
            return;
        }
        const ctx = getContext(canvas);
        ctx.beginPath();
        ctx.moveTo(from.x, from.y);
        ctx.lineTo(to.x, to.y);
        ctx.stroke();
        markHasSignature();
    }, [markHasSignature]);
    const handlePointerDown = (event) => {
        if (isLocked) {
            return;
        }
        const canvas = canvasRef.current;
        if (!canvas) {
            return;
        }
        event.preventDefault();
        canvas.setPointerCapture(event.pointerId);
        const point = getPointerPoint(canvas, event);
        drawingRef.current = true;
        lastPointRef.current = point;
        drawPoint(point);
    };
    const handlePointerMove = (event) => {
        if (!drawingRef.current || isLocked) {
            return;
        }
        const canvas = canvasRef.current;
        const previous = lastPointRef.current;
        if (!canvas || !previous) {
            return;
        }
        event.preventDefault();
        const point = getPointerPoint(canvas, event);
        drawLine(previous, point);
        lastPointRef.current = point;
    };
    const finishDrawing = (event) => {
        if (!drawingRef.current) {
            return;
        }
        event.preventDefault();
        const canvas = canvasRef.current;
        if (canvas?.hasPointerCapture(event.pointerId)) {
            canvas.releasePointerCapture(event.pointerId);
        }
        drawingRef.current = false;
        lastPointRef.current = null;
    };
    const handleClear = () => {
        if (submitting) {
            return;
        }
        if (isExceptionReleased) {
            message.info('签名状态异常，不能清空重签');
            return;
        }
        if (isAlreadySigned) {
            message.info('已存在签名记录，本阶段不支持重新签名覆盖');
            return;
        }
        resetCanvas(true);
    };
    const handleConfirm = async () => {
        if (submitting) {
            return;
        }
        if (!candidate) {
            message.warning('当前考生不存在，无法提交签名');
            return;
        }
        if (disabled || candidate.overallStatus !== 'COLLECTING') {
            message.warning(disabledReason || '请先通过扫码确认当前考生');
            return;
        }
        if (isAlreadySigned || isExceptionReleased) {
            message.warning(disabledReason || '当前签名状态不允许重复提交');
            return;
        }
        if (!hasSignatureRef.current || !hasSignature) {
            message.warning('请考生在签名区域完成签名后再提交');
            return;
        }
        const canvas = canvasRef.current;
        if (!canvas) {
            message.error('签名画布未就绪，请刷新后重试');
            return;
        }
        setSubmitting(true);
        let stage = '签名提交';
        try {
            stage = '生成签名图片';
            const blob = await canvasToPngBlob(canvas);
            stage = '计算签名 Hash';
            const signatureHash = await sha256Blob(blob);
            stage = '上传签名图片';
            const file = new File([blob], `signature-${taskId}-${candidate.candidateId}-${Date.now()}.png`, { type: 'image/png' });
            const uploaded = await uploadPlatformFile(file, 'private');
            if (!uploaded.id) {
                throw new Error('签名图片上传成功但未返回文件 ID');
            }
            stage = '提交签名记录';
            const signedAt = formatBackendDateTime();
            const record = await submitSignature({
                taskId,
                candidateId: candidate.candidateId,
                sessionId: candidate.sessionId,
                examSessionId: candidate.examSessionId,
                signatureFileId: uploaded.id,
                signatureHash,
                signerType: 'CANDIDATE',
                signerName: candidate.name,
                signedAt,
                deviceInfo: buildDeviceInfo(),
            });
            setSubmittedRecord(record);
            replaceThumbnailUrl(URL.createObjectURL(blob));
            message.success('签名提交成功');
            onSubmitted?.(record);
        }
        catch (error) {
            const detail = error instanceof Error ? error.message : '请检查网络后重试';
            message.error(`${stage}失败：${detail}`);
        }
        finally {
            setSubmitting(false);
        }
    };
    const handleNativeConfirm = async () => {
        if (submitting) {
            return;
        }
        if (!candidate) {
            message.warning('当前考生不存在，无法提交签名');
            return;
        }
        if (disabled || candidate.overallStatus !== 'COLLECTING') {
            message.warning(disabledReason || '请先通过扫码确认当前考生');
            return;
        }
        if (isAlreadySigned || isExceptionReleased) {
            message.warning(disabledReason || '当前签名状态不允许重复提交');
            return;
        }
        setSubmitting(true);
        let stage = '原生签名';
        try {
            stage = '打开原生签名板';
            const result = await getSignatureAdapter().open(candidate);
            stage = '提交签名记录';
            const record = await submitSignature({
                taskId,
                candidateId: candidate.candidateId,
                sessionId: candidate.sessionId,
                examSessionId: candidate.examSessionId,
                signatureFileId: result.signatureFileId,
                signatureHash: result.signatureHash,
                signerType: result.signerType || 'CANDIDATE',
                signerName: result.signerName || candidate.name,
                signedAt: formatBackendDateTime(result.signedAt),
                deviceInfo: JSON.stringify(result.deviceInfo || { platform: 'android' }),
            });
            setSubmittedRecord(record);
            void fetchPlatformFileBlob(String(result.signatureFileId))
                .then((blob) => replaceThumbnailUrl(URL.createObjectURL(blob)))
                .catch(() => replaceThumbnailUrl(undefined));
            message.success('签名提交成功');
            onSubmitted?.(record);
        }
        catch (error) {
            if (isSignatureCancelled(error)) {
                message.info('已取消签名');
                return;
            }
            const detail = error instanceof Error ? error.message : '请检查网络后重试';
            message.error(`${stage}失败：${detail}`);
        }
        finally {
            setSubmitting(false);
        }
    };
    const statusText = isExceptionReleased
        ? '签名状态异常'
        : latestRecord
            ? '签名已提交'
            : '未签名';
    const statusColor = isExceptionReleased ? 'orange' : latestRecord ? 'success' : 'default';
    const resolvedDisabledReason = disabledReason
        || (isAlreadySigned ? '签名已提交，不支持重复签名' : undefined)
        || (isExceptionReleased ? '签名状态异常' : undefined);
    return (_jsxs("div", { className: "signature-pad", children: [_jsxs("div", { className: "signature-pad-header", children: [_jsxs("div", { children: [_jsx(Typography.Title, { level: 4, children: isNativeSignature ? 'Android 原生电子签名' : 'H5 电子签名' }), _jsx(Typography.Text, { type: "secondary", children: isNativeSignature ? '请考生在原生签名板完成签名' : '请考生使用手指或触控笔在下方区域签名' })] }), _jsx(Tag, { color: statusColor, children: statusText })] }), resolvedDisabledReason ? (_jsx(Alert, { type: isExceptionReleased ? 'warning' : 'info', showIcon: true, message: resolvedDisabledReason })) : null, isNativeSignature ? (_jsx("div", { className: "signature-pad-native-panel", children: _jsx(Button, { type: "primary", icon: _jsx(CheckCircleOutlined, {}), loading: submitting, disabled: isLocked, onClick: handleNativeConfirm, children: "\u7535\u5B50\u7B7E\u540D" }) })) : (_jsxs(_Fragment, { children: [_jsxs("div", { className: "signature-pad-canvas-host", ref: canvasHostRef, children: [_jsx("canvas", { ref: canvasRef, className: "signature-pad-canvas", "aria-label": "\u8003\u751F\u7535\u5B50\u7B7E\u540D\u753B\u5E03", "aria-disabled": isLocked, onPointerDown: handlePointerDown, onPointerMove: handlePointerMove, onPointerUp: finishDrawing, onPointerCancel: finishDrawing, onPointerLeave: finishDrawing }), !hasSignature && !latestRecord ? (_jsx("div", { className: "signature-pad-hint", children: "\u8BF7\u8003\u751F\u5728\u6B64\u5904\u7B7E\u540D" })) : null] }), _jsxs("div", { className: "signature-pad-actions", children: [_jsx(Button, { size: "small", danger: true, icon: _jsx(DeleteOutlined, {}), disabled: submitting || isAlreadySigned || isExceptionReleased, onClick: handleClear, children: "\u6E05\u7A7A\u91CD\u7B7E" }), _jsx(Button, { size: "small", type: "primary", icon: _jsx(CheckCircleOutlined, {}), loading: submitting, disabled: isLocked || !hasSignature, onClick: handleConfirm, children: "\u786E\u8BA4\u7B7E\u540D" })] })] })), latestRecord ? (_jsxs("div", { className: "signature-pad-result", children: [thumbnailUrl ? (_jsx("img", { className: "signature-pad-thumbnail", src: thumbnailUrl, alt: "\u7B7E\u540D\u7F29\u7565\u56FE" })) : null, _jsxs(Space, { direction: "vertical", size: 4, children: [_jsx(Typography.Text, { strong: true, children: latestRecord.signerName || candidate.name }), _jsxs(Typography.Text, { type: "secondary", children: ["\u7B7E\u540D\u65F6\u95F4\uFF1A", formatSignedAt(latestRecord.signedAt)] }), _jsxs(Typography.Text, { type: "secondary", children: ["\u7B7E\u540D Hash\uFF1A", shortHash(latestRecord.signatureHash)] })] })] })) : null] }));
}
