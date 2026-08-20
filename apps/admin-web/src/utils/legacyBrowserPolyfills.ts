type PromiseWithResolversFn = <T>() => {
  promise: Promise<T>;
  resolve: (value: T | PromiseLike<T>) => void;
  reject: (reason?: unknown) => void;
};

type StructuredCloneFn = <T>(value: T, options?: { transfer?: object[] } | null) => T;

function isTransferableImage(value: object): boolean {
  return (
    (typeof HTMLCanvasElement !== 'undefined' && value instanceof HTMLCanvasElement) ||
    (typeof HTMLImageElement !== 'undefined' && value instanceof HTMLImageElement) ||
    (typeof SVGImageElement !== 'undefined' && value instanceof SVGImageElement) ||
    (typeof HTMLVideoElement !== 'undefined' && value instanceof HTMLVideoElement) ||
    (typeof OffscreenCanvas !== 'undefined' && value instanceof OffscreenCanvas) ||
    (typeof ImageBitmap !== 'undefined' && value instanceof ImageBitmap) ||
    (typeof ImageData !== 'undefined' && value instanceof ImageData) ||
    (typeof Blob !== 'undefined' && value instanceof Blob)
  );
}

function cloneStructured(value: unknown, seen: WeakMap<object, unknown>): unknown {
  if (value === null || typeof value !== 'object') {
    return value;
  }
  const objectValue = value as object;
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
    const view = value as ArrayBufferView & { constructor: new (buffer: ArrayBuffer) => ArrayBufferView };
    const buffer = view.buffer.slice(view.byteOffset, view.byteOffset + view.byteLength);
    return new view.constructor(buffer as ArrayBuffer);
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
    const cloned: unknown[] = [];
    seen.set(objectValue, cloned);
    for (let index = 0; index < value.length; index += 1) {
      cloned[index] = cloneStructured(value[index], seen);
    }
    return cloned;
  }
  const cloned: Record<string | symbol, unknown> = {};
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
  const promiseCtor = Promise as PromiseConstructor & { withResolvers?: PromiseWithResolversFn };
  if (typeof promiseCtor.withResolvers !== 'function') {
    promiseCtor.withResolvers = function withResolvers<T>() {
      let resolve!: (value: T | PromiseLike<T>) => void;
      let reject!: (reason?: unknown) => void;
      const promise = new Promise<T>((res, rej) => {
        resolve = res;
        reject = rej;
      });
      return { promise, resolve, reject };
    };
  }

  const globalRef = globalThis as typeof globalThis & { structuredClone?: StructuredCloneFn };
  if (typeof globalRef.structuredClone !== 'function') {
    globalRef.structuredClone = function structuredClone<T>(value: T) {
      return cloneStructured(value, new WeakMap()) as T;
    };
  }

  if (typeof Array.prototype.at !== 'function') {
    Object.defineProperty(Array.prototype, 'at', {
      configurable: true,
      writable: true,
      value(this: unknown[], index: number) {
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
      value(target: object, key: PropertyKey) {
        return Object.prototype.hasOwnProperty.call(target, key);
      },
    });
  }
}

installLegacyBrowserPolyfills();
