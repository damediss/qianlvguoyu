function isTransferableImage(value) {
    return ((typeof HTMLCanvasElement !== 'undefined' && value instanceof HTMLCanvasElement) ||
        (typeof HTMLImageElement !== 'undefined' && value instanceof HTMLImageElement) ||
        (typeof SVGImageElement !== 'undefined' && value instanceof SVGImageElement) ||
        (typeof HTMLVideoElement !== 'undefined' && value instanceof HTMLVideoElement) ||
        (typeof OffscreenCanvas !== 'undefined' && value instanceof OffscreenCanvas) ||
        (typeof ImageBitmap !== 'undefined' && value instanceof ImageBitmap) ||
        (typeof ImageData !== 'undefined' && value instanceof ImageData) ||
        (typeof Blob !== 'undefined' && value instanceof Blob));
}
function cloneStructured(value, seen) {
    if (value === null || typeof value !== 'object') {
        return value;
    }
    const objectValue = value;
    if (seen.has(objectValue)) {
        return seen.get(objectValue);
    }
    // PDF.js fake worker 会 structuredClone ImageBitmap/Canvas。
    // 旧浏览器没有原生 structuredClone，这里必须原样传引用，
    // 不能拷成普通对象，否则 ctx.drawImage 会报类型错误。
    if (isTransferableImage(objectValue)) {
        return value;
    }
    if (value instanceof Date) {
        return new Date(value.getTime());
    }
    if (value instanceof RegExp) {
        return new RegExp(value.source, value.flags);
    }
    if (typeof ArrayBuffer !== 'undefined' && value instanceof ArrayBuffer) {
        return value.slice(0);
    }
    if (typeof DataView !== 'undefined' && value instanceof DataView) {
        const buffer = value.buffer.slice(value.byteOffset, value.byteOffset + value.byteLength);
        return new DataView(buffer);
    }
    if (typeof ArrayBuffer !== 'undefined' && ArrayBuffer.isView(value)) {
        const view = value;
        const buffer = view.buffer.slice(view.byteOffset, view.byteOffset + view.byteLength);
        return new view.constructor(buffer);
    }
    if (value instanceof Map) {
        const cloned = new Map();
        seen.set(objectValue, cloned);
        value.forEach((entry, key) => {
            cloned.set(cloneStructured(key, seen), cloneStructured(entry, seen));
        });
        return cloned;
    }
    if (value instanceof Set) {
        const cloned = new Set();
        seen.set(objectValue, cloned);
        value.forEach((entry) => {
            cloned.add(cloneStructured(entry, seen));
        });
        return cloned;
    }
    if (Array.isArray(value)) {
        const cloned = [];
        seen.set(objectValue, cloned);
        for (let index = 0; index < value.length; index += 1) {
            cloned[index] = cloneStructured(value[index], seen);
        }
        return cloned;
    }
    const cloned = {};
    seen.set(objectValue, cloned);
    for (const key of Reflect.ownKeys(value)) {
        const descriptor = Object.getOwnPropertyDescriptor(value, key);
        if (!descriptor || !('value' in descriptor)) {
            continue;
        }
        cloned[key] = cloneStructured(descriptor.value, seen);
    }
    return cloned;
}
/** 平板 Via / 系统 WebView 为 Chrome 90，缺 Promise.withResolvers、structuredClone、Array.at。 */
export function installLegacyBrowserPolyfills() {
    const promiseCtor = Promise;
    if (typeof promiseCtor.withResolvers !== 'function') {
        promiseCtor.withResolvers = function withResolvers() {
            let resolve;
            let reject;
            const promise = new Promise((res, rej) => {
                resolve = res;
                reject = rej;
            });
            return { promise, resolve, reject };
        };
    }
    const globalRef = globalThis;
    if (typeof globalRef.structuredClone !== 'function') {
        globalRef.structuredClone = function structuredClone(value) {
            return cloneStructured(value, new WeakMap());
        };
    }
    if (typeof Array.prototype.at !== 'function') {
        Object.defineProperty(Array.prototype, 'at', {
            configurable: true,
            writable: true,
            value(index) {
                const length = this.length;
                const relative = Math.trunc(index) || 0;
                const k = relative >= 0 ? relative : length + relative;
                return k < 0 || k >= length ? undefined : this[k];
            },
        });
    }
    if (typeof Object.hasOwn !== 'function') {
        Object.defineProperty(Object, 'hasOwn', {
            configurable: true,
            writable: true,
            value(target, key) {
                return Object.prototype.hasOwnProperty.call(target, key);
            },
        });
    }
}
installLegacyBrowserPolyfills();
