package org.shortener;

import io.javalin.Javalin;
import org.shortener.db.Database;
import org.shortener.db.UrlRepository;
import org.shortener.service.RateLimiter;
import org.shortener.service.UrlService;
import org.shortener.web.HomeHandler;
import org.shortener.web.UrlController;

public class Main {
     public static void main(String[] args) throws Exception {
         UrlRepository repo = new UrlRepository(Database.getDataSource());
         UrlService service = new UrlService();
         RateLimiter rateLimiter = new RateLimiter();

         var app = Javalin.create(config -> {
             config.routes.before(ctx ->{
                 String ip = ctx.ip();
                 if(!rateLimiter.isInLimit(ip)){
                     throw new io.javalin.http.HttpResponseException(429, "Too many requests");
                 }
             });
             config.routes.get("/", new HomeHandler());
             UrlController controller = new UrlController(repo, service);
             config.routes.get("/{code}", controller::redirect);
             config.routes.post("/api/shorten", controller::create);
         }).start(7070);
    }
}