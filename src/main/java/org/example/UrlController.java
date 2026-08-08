package org.example;

import io.javalin.http.Context;

import java.util.Map;
import java.util.Optional;

public class UrlController {
    private final UrlRepository repo;
    private final UrlService service;

    public UrlController(UrlRepository repo, UrlService service){
        this.repo = repo;
        this.service = service;
    }

    public void create(Context ctx){
        ShortenReq req = ctx.bodyAsClass(ShortenReq.class);
        String code = service.createShortUrl(req.url, repo);
        ctx.json(Map.of("shortUrl", "http://localhost:7070/"+code));
    }

    public void redirect(Context ctx){
        String code = ctx.pathParam("code");
        Optional<String> longUrl = repo.findByShortCode(code);
        if(longUrl.isPresent()) ctx.redirect(longUrl.get());
        else ctx.status(404);
    }
}
