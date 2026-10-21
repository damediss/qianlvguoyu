import { jsx as _jsx, Fragment as _Fragment, jsxs as _jsxs } from "react/jsx-runtime";
import { ReloadOutlined } from '@ant-design/icons';
import { useCallback, useEffect, useRef, useState } from 'react';
import { generateCaptcha, verifyCaptcha } from '../api/auth';
import './SliderCaptcha.css';
const BG_WIDTH = 320;
const BG_HEIGHT = 160;
const PIECE_IMG_SIZE = 56; // 44 + 2*6 (bump radius)
export function SliderCaptcha({ onSuccess, onClose }) {
    const [captcha, setCaptcha] = useState(null);
    const [status, setStatus] = useState('loading');
    const [sliderX, setSliderX] = useState(0);
    const [failMsg, setFailMsg] = useState('');
    const trackRef = useRef(null);
    const draggingRef = useRef(false);
    const startXRef = useRef(0);
    const currentXRef = useRef(0);
    const maxSliderX = BG_WIDTH - PIECE_IMG_SIZE;
    const loadCaptcha = useCallback(async () => {
        setStatus('loading');
        setSliderX(0);
        setFailMsg('');
        try {
            const data = await generateCaptcha();
            setCaptcha(data);
            setStatus('ready');
        }
        catch {
            setFailMsg('加载验证码失败');
            setStatus('fail');
        }
    }, []);
    useEffect(() => {
        loadCaptcha();
    }, [loadCaptcha]);
    // ─── Drag handlers ──────────────────────────────────
    const handleDragStart = useCallback((clientX) => {
        if (status !== 'ready')
            return;
        draggingRef.current = true;
        startXRef.current = clientX;
        currentXRef.current = 0;
        setStatus('dragging');
    }, [status]);
    const handleDragMove = useCallback((clientX) => {
        if (!draggingRef.current)
            return;
        const delta = clientX - startXRef.current;
        const clamped = Math.max(0, Math.min(delta, maxSliderX));
        currentXRef.current = clamped;
        setSliderX(clamped);
    }, [maxSliderX]);
    const handleDragEnd = useCallback(async () => {
        if (!draggingRef.current || !captcha)
            return;
        draggingRef.current = false;
        const x = currentXRef.current;
        if (x < 5) {
            setStatus('ready');
            return;
        }
        setStatus('verifying');
        try {
            const result = await verifyCaptcha(captcha.captchaId, x);
            if (result.success && result.captchaToken) {
                setStatus('success');
                setTimeout(() => onSuccess(result.captchaToken), 400);
            }
            else {
                setStatus('fail');
                setFailMsg(result.message || '验证失败');
                setTimeout(() => loadCaptcha(), 800);
            }
        }
        catch {
            setStatus('fail');
            setFailMsg('验证请求失败');
            setTimeout(() => loadCaptcha(), 800);
        }
    }, [captcha, onSuccess, loadCaptcha]);
    // Mouse events
    useEffect(() => {
        const onMouseMove = (e) => handleDragMove(e.clientX);
        const onMouseUp = () => handleDragEnd();
        document.addEventListener('mousemove', onMouseMove);
        document.addEventListener('mouseup', onMouseUp);
        return () => {
            document.removeEventListener('mousemove', onMouseMove);
            document.removeEventListener('mouseup', onMouseUp);
        };
    }, [handleDragMove, handleDragEnd]);
    // Touch events
    useEffect(() => {
        const onTouchMove = (e) => {
            if (e.touches.length > 0)
                handleDragMove(e.touches[0].clientX);
        };
        const onTouchEnd = () => handleDragEnd();
        document.addEventListener('touchmove', onTouchMove, { passive: false });
        document.addEventListener('touchend', onTouchEnd);
        return () => {
            document.removeEventListener('touchmove', onTouchMove);
            document.removeEventListener('touchend', onTouchEnd);
        };
    }, [handleDragMove, handleDragEnd]);
    // ─── Render ─────────────────────────────────────────
    const trackColor = status === 'success' ? '#52c41a'
        : status === 'fail' ? '#f5222d'
            : status === 'dragging' ? '#1890ff'
                : '#e8e8e8';
    const trackIcon = status === 'success' ? '✓'
        : status === 'fail' ? '✗'
            : '→';
    const trackText = status === 'loading' ? '加载中...'
        : status === 'success' ? '验证成功'
            : status === 'fail' ? failMsg || '验证失败，请重试'
                : status === 'dragging' ? '松开鼠标进行验证'
                    : status === 'verifying' ? '验证中...'
                        : '向右拖动完成验证';
    return (_jsxs("div", { className: "slider-captcha", children: [_jsxs("div", { className: "slider-captcha__image", style: { width: BG_WIDTH, height: BG_HEIGHT }, children: [captcha && (_jsxs(_Fragment, { children: [_jsx("img", { src: captcha.bgImage, alt: "\u80CC\u666F", className: "slider-captcha__bg", draggable: false }), _jsx("img", { src: captcha.sliderImage, alt: "\u6ED1\u5757", className: "slider-captcha__piece", style: {
                                    left: sliderX,
                                    top: captcha.sliderY,
                                    width: PIECE_IMG_SIZE,
                                    height: PIECE_IMG_SIZE,
                                }, draggable: false })] })), status === 'loading' && (_jsx("div", { className: "slider-captcha__loading", children: "\u52A0\u8F7D\u4E2D..." })), _jsx("button", { className: "slider-captcha__refresh", onClick: loadCaptcha, title: "\u6362\u4E00\u5F20", disabled: status === 'dragging' || status === 'verifying' || status === 'loading', children: _jsx(ReloadOutlined, {}) })] }), _jsxs("div", { className: "slider-captcha__track", ref: trackRef, children: [_jsx("div", { className: "slider-captcha__track-fill", style: { width: sliderX, background: trackColor } }), _jsx("div", { className: "slider-captcha__track-text", children: trackText }), _jsx("div", { className: `slider-captcha__handle ${status === 'success' ? 'slider-captcha__handle--success' : ''} ${status === 'fail' ? 'slider-captcha__handle--fail' : ''}`, style: {
                            left: sliderX,
                            background: status === 'success' ? '#52c41a' : status === 'fail' ? '#f5222d' : '#fff',
                            borderColor: trackColor,
                        }, onMouseDown: (e) => {
                            e.preventDefault();
                            handleDragStart(e.clientX);
                        }, onTouchStart: (e) => {
                            if (e.touches.length > 0)
                                handleDragStart(e.touches[0].clientX);
                        }, children: _jsx("span", { className: "slider-captcha__handle-icon", children: trackIcon }) })] })] }));
}
