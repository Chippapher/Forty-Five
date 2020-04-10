package server;

import gameLogic.Card;
import gameLogic.Player;
import gameLogic.PlayerView;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class WebSocketView implements PlayerView, Runnable{
    CompletableFuture<Card> card;
    CompletableFuture<Optional<Card>> rob;
    Deque<Frame> watingPings = new ArrayDeque<>(5);
    Player player;
    InputStream in;
    OutputStream out;

    Lock inLock;
    Lock outLock;
    boolean open;
    private CompletableFuture<Boolean> rematch;

    public WebSocketView(Socket socket, Player p ) throws IOException {
        in = socket.getInputStream();
        out = socket.getOutputStream();
        open = true;
        player = p;
        inLock = new ReentrantLock();
        outLock = new ReentrantLock();

        //registerPlayerView has to be the last step because otherwise code in this class will trigger with a partially initialized object
        player.registerPlayerView(this);
    }

    private void completeCard(JSONObject payload) {
        if (card != null) {
            Card data = new Card(payload.getString("data"));
            card.complete(data);
            addToPlayZone(data);
        }
    }


    private void completeRob(JSONObject payload) {
        if (rob != null) {
            Object data = payload.get("data");
            if(data instanceof String){
                rob.complete(Optional.of(new Card((String)data)));
                setTrump(null);
                notifyHandChange();
            } else {
                rob.complete( Optional.empty());
            }


        }
    }
    private void completeRematch(JSONObject payload){
        rematch.complete(payload.getBoolean("data"));
    }
    private JSONObject getMsgObject(String method, Object data){
        JSONObject msg = null;
        try {
            msg = new JSONObject(String.format("{method: %s, data:%s}", method, data));
        }catch (JSONException e){
            e.printStackTrace();
            System.err.printf("message causing err: method:%s%n  object:%s%n", method, data.toString());
            System.err.printf("object type: :%s", data.getClass());
        }
        return msg;
    }
    private JSONObject getMsgObject(String method){
        return new JSONObject(String.format("{method:%s}", method));
    }

    private void pongReceived(Frame frame) {
       watingPings.remove(Frame.createFrame(0X9, frame.getDecodedPayload()));
    }
    private void sendPong(Frame frame)  {
        var f = Frame.createFrame(0xA, frame.getDecodedPayload());
        f.write(out, outLock);
    }
    private void sendMessage(JSONObject msg) {
        byte[] bytes = msg.toString().getBytes(StandardCharsets.UTF_8);
        Frame frame = Frame.createFrame(0x1, bytes);
        frame.write(out, outLock);
    }

    public void run() {
        var msg = "";
        while(open){
            Frame frame = Frame.readFrame(in);
            switch (frame.opCode){
                case 0x0, 0x1 -> {
                    if (frame.isFin()){
                        mux(frame.getDecodedPayload());
                    } else {
                        msg = msg.concat(new String(frame.getDecodedPayload()));
                    }}
                case 0x8 -> {open = false;
                    System.out.println("connection closed");}
                case 0x9 -> sendPong(frame);
                case 0xA -> pongReceived(frame);

                default -> {
                    var f = Frame.createFrame(0x8, "Unsupported Opcode".getBytes(StandardCharsets.UTF_8));
                    f.write(out, outLock);
                    open = false;
                }
            }
        }
        try {
            close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close() throws IOException {
        in.close();
        out.close();
    }

    private void mux(byte[] decodedPayload) {
        JSONObject payload = new JSONObject(new String(decodedPayload, StandardCharsets.UTF_8));
        JSONObject msg = null;
        switch (payload.getString("method")){
            case "getHand" -> msg = getMsgObject("getHand", player.getHand());
            case "getEligiblePlays" -> msg = getMsgObject("getEligiblePlays", player.getEligiblePlays());
            case "getCard" -> completeCard(payload);
            case "getRob" -> completeRob(payload);
            case "promptRematch" -> completeRematch(payload);
            case "getName" -> msg = getMsgObject("getName", player.getName());
            case "getOpponentName" -> msg = getMsgObject("getOpponentName", player.getOpponentName());
        }
        if (!(msg == null) ){
            sendMessage(msg);
        }
    }


    @Override
    public CompletableFuture<Card> getCard() {
        this.card = new CompletableFuture<>();
        sendMessage(getMsgObject("getCard"));
        return card;
    }

    @Override
    public CompletableFuture<Optional<Card>> getRob() {
        this.rob = new CompletableFuture<>();
        sendMessage(getMsgObject("getRob"));
        return rob;
    }

    @Override
    public void setTrump(Card c) {
        sendMessage(getMsgObject("setTrump", c));
    }

    @Override
    public void addToPlayZone(Card c) {
        sendMessage(getMsgObject("addToPlayzone", c));
    }

    @Override
    public void resetPlayZone() {
        sendMessage(getMsgObject("resetPlayzone"));
    }

    @Override
    public void setOpponentMatchPoints(int points) {
        sendMessage(getMsgObject("setOpponentMatchPoints", points));
    }

    @Override
    public void setOpponentHandPoints(int points) {
        sendMessage(getMsgObject("setOpponentHandPoints", points));
    }

    @Override
    public void setMatchPoints(int points) {
        sendMessage(getMsgObject("setMatchPoints", points));
    }

    @Override
    public void setHandPoints(int points) {
        sendMessage(getMsgObject("setHandPoints", points));
    }

    @Override
    public void notifyHandChange() {
        sendMessage(getMsgObject("notifyHandChange"));
    }

    @Override
    public CompletableFuture<Boolean> promptRematch(String winner){
        sendMessage(getMsgObject("promptRematch", winner));
        this.rematch = new CompletableFuture<>();
        return rematch;
    }

    @Override
    public void reset() {
        resetPlayZone();
    }


    static class Frame{


        private boolean fin;
        private int opCode;
        private long length;
        private byte[] decodedPayload;
        Lock writeLock;
        Lock readLock;

        public boolean isFin() {
            return fin;
        }

        public int getOpCode() {
            return opCode;
        }

        public long getLength() {
            return length;
        }

        public byte[] getDecodedPayload() {
            return decodedPayload;
        }

        public void write(OutputStream out, Lock lock) {
            lock.lock();
            String writtenSoFar = "";
            var firstByte = 0;
            if(fin) firstByte |= 10000000;
            firstByte |= opCode;
            writtenSoFar += firstByte + " ";
            try {
                out.write((byte) firstByte);

                var secondByte = 0;
                if (length > 0xFFFF) {
                    secondByte = 0b01111111;
                    writtenSoFar += secondByte + " ";
                    out.write(secondByte);
                    for (int i = 0; i < Long.BYTES; i++) {
                        writtenSoFar += (length & 0xFF) + " ";
                        out.write((byte) (length & 0xFF));
                        length >>>= 8;
                    }
                } else if (length > 125) {
                    secondByte = 0b01111110;
                    writtenSoFar += secondByte + " ";
                    out.write(secondByte);
                    for (int i = 0; i < Short.BYTES; i++) {
                        writtenSoFar += (length & 0xFF) + " ";
                        out.write((byte) (length & 0xFF));
                        length >>>= 8;
                    }
                } else {
                    secondByte = (int) length;
                    writtenSoFar += (length & 0xFF) + " ";
                    out.write(secondByte);
                }
                writtenSoFar += new String(decodedPayload, StandardCharsets.UTF_8) + "\n";
                out.write(decodedPayload);
                System.out.println(writtenSoFar);
                lock.unlock();
            } catch (IOException e){
                e.printStackTrace();
            }

        }

        static Frame createFrame(int opCode, byte[] data){
            var f = new Frame();
            f.fin = true;
            f.opCode = opCode;
            f.length = data.length;
            f.decodedPayload = data;
            f.writeLock = new ReentrantLock();
            f.readLock = new ReentrantLock();
            return f;
        }
        static Frame readFrame(InputStream in) {

            var f = new Frame();
            try {
                var firstByte = in.read();
                f.fin = firstByte >>> 7 == 1;
                f.opCode = firstByte & 0b00001111;
                f.length = getMessageLength(in);

                var req = new byte[(int) f.length + 4];
                f.decodedPayload = getMessage(in, (int) f.length, req).getBytes(StandardCharsets.UTF_8);
            } catch(IOException e){
                e.printStackTrace();
            }
            return f;
        }
        private static long getMessageLength(InputStream in) throws IOException {
            int secondByte = in.read();
            var l = secondByte & 0b01111111;
            long length = 0;

            if (l > 125) {
                byte[] lengthbytes;
                if (l > 126) {
//                    System.out.println("big message");
                    lengthbytes = new byte[8];
                } else {
//                    System.out.println("bigger Message");
                    lengthbytes = new byte[2];
                }
                for (int i = 0; i < lengthbytes.length; i++) {
                    lengthbytes[i] = (byte) in.read();
                }
                length = bytesToLong(lengthbytes, 0, lengthbytes.length);
            } else {
                length = l;
            }
            return length;
        }
        private static long bytesToLong(final byte[] bytes, final int offset, final int numberSize) {
            long result = 0;
            for (int i = offset; i < numberSize + offset; i++) {
                result <<= 8;
                result |= (bytes[i] & 0xFF);
            }
            return result;
        }
        private static String decodeMsg(byte[] msg , int length){
//        byte[] key = new byte[4];
            //high capacity storage not likely
//        byte[] encoded = new byte[(int)length];
            byte[] decoded = new byte[(int)length];


            byte[] key = new byte[4];
            byte[] encoded = new byte[(int)length];

            System.arraycopy(msg,0, key, 0, 4);
            System.arraycopy(msg, 4, encoded, 0, length);

            for (int i = 0; i < encoded.length; i++) {
                decoded[i] = (byte) (encoded[i] ^ key[i & 0x3]);
            }

            var builder = new StringBuilder();
            for (int i = 0; i < decoded.length; i++) {
                var character =String.valueOf((char)(decoded[i] & 0xFF)) ;
                builder.append(character);
            }
            return builder.toString();
        }
        private static String getMessage(InputStream in, int length, byte[] req) throws IOException {
            in.read(req);
            return decodeMsg(req, length);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Frame)) return false;
            Frame frame = (Frame) o;
            return getOpCode() == frame.getOpCode() &&
                    Arrays.equals(getDecodedPayload(), frame.getDecodedPayload());
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(getOpCode());
            result = 31 * result + Arrays.hashCode(getDecodedPayload());
            return result;
        }
    }
}
