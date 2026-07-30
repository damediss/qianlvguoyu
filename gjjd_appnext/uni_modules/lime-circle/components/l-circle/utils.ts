// @ts-nocheck
import { unitConvert } from '@/uni_modules/lime-shared/unitConvert';
import { isString } from '@/uni_modules/lime-shared/isString';

/**
 * 圆形信息接口
 */
interface CircleInfo {
    s: number;  // 圆形尺寸
    w: number;  // 线宽
    c: number;  // 中心位置
    r: number;  // 半径
}

/**
 * 获取圆形的基本信息
 * @param {number | string} size - 圆形直径
 * @param {number | string} lineWidth - 边框宽度
 * @returns {CircleInfo} 包含圆形尺寸信息的对象
 */
export function getCircle(size: number | string, lineWidth: number | string): CircleInfo {
    const s = unitConvert(size);
    const w = unitConvert(lineWidth);
    const c = (s - w) / 2;
    const r = s / 2 - w;
    return { s, w, c, r };
}

/**
 * 创建圆形遮罩样式
 * @param {number} radius - 遮罩半径
 * @returns {string} CSS径向渐变值
 */
export function getMaskStyle(radius: number = 0): string {
    return `radial-gradient(transparent ${radius - 0.5}px, #000 ${radius}px)`;
}

/**
 * 获取圆形进度条的样式配置
 * @param {string} name - 圆形标识名称
 * @param {number} size - 圆形直径
 * @param {number} percent - 当前进度百分比(0-100)
 * @param {number} gapDegree - 缺口角度
 * @param {string} gapPosition - 缺口位置('bottom', 'top', 'left', 'right')
 * @param {string | string[]} strokeColor - 进度条颜色(单色或渐变色数组)
 * @param {number} strokeWidth - 进度条宽度
 * @returns {Object} 圆形进度条的样式配置对象
 */
export function getCircleStyle(
    name: string,
    size: number,
    percent: number,
    gapDegree: number,
    gapPosition: string,
    strokeColor: string | string[],
    strokeWidth: number
): {
    color: string;
    [key: string]: string | number;
} {
    // 根据缺口位置计算起始角度
    const positionDeg = gapDegree === 0 ? 0 : {
        bottom: 0,
        top: 180,
        left: 90,
        right: -90,
    }[gapPosition] || 0;
    
    // 计算旋转角度
    const rotateDeg = gapDegree > 0 ? 90 + gapDegree / 2 : -90;
    const offsetDeg = 90;
    
    // 获取圆形基本信息
    const circle = getCircle(size, strokeWidth);
    
    // 计算进度条周长占比
    const perimeter = (360 - gapDegree) / 360 * percent * 100;
    const startDeg = positionDeg + rotateDeg + offsetDeg;
    const mask = getMaskStyle(circle.r);
    
    let background = '';
    let startColor = '';
    let endColor = '';
    
    // 处理单色情况
    if (isString(strokeColor) || !strokeColor) {
        // 使用CSS变量代替固定颜色
        const colorVar = `--l-circle-${name}-color`;
        startColor = `var(${colorVar})`;
        endColor = `var(${colorVar})`;
        background = `conic-gradient(from ${startDeg}deg, var(${colorVar}, var(--l-circle-${name}-base-color)) 0%, var(${colorVar}, var(--l-circle-${name}-base-color)) ${perimeter}%, transparent ${perimeter}%, transparent 100%)`;
    } 
    // 处理渐变色情况
    else if (Array.isArray(strokeColor)) {
        background = `conic-gradient(from ${startDeg}deg, transparent 0%,`;
        const len = strokeColor.length;
        
        for (let i = 0; i < len; i++) {
            // 使用带索引的CSS变量
            const colorVar = `--l-circle-${name}-color-${i + 1}`;
            const colorValue = strokeColor[i].startsWith('--') ? 
                `var(${strokeColor[i]})` :  // 如果已经是变量则直接使用
                `var(${colorVar})`;         // 否则使用自动生成的变量名
            
            if (i === 0) {
                background += `${colorValue} 0%,`;
                startColor = colorValue;
            } else {
                background += `${colorValue} ${perimeter * (i + 1) / len}%,`;
            }
            if (i === len - 1) {
                endColor = colorValue;
            }
        }
        background += `transparent ${perimeter}%, transparent 100%)`;
    }
    
	
    // 返回样式配置对象
	const style = {
        // color: startColor,
        [`--l-circle-${name}-cap-start`]: `${startDeg}deg`,
        // [`--l-circle-${name}-cap-color-end`]: endColor,
        [`--l-circle-${name}-cap-end`]: `${perimeter / 100 * 360 + startDeg}deg`,
        [`--l-circle-${name}-cap-size`]: `${strokeWidth / 2}px`,
        mask,
        '-webkit-mask': mask,
        '--l-background': background,
        // 添加颜色变量定义（如果传入的是固定颜色）
        ...(isString(strokeColor) && !strokeColor.startsWith('--') 
            ? { [`--l-circle-${name}-color`]: strokeColor } 
            : {}),
        ...(Array.isArray(strokeColor) 
            ? strokeColor.reduce((acc, color, index) => {
                if (!color.startsWith('--')) {
                    acc[`--l-circle-${name}-color-${index + 1}`] = color;
                }
                return acc;
            }, {} as Record<string, string>)
            : {})
    }
	
	if(name !== 'trail') {
		style[`--l-circle-${name}-cap-start-color`] = startColor
		style[`--l-circle-${name}-cap-end-color`] = endColor
	}
	
    return style
}