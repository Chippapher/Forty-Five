package server;

import gameLogic.Game;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/* TODO: 2020-04-05
    refactor codebase to locking code,
    enable a player to rejoin mid-session,
    fix null pointer issue in WsServer,

 */
/**
 *
 * Application is the entry point for starting the 45's webapp.
 *
 * */
public class Application {
    public static void main(String[] args) {
        ConcurrentMap<String, Game> gameLobbies = new ConcurrentHashMap<>();

        var cs = new ContentServer(null, gameLobbies);
        var ws = new WsServer(gameLobbies);

        new Thread(cs).start();
        new Thread(ws).start();

    }
}
