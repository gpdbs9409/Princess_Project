package com.example.princessproject.upload.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/**
 * Rejects mission-verification photos that weren't taken today, read from the file's own
 * EXIF DateTimeOriginal tag - catches someone re-uploading an old photo to fake a mission.
 * A photo with no EXIF date at all (screenshots, or photos that passed through a messenger
 * app like KakaoTalk, which strips metadata on send) is also rejected - we can't prove it
 * was taken today, so it doesn't get the benefit of the doubt.
 */
@Component
public class PhotoDateVerifier {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter EXIF_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");

    public void verifyTakenToday(InputStream inputStream) {
        LocalDate takenDate = readTakenDate(inputStream);
        if (takenDate == null) {
            throw new UploadValidationException(
                    "PHOTO_DATE_UNKNOWN",
                    "Photo has no readable EXIF capture date - cannot verify it was taken today");
        }

        LocalDate today = LocalDate.now(ZONE);
        if (!takenDate.equals(today)) {
            throw new UploadValidationException(
                    "PHOTO_NOT_FROM_TODAY",
                    "Photo appears to have been taken on " + takenDate + ", not today (" + today + ")");
        }
    }

    private LocalDate readTakenDate(InputStream inputStream) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(inputStream);
            ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (directory == null) {
                return null;
            }
            String raw = directory.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
            if (raw == null || raw.isBlank()) {
                return null;
            }
            return LocalDateTime.parse(raw, EXIF_DATE_FORMAT).toLocalDate();
        } catch (Exception e) {
            // Unreadable format, no metadata, or a malformed date string - treated the same as
            // "no date at all" by the caller, i.e. rejected.
            return null;
        }
    }
}
