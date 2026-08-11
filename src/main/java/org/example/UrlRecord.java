package org.example;

import java.time.OffsetDateTime;

public class UrlRecord {
    public String longUrl;
    public OffsetDateTime expiresAt; // null means no expiration
}
