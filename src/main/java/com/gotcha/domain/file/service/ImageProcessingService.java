package com.gotcha.domain.file.service;

import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ImageProcessingService {

    private static final int MAX_DIMENSION = 1920;
    private static final int THUMB_DIMENSION = 400;
    private static final int MAIN_QUALITY = 80;
    private static final int THUMB_QUALITY = 75;

    public record ProcessedImageResult(
            byte[] mainImageBytes,
            byte[] thumbnailBytes,
            String contentType,
            String extension
    ) {}

    public ProcessedImageResult process(byte[] imageBytes, String originalContentType) {
        try {
            byte[] readableBytes = imageBytes;

            if (isHeic(originalContentType)) {
                readableBytes = convertHeicToJpeg(imageBytes);
                if (readableBytes == null) {
                    return null;
                }
            }

            ImmutableImage image = ImmutableImage.loader().fromBytes(readableBytes);

            ImmutableImage mainImage = image.bound(MAX_DIMENSION, MAX_DIMENSION);
            byte[] mainBytes = mainImage.bytes(WebpWriter.DEFAULT.withQ(MAIN_QUALITY));

            ImmutableImage thumbImage = image.bound(THUMB_DIMENSION, THUMB_DIMENSION);
            byte[] thumbBytes = thumbImage.bytes(WebpWriter.DEFAULT.withQ(THUMB_QUALITY));

            log.info("Image processed: original {}KB -> main {}KB, thumb {}KB",
                    imageBytes.length / 1024,
                    mainBytes.length / 1024,
                    thumbBytes.length / 1024);

            return new ProcessedImageResult(mainBytes, thumbBytes, "image/webp", ".webp");
        } catch (Exception e) {
            log.warn("Image processing failed, will upload original: {}", e.getMessage());
            return null;
        }
    }

    private boolean isHeic(String contentType) {
        return contentType != null
                && (contentType.equalsIgnoreCase("image/heic") || contentType.equalsIgnoreCase("image/heif"));
    }

    private byte[] convertHeicToJpeg(byte[] heicBytes) {
        Path heicFile = null;
        Path jpegFile = null;
        try {
            heicFile = Files.createTempFile("heic_", ".heic");
            jpegFile = Path.of(heicFile.toString().replace(".heic", ".jpg"));

            Files.write(heicFile, heicBytes);

            Process process = new ProcessBuilder("heif-convert", heicFile.toString(), jpegFile.toString())
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();

            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                log.warn("heif-convert timed out");
                return null;
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.warn("heif-convert failed with exit code {}", exitCode);
                return null;
            }

            return Files.readAllBytes(jpegFile);
        } catch (Exception e) {
            log.warn("HEIC conversion failed: {}", e.getMessage());
            return null;
        } finally {
            deleteTempFile(heicFile);
            deleteTempFile(jpegFile);
        }
    }

    private void deleteTempFile(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                log.debug("Failed to delete temp file: {}", path);
            }
        }
    }
}
