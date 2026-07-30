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
 * A photo with no EXIF date at all (screenshots, or photos re-encoded by an app that strips
 * metadata) is let through rather than rejected, since we can't tell it's actually old either -
 * this only blocks the case we can positively verify.
 */
@Component
public class PhotoDateVerifier {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter EXIF_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");

    public void verifyTakenToday(InputStream inputStream) {
        LocalDate takenDate = readTakenDate(inputStream);
        if (takenDate == null) {
            return;
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
            // Unreadable format, no metadata, or a malformed date string - we can't verify
            // either way, so let the upload proceed rather than blocking on an unrelated failure.
            return null;
        }
    }
}
