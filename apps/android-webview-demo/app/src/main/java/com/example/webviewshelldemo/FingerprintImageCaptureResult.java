package com.example.webviewshelldemo;

import android.graphics.Bitmap;

public final class FingerprintImageCaptureResult {
    final FingerprintCapturePayload payload;
    final Bitmap bitmap;

    FingerprintImageCaptureResult(FingerprintCapturePayload payload, Bitmap bitmap) {
        this.payload = payload;
        this.bitmap = bitmap;
    }
}
