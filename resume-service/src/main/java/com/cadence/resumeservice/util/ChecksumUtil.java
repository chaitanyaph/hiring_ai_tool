package com.cadence.resumeservice.util;

import com.cadence.resumeservice.exception.ErrorCode;
import com.cadence.resumeservice.exception.ResumeServiceException;
import org.springframework.http.HttpStatus;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** SHA-256 content hash -- the basis for both duplicate-upload detection and the checksum stored on every resume row. */
public final class ChecksumUtil {
    private ChecksumUtil() {}

    public static String sha256(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new ResumeServiceException(ErrorCode.INTERNAL_ERROR, "Could not compute file checksum", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
