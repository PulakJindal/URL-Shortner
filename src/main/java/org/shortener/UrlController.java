package org.shortener;

import io.javalin.http.Context;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

public class UrlController {
    private final UrlRepository repo;
    private final UrlService service;
    private static final String BASE_URL = EnvUtil.getEnvOrDefault("BASE_URL", "http://localhost:7070");

    public UrlController(UrlRepository repo, UrlService service){
        this.repo = repo;
        this.service = service;
    }

    public void create(Context ctx){
        ShortenReq req = ctx.bodyAsClass(ShortenReq.class);
        if(req.url == null || req.url.isBlank()){
            ctx.status(400).json(Map.of("error", "url is required"));
            return;
        }
        if(!req.url.startsWith("http://") && !req.url.startsWith("https://")){
            ctx.status(400).json(Map.of("error", "not a valid url"));
            return;
        }
        OffsetDateTime expiresAt = req.expiresInSecond != null
                ? OffsetDateTime.now().plusSeconds(req.expiresInSecond)
                : null;
        String code = service.createShortUrl(req.url, expiresAt, repo);
        ctx.json(Map.of("shortUrl", BASE_URL+code));
    }

    public void redirect(Context ctx){
        String code = ctx.pathParam("code");
        Optional<UrlRecord> recordO = repo.findByShortCode(code);
        if(recordO.isPresent()) {
            UrlRecord record = recordO.get();
            if(record.expiresAt != null && record.expiresAt.isBefore(OffsetDateTime.now())) ctx.status(410);
            else {
                repo.incrementClickCount(code);
                ctx.redirect(record.longUrl);
            }
        }
        else ctx.status(404);
    }
}
