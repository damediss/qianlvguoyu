package com.guoyu.platform.trainingdepartment.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 二维码生成服务 - 使用 ZXing 库生成二维码图片
 */
@Service
@Slf4j
public class QrCodeGenerationService {

    private final String cdnPath;
    private final String cdnUrl;
    private final int qrCodeSize;

    public QrCodeGenerationService(
            @Value("${platform.cdn.path:./data/cdn}") String cdnPath,
            @Value("${platform.cdn.url:http://localhost:8080/api/exam/files}") String cdnUrl,
            @Value("${platform.qrcode.size:300}") int qrCodeSize) {
        this.cdnPath = cdnPath;
        this.cdnUrl = cdnUrl.endsWith("/") ? cdnUrl.substring(0, cdnUrl.length() - 1) : cdnUrl;
        this.qrCodeSize = qrCodeSize;
    }

    /**
     * 生成二维码图片并保存到文件系统
     *
     * @param content 二维码内容（URL 或文本）
     * @param fileName 文件名（不含扩展名）
     * @return 二维码图片的访问 URL
     */
    public String generateAndSaveQrCode(String content, String fileName) throws IOException, WriterException {
        Path qrCodeDir = Paths.get(cdnPath).resolve("qrcodes");
        Files.createDirectories(qrCodeDir);

        Path qrCodeFile = qrCodeDir.resolve(fileName + ".png");

        // 如果文件已存在，直接返回 URL
        if (Files.exists(qrCodeFile)) {
            log.debug("QR code already exists: {}", qrCodeFile);
            return getQrCodeUrl(fileName);
        }

        // 生成二维码
        BufferedImage qrImage = generateQrCodeImage(content);

        // 保存到文件
        ImageIO.write(qrImage, "PNG", qrCodeFile.toFile());
        log.info("QR code generated and saved: {}", qrCodeFile);

        return getQrCodeUrl(fileName);
    }

    /**
     * 生成二维码图片的 Base64 编码（data URL）
     *
     * @param content 二维码内容
     * @return Base64 编码的 data URL
     */
    public String generateQrCodeDataUrl(String content) throws IOException, WriterException {
        BufferedImage qrImage = generateQrCodeImage(content);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(qrImage, "PNG", outputStream);
        byte[] imageBytes = outputStream.toByteArray();

        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        return "data:image/png;base64," + base64;
    }

    /**
     * 生成二维码 BufferedImage
     */
    private BufferedImage generateQrCodeImage(String content) throws WriterException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix bitMatrix = qrCodeWriter.encode(
                content,
                BarcodeFormat.QR_CODE,
                qrCodeSize,
                qrCodeSize,
                hints
        );

        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    /**
     * 获取二维码文件的访问 URL
     */
    public String getQrCodeUrl(String fileName) {
        return cdnUrl + "/qrcodes/" + fileName + ".png";
    }

    /**
     * 检查二维码文件是否存在
     */
    public boolean qrCodeExists(String fileName) {
        Path qrCodeFile = Paths.get(cdnPath).resolve("qrcodes").resolve(fileName + ".png");
        return Files.exists(qrCodeFile);
    }

    /**
     * 删除二维码文件
     */
    public void deleteQrCode(String fileName) throws IOException {
        Path qrCodeFile = Paths.get(cdnPath).resolve("qrcodes").resolve(fileName + ".png");
        if (Files.exists(qrCodeFile)) {
            Files.delete(qrCodeFile);
            log.info("QR code deleted: {}", qrCodeFile);
        }
    }
}
