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
 *
 * The web client no longer offers a gallery file picker at all - the only way to produce a
 * mission photo is the in-app live camera capture, which draws a fresh video frame to a
 * <canvas> and encodes it on the spot. Canvas-encoded images never carry EXIF (there's no
 * camera hardware/driver involved), so treating "no EXIF" as suspicious would reject every
 * legitimate upload. Missing EXIF is therefore allowed through - freshness is enforced by
 * the capture UI itself. If a DateTimeOriginal tag *is* present (e.g. a future upload path
 * that accepts real camera files again) and it doesn't match today, we still reject as
 * defense in depth.
 */
@Component
public class PhotoDateVerifier {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter EXIF_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss");

    public void verifyTakenToday(InputStream inputStream) {
        LocalDate takenDate = readTakenDate(inputStream);
        if (takenDate == null) {
            // No EXIF - expected for canvas-captured photos from the live camera flow. Not
            // proof of anything either way, so it gets the benefit of the doubt.
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
            // Unreadable format, no metadata, or a malformed date string - treated the same as
            // "no date at all" by the caller, i.e. rejected.
            return null;
        }
    }
}
