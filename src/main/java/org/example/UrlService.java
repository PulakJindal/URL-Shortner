package org.example;

import java.security.SecureRandom;
import java.util.Optional;

public class UrlService {
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQURSTUVWXYZ1234567890";

    public static String generateShortCode(int length){
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<length; i++){
            int index = random.nextInt(ALPHABET.length());
            sb.append(ALPHABET.charAt(index));
        }
        return sb.toString();
    }

    public String createShortUrl(String longUrl, UrlRepository repo){
        int maxAttempts = 5;
        for(int i=0; i<maxAttempts; i++){
            String code = generateShortCode(7);
            Optional<String> inserted = repo.insertIfNotExists(code, longUrl);
            if(inserted.isPresent()) return inserted.get();
        }
        throw new RuntimeException("Failed to generate a unique short code after " + maxAttempts + " attempts");
    }
}
