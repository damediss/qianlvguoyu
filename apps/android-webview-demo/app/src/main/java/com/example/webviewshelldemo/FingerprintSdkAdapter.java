package com.example.webviewshelldemo;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import com.fpreader.fpdevice.AsyncUsbReader;
import com.fpreader.fpdevice.UseManager;
import com.fpreader.fpdevice.UsbReader;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class FingerprintSdkAdapter {
    public interface ResultCallback {
        void onComplete(FingerprintCapturePayload payload);
    }

    public interface ImageResultCallback {
        void onComplete(FingerprintImageCaptureResult result);
    }

    public interface StatusListener {
        void onStatus(FingerprintCapturePayload payload);
    }

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_DEVICE_READY = "DEVICE_READY";
    public static final String STATUS_DEVICE_NOT_FOUND = "FINGERPRINT_DEVICE_NOT_FOUND";
    public static final String STATUS_USB_PERMISSION_DENIED = "USB_PERMISSION_DENIED";
    public static final String STATUS_USB_PERMISSION_REQUIRED = "USB_PERMISSION_REQUIRED";
    public static final String STATUS_DEVICE_OPEN_FAILED = "DEVICE_OPEN_FAILED";
    public static final String STATUS_CAPTURE_TIMEOUT = "CAPTURE_TIMEOUT";
    public static final String STATUS_CAPTURE_FAILED = "CAPTURE_FAILED";
    public static final String STATUS_IMAGE_READ_FAILED = "FINGERPRINT_IMAGE_READ_FAILED";
    public static final String STATUS_CAPTURE_CANCELLED = "FINGERPRINT_CAPTURE_CANCELLED";
    public static final String STATUS_DEVICE_DETACHED = "DEVICE_DETACHED";
    public static final String STATUS_CAPTURE_IN_PROGRESS = "CAPTURE_IN_PROGRESS";
    public static final String STATUS_SDK_EXCEPTION = "SDK_EXCEPTION";

    private static final long CAPTURE_TIMEOUT_MS = 15_000L;
    private static final long CAPTURE_RETRY_DELAY_MS = 250L;
    private static final int CAPTURE_MODE_METADATA = 0;
    private static final int CAPTURE_MODE_IMAGE = 1;

    private final Context context;
    private final UsbManager usbManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US);
    private final String usbPermissionAction;

    private AsyncUsbReader deviceReader;
    private AsyncUsbReader captureReader;
    private PendingIntent permissionIntent;
    private UsbDevice selectedDevice;
    private boolean receiverRegistered;
    private boolean captureInProgress;
    private boolean openedForActiveCapture;
    private long activeStartElapsedMs;
    private int activeCaptureMode = CAPTURE_MODE_METADATA;
    private FingerprintCapturePayload activePayload;
    private ResultCallback activeCaptureCallback;
    private ImageResultCallback activeImageCaptureCallback;
    private ResultCallback permissionOnlyCallback;
    private StatusListener statusListener;

    private final Runnable captureTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (!captureInProgress) {
                return;
            }
            stopSdkWork();
            finishActiveCapture(false, STATUS_CAPTURE_TIMEOUT, STATUS_CAPTURE_TIMEOUT,
                    "Fingerprint capture timed out.");
        }
    };

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context receiverContext, Intent intent) {
            String action = intent == null ? null : intent.getAction();
            if (usbPermissionAction.equals(action) || AsyncUsbReader.ACTION_USB_PERMISSION.equals(action)) {
                handleUsbPermissionResult(intent);
                return;
            }
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                selectedDevice = getUsbDevice(intent);
                emitStatus(checkDevice());
                return;
            }
            if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice detachedDevice = getUsbDevice(intent);
                if (selectedDevice == null || sameDevice(selectedDevice, detachedDevice)) {
                    selectedDevice = null;
                    if (captureInProgress) {
                        stopSdkWork();
                        finishActiveCapture(false, STATUS_DEVICE_DETACHED, STATUS_DEVICE_DETACHED,
                                "Fingerprint device was detached during capture.");
                    } else {
                        closeDevice();
                        emitStatus(failurePayload(null, STATUS_DEVICE_DETACHED, STATUS_DEVICE_DETACHED,
                                "Fingerprint device detached."));
                    }
                }
            }
        }
    };

    public FingerprintSdkAdapter(Context context) {
        this.context = context;
        this.usbManager = (UsbManager) this.context.getSystemService(Context.USB_SERVICE);
        this.usbPermissionAction = this.context.getPackageName() + ".fingerprint.USB_PERMISSION";
    }

    public void setStatusListener(StatusListener statusListener) {
        this.statusListener = statusListener;
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    public void start() {
        if (deviceReader == null) {
            deviceReader = UseManager.getInstance().getAsyncUsbReader(context);
            captureReader = UseManager.getInstance().getAsyncUsbReader(context);
            configureSdkCallbacks();
        }
        if (receiverRegistered) {
            return;
        }

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            flags |= PendingIntent.FLAG_MUTABLE;
        }
        Intent permissionIntentValue = new Intent(usbPermissionAction).setPackage(context.getPackageName());
        permissionIntent = PendingIntent.getBroadcast(context, 0, permissionIntentValue, flags);

        IntentFilter filter = new IntentFilter(usbPermissionAction);
        filter.addAction(AsyncUsbReader.ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            context.registerReceiver(usbReceiver, filter);
        }
        receiverRegistered = true;
        deviceReader.ResumeRegister(context);
    }

    public FingerprintCapturePayload checkDevice() {
        start();
        FingerprintCapturePayload payload = startedPayload(null);
        selectedDevice = findCandidateDevice();
        applyDeviceInfo(payload, selectedDevice);
        if (selectedDevice == null) {
            finishPayload(payload, false, STATUS_DEVICE_NOT_FOUND, STATUS_DEVICE_NOT_FOUND,
                    "No USB fingerprint device detected.", 0L);
            return payload;
        }
        if (!hasUsbPermission(selectedDevice)) {
            finishPayload(payload, false, STATUS_USB_PERMISSION_REQUIRED, STATUS_USB_PERMISSION_REQUIRED,
                    "USB permission is required before capture.", 0L);
            return payload;
        }
        payload.deviceSn = readAndroidUsbSerial(selectedDevice);
        if (UseManager.getInstance().IsOpen()) {
            readSdkDeviceFields(payload);
        }
        finishPayload(payload, true, STATUS_DEVICE_READY, null, null, 0L);
        return payload;
    }

    public void requestUsbPermission(ResultCallback callback) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                start();
                if (captureInProgress || permissionOnlyCallback != null) {
                    callback.onComplete(failurePayload(null, STATUS_CAPTURE_IN_PROGRESS,
                            STATUS_CAPTURE_IN_PROGRESS, "Another fingerprint operation is in progress."));
                    return;
                }
                selectedDevice = findCandidateDevice();
                if (selectedDevice == null) {
                    callback.onComplete(failurePayload(null, STATUS_DEVICE_NOT_FOUND,
                            STATUS_DEVICE_NOT_FOUND, "No USB fingerprint device detected."));
                    return;
                }
                if (hasUsbPermission(selectedDevice)) {
                    FingerprintCapturePayload payload = startedPayload(null);
                    applyDeviceInfo(payload, selectedDevice);
                    payload.deviceSn = readAndroidUsbSerial(selectedDevice);
                    finishPayload(payload, true, STATUS_DEVICE_READY, null, null, 0L);
                    callback.onComplete(payload);
                    return;
                }
                permissionOnlyCallback = callback;
                requestPermissionForSelectedDevice();
            }
        });
    }

    public FingerprintCapturePayload openDevice() {
        start();
        FingerprintCapturePayload payload = startedPayload(null);
        selectedDevice = findCandidateDevice();
        applyDeviceInfo(payload, selectedDevice);
        if (selectedDevice == null) {
            finishPayload(payload, false, STATUS_DEVICE_NOT_FOUND, STATUS_DEVICE_NOT_FOUND,
                    "No USB fingerprint device detected.", 0L);
            return payload;
        }
        if (!hasUsbPermission(selectedDevice)) {
            finishPayload(payload, false, STATUS_USB_PERMISSION_REQUIRED, STATUS_USB_PERMISSION_REQUIRED,
                    "USB permission is required before opening the device.", 0L);
            return payload;
        }
        int result;
        try {
            result = UseManager.getInstance().OpenDevice();
        } catch (RuntimeException ex) {
            finishPayload(payload, false, STATUS_SDK_EXCEPTION, STATUS_SDK_EXCEPTION,
                    "OpenDevice failed: " + ex.getClass().getSimpleName(), 0L);
            return payload;
        }
        if (result != 0) {
            finishPayload(payload, false, STATUS_DEVICE_OPEN_FAILED, String.valueOf(result),
                    sdkErrorToString(result), 0L);
            return payload;
        }
        payload.deviceSn = readAndroidUsbSerial(selectedDevice);
        readSdkDeviceFields(payload);
        finishPayload(payload, true, STATUS_DEVICE_READY, null, null, 0L);
        return payload;
    }

    public FingerprintCapturePayload closeDevice() {
        FingerprintCapturePayload payload = startedPayload(null);
        selectedDevice = findCandidateDevice();
        applyDeviceInfo(payload, selectedDevice);
        int result;
        try {
            result = UseManager.getInstance().CloseDevice();
        } catch (RuntimeException ex) {
            finishPayload(payload, false, STATUS_SDK_EXCEPTION, STATUS_SDK_EXCEPTION,
                    "CloseDevice failed: " + ex.getClass().getSimpleName(), 0L);
            return payload;
        }
        finishPayload(payload, result == 0, result == 0 ? STATUS_DEVICE_READY : STATUS_SDK_EXCEPTION,
                result == 0 ? null : String.valueOf(result),
                result == 0 ? null : sdkErrorToString(result), 0L);
        return payload;
    }

    public void collectOnce(String requestId, ResultCallback callback) {
        beginCapture(requestId, callback, null, CAPTURE_MODE_METADATA);
    }

    public void collectImageOnce(String requestId, ImageResultCallback callback) {
        beginCapture(requestId, null, callback, CAPTURE_MODE_IMAGE);
    }

    public void cancelActiveCapture(String message) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!captureInProgress) {
                    return;
                }
                stopSdkWork();
                finishActiveCapture(false, STATUS_CAPTURE_CANCELLED, STATUS_CAPTURE_CANCELLED,
                        message == null ? "Fingerprint capture cancelled." : message);
            }
        });
    }

    private void beginCapture(
            String requestId,
            ResultCallback callback,
            ImageResultCallback imageCallback,
            int captureMode
    ) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                start();
                if (captureInProgress) {
                    FingerprintCapturePayload failure = failurePayload(requestId, STATUS_CAPTURE_IN_PROGRESS,
                            STATUS_CAPTURE_IN_PROGRESS, "Fingerprint capture is already in progress.");
                    completeCaptureCallback(callback, imageCallback, failure, null);
                    return;
                }
                captureInProgress = true;
                openedForActiveCapture = false;
                activeCaptureMode = captureMode;
                activeCaptureCallback = callback;
                activeImageCaptureCallback = imageCallback;
                activePayload = startedPayload(requestId);
                activeStartElapsedMs = SystemClock.elapsedRealtime();

                selectedDevice = findCandidateDevice();
                applyDeviceInfo(activePayload, selectedDevice);
                if (selectedDevice == null) {
                    finishActiveCapture(false, STATUS_DEVICE_NOT_FOUND, STATUS_DEVICE_NOT_FOUND,
                            "No USB fingerprint device detected.");
                    return;
                }
                if (!hasUsbPermission(selectedDevice)) {
                    requestPermissionForSelectedDevice();
                    return;
                }
                openAndStartCapture();
            }
        });
    }

    public void release() {
        mainHandler.removeCallbacks(captureTimeoutRunnable);
        if (captureInProgress) {
            stopSdkWork();
        }
        UseManager.getInstance().CloseDevice();
        captureInProgress = false;
        activePayload = null;
        activeCaptureCallback = null;
        activeImageCaptureCallback = null;
        permissionOnlyCallback = null;

        if (deviceReader != null) {
            deviceReader.PauseUnRegister(context);
        }
        if (receiverRegistered) {
            context.unregisterReceiver(usbReceiver);
            receiverRegistered = false;
        }
    }

    private void configureSdkCallbacks() {
        deviceReader.setOnDeviceStatusListener(new AsyncUsbReader.OnDeviceStatusListener() {
            @Override
            public void onReady() {
                emitStatus(checkDevice());
            }

            @Override
            public void onNoFound() {
                emitStatus(failurePayload(null, STATUS_DEVICE_NOT_FOUND, STATUS_DEVICE_NOT_FOUND,
                        "SDK did not find a fingerprint device."));
            }

            @Override
            public void onNoPermission() {
                emitStatus(failurePayload(null, STATUS_USB_PERMISSION_REQUIRED, STATUS_USB_PERMISSION_REQUIRED,
                        "SDK reports missing USB permission."));
            }

            @Override
            public void onAttached() {
                emitStatus(checkDevice());
            }

            @Override
            public void onDetached() {
                if (captureInProgress) {
                    stopSdkWork();
                    finishActiveCapture(false, STATUS_DEVICE_DETACHED, STATUS_DEVICE_DETACHED,
                            "Fingerprint device was detached during capture.");
                } else {
                    closeDevice();
                    emitStatus(failurePayload(null, STATUS_DEVICE_DETACHED, STATUS_DEVICE_DETACHED,
                            "Fingerprint device detached."));
                }
            }

            @Override
            public void onError() {
                emitStatus(failurePayload(null, STATUS_SDK_EXCEPTION, STATUS_SDK_EXCEPTION,
                        "SDK device status error."));
            }
        });

        captureReader.setOnGetImageListener(new AsyncUsbReader.OnGetImageListener() {
            @Override
            public void onGetImageSuccess() {
                if (activeCaptureMode == CAPTURE_MODE_IMAGE) {
                    try {
                        captureReader.UpImage();
                    } catch (RuntimeException ex) {
                        finishActiveCapture(false, STATUS_IMAGE_READ_FAILED, STATUS_IMAGE_READ_FAILED,
                                "UpImage failed: " + ex.getClass().getSimpleName());
                    }
                    return;
                }
                finishActiveCapture(true, STATUS_SUCCESS, null, null);
            }

            @Override
            public void onGetImageFail() {
                retryGetImage();
            }
        });

        captureReader.setOnUpImageListener(new AsyncUsbReader.OnUpImageListener() {
            @Override
            public void onUpImageSuccess(byte[] data) {
                if (!captureInProgress || activePayload == null) {
                    return;
                }
                Bitmap bitmap = null;
                if (data != null && data.length > 0) {
                    bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                }
                if (bitmap == null) {
                    finishActiveCapture(false, STATUS_IMAGE_READ_FAILED, STATUS_IMAGE_READ_FAILED,
                            "Fingerprint image bytes could not be decoded.");
                    return;
                }
                finishActiveCaptureWithBitmap(true, STATUS_SUCCESS, null, null, bitmap);
            }

            @Override
            public void onUpImageFail() {
                finishActiveCapture(false, STATUS_IMAGE_READ_FAILED, STATUS_IMAGE_READ_FAILED,
                        "Fingerprint image read failed.");
            }
        });
    }

    private void handleUsbPermissionResult(Intent intent) {
        UsbDevice device = getUsbDevice(intent);
        boolean granted = intent != null
                && intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
        if (device != null) {
            selectedDevice = device;
        }

        if (permissionOnlyCallback != null) {
            ResultCallback callback = permissionOnlyCallback;
            permissionOnlyCallback = null;
            FingerprintCapturePayload payload = startedPayload(null);
            applyDeviceInfo(payload, selectedDevice);
            if (granted) {
                payload.deviceSn = readAndroidUsbSerial(selectedDevice);
                finishPayload(payload, true, STATUS_DEVICE_READY, null, null, 0L);
            } else {
                finishPayload(payload, false, STATUS_USB_PERMISSION_DENIED, STATUS_USB_PERMISSION_DENIED,
                        "USB permission was denied.", 0L);
            }
            callback.onComplete(payload);
            return;
        }

        if (!captureInProgress) {
            return;
        }
        applyDeviceInfo(activePayload, selectedDevice);
        if (!granted) {
            finishActiveCapture(false, STATUS_USB_PERMISSION_DENIED, STATUS_USB_PERMISSION_DENIED,
                    "USB permission was denied.");
            return;
        }
        openAndStartCapture();
    }

    private void requestPermissionForSelectedDevice() {
        try {
            if (deviceReader != null) {
                deviceReader.RequestPermission(context);
                return;
            }
        } catch (RuntimeException ignored) {
            // Fall through to the platform UsbManager permission request.
        }
        if (usbManager != null && selectedDevice != null && permissionIntent != null) {
            usbManager.requestPermission(selectedDevice, permissionIntent);
        }
    }

    private void openAndStartCapture() {
        if (!captureInProgress || activePayload == null) {
            return;
        }
        if (selectedDevice == null) {
            selectedDevice = findCandidateDevice();
            applyDeviceInfo(activePayload, selectedDevice);
        }
        if (selectedDevice == null) {
            finishActiveCapture(false, STATUS_DEVICE_NOT_FOUND, STATUS_DEVICE_NOT_FOUND,
                    "No USB fingerprint device detected.");
            return;
        }
        if (!hasUsbPermission(selectedDevice)) {
            finishActiveCapture(false, STATUS_USB_PERMISSION_REQUIRED, STATUS_USB_PERMISSION_REQUIRED,
                    "USB permission is required before capture.");
            return;
        }

        int openResult;
        try {
            openResult = UseManager.getInstance().OpenDevice();
        } catch (RuntimeException ex) {
            finishActiveCapture(false, STATUS_SDK_EXCEPTION, STATUS_SDK_EXCEPTION,
                    "OpenDevice failed: " + ex.getClass().getSimpleName());
            return;
        }
        if (openResult != 0) {
            finishActiveCapture(false, STATUS_DEVICE_OPEN_FAILED, String.valueOf(openResult),
                    sdkErrorToString(openResult));
            return;
        }
        openedForActiveCapture = true;
        activePayload.deviceSn = readAndroidUsbSerial(selectedDevice);
        readSdkDeviceFields(activePayload);

        mainHandler.postDelayed(captureTimeoutRunnable, CAPTURE_TIMEOUT_MS);
        try {
            captureReader.GetImage();
        } catch (RuntimeException ex) {
            finishActiveCapture(false, STATUS_SDK_EXCEPTION, STATUS_SDK_EXCEPTION,
                    "GetImage failed: " + ex.getClass().getSimpleName());
        }
    }

    private void retryGetImage() {
        if (!captureInProgress || activePayload == null) {
            return;
        }
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!captureInProgress || activePayload == null) {
                    return;
                }
                try {
                    captureReader.GetImage();
                } catch (RuntimeException ex) {
                    finishActiveCapture(false, STATUS_SDK_EXCEPTION, STATUS_SDK_EXCEPTION,
                            "GetImage retry failed: " + ex.getClass().getSimpleName());
                }
            }
        }, CAPTURE_RETRY_DELAY_MS);
    }

    private void finishActiveCapture(boolean success, String collectStatus, String errorCode, String errorMessage) {
        finishActiveCaptureWithBitmap(success, collectStatus, errorCode, errorMessage, null);
    }

    private void finishActiveCaptureWithBitmap(
            boolean success,
            String collectStatus,
            String errorCode,
            String errorMessage,
            Bitmap bitmap
    ) {
        if (!captureInProgress || activePayload == null) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
            return;
        }
        mainHandler.removeCallbacks(captureTimeoutRunnable);
        if (!success) {
            stopSdkWork();
        }
        if (openedForActiveCapture || UseManager.getInstance().IsOpen()) {
            UseManager.getInstance().CloseDevice();
        }

        long duration = Math.max(0L, SystemClock.elapsedRealtime() - activeStartElapsedMs);
        finishPayload(activePayload, success, collectStatus, errorCode, errorMessage, duration);
        ResultCallback callback = activeCaptureCallback;
        ImageResultCallback imageCallback = activeImageCaptureCallback;
        FingerprintCapturePayload result = activePayload;

        captureInProgress = false;
        openedForActiveCapture = false;
        activeCaptureMode = CAPTURE_MODE_METADATA;
        activeCaptureCallback = null;
        activeImageCaptureCallback = null;
        activePayload = null;

        completeCaptureCallback(callback, imageCallback, result, success ? bitmap : null);
        if (!success && bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    private void completeCaptureCallback(
            ResultCallback callback,
            ImageResultCallback imageCallback,
            FingerprintCapturePayload payload,
            Bitmap bitmap
    ) {
        if (callback != null) {
            callback.onComplete(payload);
        }
        if (imageCallback != null) {
            imageCallback.onComplete(new FingerprintImageCaptureResult(payload, bitmap));
        } else if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
    }

    private FingerprintCapturePayload startedPayload(String requestId) {
        FingerprintCapturePayload payload = new FingerprintCapturePayload();
        payload.requestId = requestId;
        payload.startedAt = formatNow();
        payload.finishedAt = null;
        payload.durationMs = null;
        payload.qualityScore = null;
        payload.collectStatus = "STARTED";
        payload.success = false;
        return payload;
    }

    private FingerprintCapturePayload failurePayload(
            String requestId,
            String collectStatus,
            String errorCode,
            String errorMessage
    ) {
        FingerprintCapturePayload payload = startedPayload(requestId);
        selectedDevice = findCandidateDevice();
        applyDeviceInfo(payload, selectedDevice);
        finishPayload(payload, false, collectStatus, errorCode, errorMessage, 0L);
        return payload;
    }

    private void finishPayload(
            FingerprintCapturePayload payload,
            boolean success,
            String collectStatus,
            String errorCode,
            String errorMessage,
            long durationMs
    ) {
        payload.success = success;
        payload.collectStatus = collectStatus;
        payload.errorCode = errorCode;
        payload.errorMessage = errorMessage;
        payload.finishedAt = formatNow();
        payload.durationMs = durationMs;
        payload.qualityScore = null;
    }

    private void applyDeviceInfo(FingerprintCapturePayload payload, UsbDevice device) {
        if (payload == null || device == null) {
            return;
        }
        payload.deviceVendorId = String.valueOf(device.getVendorId());
        payload.deviceProductId = String.valueOf(device.getProductId());
        try {
            String productName = device.getProductName();
            payload.deviceModel = productName == null || productName.trim().isEmpty()
                    ? "USB fingerprint device"
                    : productName.trim();
        } catch (SecurityException ex) {
            payload.deviceModel = "USB fingerprint device";
        }
    }

    private void readSdkDeviceFields(FingerprintCapturePayload payload) {
        UsbReader reader = UseManager.getInstance().getUsbReader();
        if (reader == null || payload == null) {
            return;
        }
        try {
            payload.sdkDeviceType = String.valueOf(reader.GetDeviceModel());
        } catch (RuntimeException ignored) {
            payload.sdkDeviceType = null;
        }

        byte[] serialBuffer = new byte[128];
        try {
            int serialResult = reader.ReadSerialNumber(serialBuffer);
            if (serialResult == 0) {
                String serial = decodeCString(serialBuffer);
                if (!serial.isEmpty()) {
                    payload.deviceSn = serial;
                }
            }
        } catch (RuntimeException ignored) {
            // Serial number is optional metadata; capture must not fail because it is unavailable.
        }
    }

    private UsbDevice findCandidateDevice() {
        if (usbManager == null) {
            return null;
        }
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        UsbDevice firstDevice = null;
        for (Map.Entry<String, UsbDevice> entry : deviceList.entrySet()) {
            UsbDevice device = entry.getValue();
            if (firstDevice == null) {
                firstDevice = device;
            }
            if (isKnownFingerprintCandidate(device)) {
                return device;
            }
        }
        return firstDevice;
    }

    private boolean isKnownFingerprintCandidate(UsbDevice device) {
        if (device == null) {
            return false;
        }
        int vendorId = device.getVendorId();
        int productId = device.getProductId();
        return (vendorId == 0x0453 && productId == 0x9005)
                || (vendorId == 0x2009 && productId == 0x7638)
                || (vendorId == 0x2109 && productId == 0x7638)
                || (vendorId == 0x0483 && productId == 0x5720);
    }

    private boolean hasUsbPermission(UsbDevice device) {
        return usbManager != null && device != null && usbManager.hasPermission(device);
    }

    private String readAndroidUsbSerial(UsbDevice device) {
        try {
            String serial = device == null ? null : device.getSerialNumber();
            return serial == null || serial.trim().isEmpty() ? null : serial.trim();
        } catch (SecurityException ex) {
            return null;
        }
    }

    private String decodeCString(byte[] data) {
        int length = 0;
        while (length < data.length && data[length] != 0) {
            length++;
        }
        return new String(data, 0, length, StandardCharsets.US_ASCII).trim();
    }

    private String sdkErrorToString(int code) {
        try {
            UsbReader reader = UseManager.getInstance().getUsbReader();
            if (reader != null) {
                String message = reader.FPErr2Str(code);
                if (message != null && !message.trim().isEmpty()) {
                    return message.trim();
                }
            }
        } catch (RuntimeException ignored) {
            // Error mapping is best-effort.
        }
        return "SDK error code: " + code;
    }

    private UsbDevice getUsbDevice(Intent intent) {
        if (intent == null) {
            return null;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice.class);
        }
        return intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
    }

    private boolean sameDevice(UsbDevice left, UsbDevice right) {
        if (left == null || right == null) {
            return false;
        }
        return left.getDeviceId() == right.getDeviceId()
                && left.getVendorId() == right.getVendorId()
                && left.getProductId() == right.getProductId();
    }

    private void stopSdkWork() {
        try {
            UsbReader reader = UseManager.getInstance().getUsbReader();
            if (reader != null) {
                reader.StopWork();
            }
        } catch (RuntimeException ignored) {
            // StopWork is best-effort during timeout, close, and detach handling.
        }
    }

    private void emitStatus(FingerprintCapturePayload payload) {
        if (statusListener != null) {
            statusListener.onStatus(payload);
        }
    }

    private String formatNow() {
        return timeFormat.format(new Date());
    }
}
