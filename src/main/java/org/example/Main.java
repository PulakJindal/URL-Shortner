package org.example;

import java.util.Optional;

import io.javalin.Javalin;

public class Main {
     public static void main(String[] args) throws Exception {
        UrlRepository repo = new UrlRepository(Database.getDataSource());

        boolean inserted = repo.insert("test111", "https://anthropic.com");
         System.out.println("Inserted: " + inserted);

         Optional<String> found = repo.findByShortCode("test111");
         System.out.println("Found: "+ found.orElse("nothing"));

         var app = Javalin.create(config -> {
             config.routes.get("/", ctx -> ctx.result("Hello World"));
             config.routes.get("/{url}", ctx ->{
                String shortUrl = ctx.pathParam("url");
                Optional<String> longUrl = repo.findByShortCode(shortUrl);
                if(longUrl.isPresent()) ctx.redirect(longUrl.get());
                else ctx.status(404);
             });
         }).start(7070);
    }
}