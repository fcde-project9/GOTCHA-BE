package com.gotcha.domain.file.util;

public final class ImageUrlUtils {

    private ImageUrlUtils() {}

    public static String toThumbnailUrl(String imageUrl) {
        if (imageUrl == null || !imageUrl.endsWith(".webp") || imageUrl.contains("_thumb.webp")) {
            return imageUrl;
        }
        return imageUrl.substring(0, imageUrl.length() - 5) + "_thumb.webp";
    }
}
