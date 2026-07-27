package com.guoyu.platform.trainingdepartment.support;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/** 对齐 PHP ImageCombiner::combineVertical */
public final class TrainingImageCombiner {

  private TrainingImageCombiner() {}

  public static void combineVertical(List<Path> sources, Path output) throws IOException {
    if (sources == null || sources.isEmpty()) {
      throw new IllegalArgumentException("sources empty");
    }
    List<BufferedImage> images = new ArrayList<>();
    int maxWidth = 0;
    int totalHeight = 0;
    for (Path source : sources) {
      try (InputStream in = Files.newInputStream(source)) {
        BufferedImage image = ImageIO.read(in);
        if (image == null) {
          throw new IOException("无法读取图片: " + source);
        }
        images.add(image);
        maxWidth = Math.max(maxWidth, image.getWidth());
        totalHeight += image.getHeight();
      }
    }
    BufferedImage combined = new BufferedImage(maxWidth, totalHeight, BufferedImage.TYPE_INT_RGB);
    Graphics2D g = combined.createGraphics();
    try {
      g.setColor(Color.WHITE);
      g.fillRect(0, 0, maxWidth, totalHeight);
      int offsetY = 0;
      for (BufferedImage image : images) {
        g.drawImage(image, 0, offsetY, null);
        offsetY += image.getHeight();
      }
    } finally {
      g.dispose();
    }
    Files.createDirectories(output.getParent());
    String format = outputFileFormat(output);
    if (!ImageIO.write(combined, format, output.toFile())) {
      // JPEG writer 偶发拒绝 TYPE_INT_RGB 之外格式时回退
      ImageIO.write(combined, "png", output.toFile());
    }
  }

  private static String outputFileFormat(Path output) {
    String name = output.getFileName().toString().toLowerCase();
    if (name.endsWith(".png")) {
      return "png";
    }
    return "jpg";
  }
}
