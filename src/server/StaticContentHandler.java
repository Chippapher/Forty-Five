package server;

import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class StaticContentHandler extends ContextHandler {
    private URI baseURI;
    private String FSURI;
    private String contentType;

    public StaticContentHandler(String contextPath, String contentType){
        this.baseURI = URI.create(contextPath);
        this.FSURI = "C:/Users/borni/Desktop/UPEI/CS2060- WebDevelopment/hobbyProjects/45s/src/server/front-end";
        this.contentType = contentType;
    }

    @Override
    protected List<String> getSupportedMethods() {
        return List.of("GET");
    }
    public void GET(HttpExchange exchange){

        try{
            var headers = exchange.getResponseHeaders();
            headers.set("Content-type", contentType);
            var filePath = Paths.get(FSURI + exchange.getRequestURI().toString());
            exchange.sendResponseHeaders(200, 0);
            streamFileToOutputStream(filePath, exchange.getResponseBody());

        } catch(FileNotFoundException e ){
            send404(exchange);
        } catch(IOException e){
            e.printStackTrace();
        }
    }


}
