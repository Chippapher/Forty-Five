package server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import gameLogic.Game;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public class ContentServer implements Runnable {
    static String fileContents = "";

    private final ExecutorService executor;
    private ConcurrentMap<String, Game> lobbies;


    ContentServer(ExecutorService exe, ConcurrentMap<String, Game> lobbies){
        this.executor = exe;
        this.lobbies = lobbies;

    }
    ContentServer() {
        this(null, null);
    }
    public void run(){
        try {
            System.out.println("contentServerStarted");
            var httpServer = HttpServer.create(new InetSocketAddress(80), 0);
            httpServer.setExecutor(executor);


            httpServer.createContext("/", new ContextHandler() {
                @Override
                protected List<String> getSupportedMethods() {
                    return List.of("GET");
                }
                @Override
                public void GET(HttpExchange exchange) throws IOException {
                    exchange.getResponseHeaders().set("Content-type", "text/html");
                    exchange.sendResponseHeaders(200, 0);
                    streamFileToOutputStream(Paths.get("./src/server/front-end/HTML/index.html"), exchange.getResponseBody());
                }
            });
            httpServer.createContext("/game", new ContextHandler() {
                @Override
                protected List<String> getSupportedMethods() {
                   return  List.of("GET");
                }
                @Override
                public void GET(HttpExchange exchange) throws IOException {
                    Headers headers = exchange.getResponseHeaders();
                    headers.set("Content-type", "text/html");
                    exchange.sendResponseHeaders(200, 0);
                    streamFileToOutputStream(Paths.get("./src/server/front-end/HTML/game.html"), exchange.getResponseBody());
                }
            });
            httpServer.createContext("/CSS", new StaticContentHandler("/CSS", "text/css"));
            httpServer.createContext("/JS", new StaticContentHandler("JS", "text/javascript"));
            httpServer.createContext("/IMAGES", new StaticContentHandler("IMAGES", "image/png" ));
            httpServer.createContext("/createGame", new CreateGameHandler(lobbies));
            httpServer.createContext("/joinGame", new JoinGameHanlder(lobbies));


            httpServer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
