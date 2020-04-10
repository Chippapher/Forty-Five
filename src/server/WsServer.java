package server;

import gameLogic.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class WsServer implements Runnable{

    private ConcurrentMap<String, Game> lobbies;

    WsServer(ConcurrentMap<String, Game> lobbyMap){
        lobbies = lobbyMap;
    }

    private static Map<String, String> getCookie(String request){
        Matcher cookie = Pattern.compile("Cookie: (.*)\r\n").matcher(request);
        if(cookie.find()) {
            return Arrays.stream(cookie.group(1).split(";"))
                    .map(String::strip)
                    .collect(Collectors.toConcurrentMap(s->s.split("=")[0], s->s.split("=")[1]));
        } else {
            //likely should throw an error or do something meaningful
            return null;
        }
    }

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, InterruptedException, ExecutionException {



//        var staticServerThread = new Thread(new ContentServer());
//
//        var server = new ServerSocket(9000);
//        staticServerThread.start();
//
//
//
//
//
//
//        for (int i = 0; i < 2; i++) {
//            var socket = server.accept();
//            Scanner s = new Scanner(socket.getInputStream(), StandardCharsets.UTF_8);
//            String request = s.useDelimiter("\\r\\n\\r\\n").next();
//            handShake(socket.getOutputStream(), request);
//            var p = (i == 0) ? player1 : player2;
//
//            Thread.sleep(100);
//
//
//        }
//        try {
//            game.start();
//        } catch (ExecutionException | InterruptedException e) {
//            e.printStackTrace();
//        }

    }

    private static void handShake(OutputStream out, String data) throws NoSuchAlgorithmException, IOException {
        System.out.println(data);
        Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(data);
        match.find();
        byte[] response = ("HTTP/1.1 101 Switching Protocols\r\n"
                + "Connection: Upgrade\r\n"
                + "Upgrade: websocket\r\n"
                + "Sec-WebSocket-Accept: "
                + Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes("UTF-8")))
                + "\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        out.write(response, 0, response.length);
    }

    @Override
    public void run() {

        try(ServerSocket server = new ServerSocket(9000)) {
            while (true){
                Socket socket = server.accept();
                Scanner s = new Scanner(socket.getInputStream(), StandardCharsets.UTF_8);
                String request = s.useDelimiter("\\r\\n\\r\\n").next();
                handShake(socket.getOutputStream(), request);
                Map<String, String> cookie = getCookie(request);
                String playerName = cookie.get("name");
                String lobby = cookie.get("lobbyCode");
                Game game = lobbies.get(lobby);

                // this line is giving me trouble
                Player player = (playerName.equals(game.getPlayer1().getName()))? game.getPlayer1():game.getPlayer2();


                WebSocketView ws = new WebSocketView(socket,player);
                Thread wsThread = new Thread(ws, String.format("ws thread for player: %s", playerName));
                wsThread.start();
            }
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }



    }
//    static String getPath(String request){
//        Matcher path = Pattern.compile("GET (.*) ").matcher(request);
//        if(path.find()) {
//            return path.group(1);
//        } else {
//            //likely should throw an error or do something meaningful
//            return null;
//        }
//    }
}

