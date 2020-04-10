package server;

import com.sun.net.httpserver.HttpExchange;
import gameLogic.Game;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO: 2020-04-03 implement a mechanism by which when a game is complete, the game's lobbyCode is free'd
public class CreateGameHandler extends ContextHandler{

    private ConcurrentMap<String, Game> gameLobbies;
    public CreateGameHandler(ConcurrentMap<String, Game> gameLobbies){
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
                .flatMap(s->Stream.of(s.split("&")))
                .collect(Collectors.toConcurrentMap(
                        s -> URLDecoder.decode(s.split("=")[0], StandardCharsets.UTF_8),
                        s -> URLDecoder.decode(s.split("=")[1], StandardCharsets.UTF_8)));

        var newGame = new Game();

        newGame.getPlayer1().setName(postData.get("name"));

        var lobbyCode = generateLobbyCode();
        while(gameLobbies.containsKey(lobbyCode)){
            lobbyCode = generateLobbyCode();
        }
        String finalLobbyCode = lobbyCode;
        newGame.registerTerminationListener(()-> gameLobbies.remove(finalLobbyCode));
        gameLobbies.put(lobbyCode, newGame);

        var headers = httpExchange.getResponseHeaders();
        headers.add("Set-cookie", String.format("lobbyCode=%s; domain=24.224.183.243", lobbyCode));
        headers.add("Set-cookie", String.format("name=%s; domain=24.224.183.243; path=/", postData.get("name")));
        headers.set("Content-type", "application/json");
        httpExchange.sendResponseHeaders(200, 0);
        httpExchange.getResponseBody().write(String.format("{\"lobbyCode\":\"%s\"}", lobbyCode).getBytes((StandardCharsets.UTF_8)));
        httpExchange.close();


    }

    public char randAlphaNumeric(){
        int random = (int)(Math.random() * 62 + 55);
        if(random < 65){
            return (char) (random - 7);
        } else if(random > 90){
            return (char) (random + 6);
        } else {
            return (char) random;
        }
    }

    String generateLobbyCode(){
        var builder = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            builder.append(randAlphaNumeric());
        }
        return builder.toString();
    }



}
