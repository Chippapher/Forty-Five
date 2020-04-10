package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


/**
 *
 * ContextHandler is a base class for handling http contexts handling basic method routing logic.
 *
 * subclasses must override getSupportedMethods() to reflect which HttpExchange handlers (GET(), POST(), etc.) they've
 * overridden
 *
 * all HttpExchange handlers except OPTIONS() sends a 405 (method not supported) response by default, OPTIONS() sends
 * a 201 with an Allows header reflecting the return value of getSupportedMethods
 *
 * */
public abstract class ContextHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        switch (exchange.getRequestMethod()){
            case "GET"-> GET(exchange);
            case "HEAD"-> HEAD(exchange);
            case "POST"-> POST(exchange);
            case "PUT"-> PUT(exchange);
            case "DELETE"-> DELETE(exchange);
            case "CONNECT"-> CONNECT(exchange);
            case "OPTIONS"-> OPTIONS(exchange);
            case "TRACE"-> TRACE(exchange);
            case "PATCH"-> PATCH(exchange);
            default -> sendAllowHeader(exchange, 405);

        }
    }

    private void sendAllowHeader(HttpExchange exchange, int rCode) throws IOException {
        var header = exchange.getResponseHeaders();
        var supportedMethods = getSupportedMethods();
        for(String method: supportedMethods){
            header.add("Allow", method);
        }
        exchange.sendResponseHeaders(rCode, -1);
    }
    public static void streamFileToOutputStream(Path path, OutputStream outputStream){
        try (var in = new BufferedInputStream(new FileInputStream(path.toFile())); var out = new BufferedOutputStream(outputStream)){
            byte[] buffer = new byte[2048];
            int bytesRead = 0;
            while(bytesRead != -1){
                int available = in.available();
                bytesRead = in.read(buffer, 0 , (available < buffer.length && available > 0)? available : buffer.length);
                out.write(buffer, 0, bytesRead);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    protected void send404(HttpExchange exchange){
        try{
            Path path = Paths.get("./src/server/front-end/HTML/404.html");
            var headers = exchange.getResponseHeaders();
            headers.add("Content-type", "text/html");
            exchange.sendResponseHeaders(404, 0);
            streamFileToOutputStream(path, exchange.getResponseBody());


        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public void GET(HttpExchange exchange) throws IOException {
        sendAllowHeader(exchange, 405);
    }
    public void HEAD(HttpExchange exchange) throws IOException {
        sendAllowHeader(exchange, 405);
    }
    public void POST(HttpExchange exchange) throws IOException {
        sendAllowHeader(exchange, 405);
    }
    public void PUT(HttpExchange exchange) throws IOException {
        sendAllowHeader(exchange, 405);
    }
    public void DELETE(HttpExchange exchange) throws IOException {
        sendAllowHeader(exchange, 405);
    }
    public void CONNECT(HttpExchange exchange) throws IOException {
        sendAllowHeader(exchange, 405);
    }
    public void OPTIONS(HttpExchange exchange) throws IOException {
        sendAllowHeader(exchange, 201);
    }
    public void TRACE(HttpExchange exchange) throws IOException {
        sendAllowHeader(exchange, 405);
    }
    public void PATCH(HttpExchange exchange) throws IOException {
        sendAllowHeader(exchange, 405);
    }


    /**
     * @return A list of strings corresponding to the methods overridden in this classes subclasses
     */
    protected abstract List<String> getSupportedMethods();



}
