package server;

import com.sun.net.httpserver.HttpExchange;
import gameLogic.Game;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JoinGameHanlder extends ContextHandler{

    private volatile ConcurrentMap<String, Game> gameLobbies;
    public JoinGameHanlder(ConcurrentMap<String, Game> gameLobbies){
        this.gameLobbies = gameLobbies;
    }
    @Override
    protected List<String> getSupportedMethods() {
        return List.of("POST");
    }
    @Override
    public void POST(HttpExchange httpExchange) throws IOException {
        var reader = new BufferedReader(new InputStreamReader(httpExchange.getRequestBody()));
        ConcurrentMap<String, String> postData =
                reader.lines()
                        .flatMap(s-> Stream.of(s.split("&")))
                        .collect(Collectors.toConcurrentMap(
                                s -> URLDecoder.decode(s.split("=")[0], StandardCharsets.UTF_8),
                                s -> URLDecoder.decode(s.split("=")[1], StandardCharsets.UTF_8)));

        String lobbyCode = postData.get("lobbyID");
        var game = gameLobbies.get(lobbyCode);

        game.getPlayer2().setName(postData.get("name"));

        var headers = httpExchange.getResponseHeaders();
        headers.add("Set-cookie", String.format("lobbyCode=%s; domain=24.224.183.243", lobbyCode));
        headers.add("Set-cookie", String.format("name=%s; domain=24.224.183.243; path=/", postData.get("name")));
        headers.set("Content-type", "application/json");
        httpExchange.sendResponseHeaders(200, 0);
        httpExchange.getResponseBody().write(String.format("{\"lobbyCode\":\"%s\"}", lobbyCode).getBytes((StandardCharsets.UTF_8)));
        httpExchange.close();


    }

}
