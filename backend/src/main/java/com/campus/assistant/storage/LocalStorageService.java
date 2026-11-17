package com.campus.assistant.storage;

import com.campus.assistant.common.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 本地磁盘存储实现：
 * - 限制 jpg/png/webp，<=8MB
 * - 文件名随机（UUID），按日期分目录
 * - EXIF 清洗：通过 ImageIO 重绘图片去掉元数据（webp 读不支持时原样保存）
 * 预览 URL：/api/files/{ossKey}（WebMvcConfig 静态映射）
 */
@Slf4j
@Component
public class LocalStorageService implements StorageService {

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp");

    @Value("${campus.upload.dir}")
    private String dir;

    @Value("${campus.upload.max-size-mb}")
    private int maxSizeMb;

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BizException.of(BizException.BAD_REQUEST, "未选择文件");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = original.lastIndexOf('.');
        String ext = dot >= 0 ? original.substring(dot + 1).toLowerCase() : "";
        if (!ALLOWED_EXT.contains(ext)) {
            throw BizException.of(BizException.BAD_REQUEST, "仅支持 jpg/png/webp 格式");
        }
        if (file.getSize() > maxSizeMb * 1024L * 1024L) {
            throw BizException.of(BizException.BAD_REQUEST, "文件超过 " + maxSizeMb + "MB 限制");
        }
        if (ext.equals("jpeg")) {
            ext = "jpg";
        }
        // 随机文件名，按日期分目录
        String date = LocalDate.now().toString().replace("-", "");
        String name = UUID.randomUUID().toString().replace("-", "")
                + ThreadLocalRandom.current().nextInt(1000) + "." + ext;
        String ossKey = "img/" + date + "/" + name;

        File dest = new File(dir, ossKey);
        dest.getParentFile().mkdirs();
        try {
            file.transferTo(dest.getAbsoluteFile());
        } catch (IOException e) {
            throw new IllegalStateException("保存文件失败", e);
        }
        // EXIF 清洗：重绘图片去掉元数据（失败不阻断，保留原图）
        cleanExif(dest, ext);
        return ossKey;
    }

    /** 重绘图片以去除 EXIF 元数据 */
    private void cleanExif(File f, String ext) {
        try {
            if ("webp".equals(ext)) {
                // JDK ImageIO 默认不支持 webp 解码，跳过（webp 极少含 EXIF）
                return;
            }
            BufferedImage src = ImageIO.read(f);
            if (src == null) {
                return;
            }
            BufferedImage clean = new BufferedImage(src.getWidth(), src.getHeight(),
                    src.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
            clean.getGraphics().drawImage(src, 0, 0, null);
            clean.getGraphics().dispose();
            ImageIO.write(clean, "png".equals(ext) ? "png" : "jpg", f);
        } catch (Exception e) {
            log.warn("EXIF 清洗失败（保留原图）: {}", e.getMessage());
        }
    }

    @Override
    public byte[] read(String ossKey) {
        checkKey(ossKey);
        File f = new File(dir, ossKey);
        if (!f.exists()) {
            throw BizException.of(BizException.NOT_FOUND, "文件不存在");
        }
        try {
            return java.nio.file.Files.readAllBytes(f.toPath());
        } catch (IOException e) {
            throw BizException.of(BizException.NOT_FOUND, "文件不存在");
        }
    }

    private void checkKey(String ossKey) {
        if (ossKey == null || ossKey.contains("..") || ossKey.startsWith("/")) {
            throw BizException.of(BizException.BAD_REQUEST, "非法文件路径");
        }
    }
}
