package webscoket_endpoint;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.Random;

/**
 *
 * WsFrame is the first class in a plan to create a more cohesive and user-friendly API for WebSockets
 * very much still under development;
 *
 * */
public class WsFrame {
    private boolean fin;
    private int opCode;
    private long length;
    private byte[] payload;
    private StatusCode sc;

    private WsFrame() {
        this(new byte[0],-1, false);
    }

    private WsFrame(byte[] payload, StatusCode sc) {
        this(payload, 0x8, true);
        this.sc = sc;
    }

    private WsFrame(byte[] payload, int opCode, boolean fin) {
        this.payload = payload;
        this.length = this.payload.length;
        this.opCode = opCode;
        this.fin = fin;
    }

    public static WsFrame createContinuationFrame(byte[] payload, boolean fin){
        return new WsFrame(payload, 0x0, fin);
    }
    public static WsFrame createContinuationFrame(String msg, boolean fin) {
        return new WsFrame(msg.getBytes(StandardCharsets.UTF_8), 0x0, fin);
    }


    public static WsFrame createTextFrame(String msg, boolean fin){
        return new WsFrame(msg.getBytes(StandardCharsets.UTF_8), 0x1, fin);
    }


    public static WsFrame createBinaryFrame(byte[] payload, boolean fin){
        return new WsFrame(payload, 0x2, fin);
    }


    public static WsFrame createCloseFrame(String reason, StatusCode sc){
        return new WsFrame(reason.getBytes(StandardCharsets.UTF_8), sc);
    }
    public static WsFrame createCloseFrame(byte[] reason, StatusCode sc){
        return new WsFrame(reason, sc);
    }


    public static WsFrame createPing() {
        var bytes = new byte[64];
        new Random().nextBytes(bytes);
        return new WsFrame(bytes, 0x9, true);
    }


    public static WsFrame createPong(byte[] pingData) {
        return new WsFrame(pingData, 0x9, true);
    }

    static WsFrame readFrame(InputStream in) {
        WsFrame f = null;
        try {
            var firstByte = in.read();
            boolean fin = firstByte >>> 7 == 1;
            int opCode = firstByte & 0b00001111;

            int secondByte = in.read();
            boolean payloadIsMasked = secondByte >>> 7 == 1;

            var l = secondByte & 0b01111111;
            long length;

            if (l > 125) {
                byte[] lengthbytes;
                if (l > 126) {
                    lengthbytes = new byte[8];
                } else {
                    lengthbytes = new byte[2];
                }
                for (int i = 0; i < lengthbytes.length; i++) {
                    lengthbytes[i] = (byte) in.read();
                }
                length = bytesToLong(lengthbytes, 0, lengthbytes.length);
            } else {
                length = l;
            }

            byte[] payload = new byte[(int) length];
            if(payloadIsMasked){
                byte[] key = new byte[4];
                byte[] encoded = new byte[(int) length];
                int totalBytesRead = 0;
                int bytesReadPerIteration = 0;

                while (totalBytesRead != key.length){
                    bytesReadPerIteration = in.read(key, totalBytesRead, key.length - totalBytesRead);
                    if(bytesReadPerIteration != -1) {
                        totalBytesRead += bytesReadPerIteration;
                    } else {
                        throw new EOFException("End of stream while reading key.");
                    }
                }
                totalBytesRead = 0;
                while(totalBytesRead != encoded.length){
                    bytesReadPerIteration = in.read(encoded, totalBytesRead, encoded.length - totalBytesRead);
                    totalBytesRead += bytesReadPerIteration;
                }

                for (int i = 0; i < encoded.length; i++) {
                    payload[i] = (byte) (encoded[i] ^ key[i & 0x3]);
                }
            } else {
                int bytesRead = 0;
                while(bytesRead != payload.length || bytesRead != -1){
                    bytesRead += in.read(payload, bytesRead, payload.length - bytesRead);
                }
            }
            f = new WsFrame(payload, opCode, fin);

        } catch(IOException e){
            e.printStackTrace();
        }
        return f;
    }

    private static long bytesToLong(final byte[] bytes, final int offset, final int numberSize) {
        long result = 0;
        for (int i = offset; i < numberSize + offset; i++) {
            result <<= 8;
            result |= (bytes[i] & 0xFF);
        }
        return result;
    }


    public boolean isFin() {
        return fin;
    }

    public int getOpCode() {
        return opCode;
    }

    public long getLength() {
        return length;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void write(OutputStream out) {
        var firstByte = 0;
        if(fin) firstByte |= 10000000;
        firstByte |= opCode;
        try {
            out.write((byte) firstByte);

            if(opCode == 0x8){
                //Close control frames must include a 16-bit status code
                length +=2;
            }
            var secondByte = 0;
            if (length > 0xFFFF) {
                secondByte = 0b01111111;
                out.write(secondByte);
                out.write(longToBytes(length));
            } else if (length > 125) {
                secondByte = 0b01111110;
                out.write(secondByte);
                out.write(shortToByes((short) length));
            } else {
                secondByte = (int) length;
                out.write(secondByte);
            }

            if(opCode == 0x8){
                //write status code
                out.write(shortToByes(sc.getValue()));
            }
            out.write(payload);
        } catch (IOException e){
            e.printStackTrace();
        }

    }

    private static byte[] shortToByes(short s) {
        var byteArr = new byte[Short.BYTES];
        for (int i = 0; i < Short.BYTES; i++) {
            byteArr[i] = (byte) (s & 0xFF);
            s >>>= 8;
        }
        return byteArr;
    }

    private static byte[] longToBytes(long l){
        var byteArr = new byte[Long.BYTES];
        for (int i = 0; i < Long.BYTES; i++) {
            byteArr[i] = (byte) (l & 0xFF);
            l >>>=8;
        }
        return byteArr;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WsFrame)) return false;
        WsFrame frame = (WsFrame) o;
        return getOpCode() == frame.getOpCode() &&
                Arrays.equals(getPayload(), frame.getPayload());
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(getOpCode());
        result = 31 * result + Arrays.hashCode(getPayload());
        return result;
    }


    public enum StatusCode{
        NORMAL_CLOSURE((short)1000),
        GOING_AWAY((short)1000),
        PROTOCOL_ERROR((short)1000),
        DATA_TYPE_NOT_ACCEPTED((short)1000),
        DATA_NOT_CONSISTENT_WITH_MSG_TYPE((short)1000),
        POLICY_VIOLATION((short)1000),
        MSG_TOO_LARGE((short)1000);

        private final short value;

        StatusCode(short i) {
            this.value = i;
        }

        public short getValue() {
            return value;
        }
    }
}
