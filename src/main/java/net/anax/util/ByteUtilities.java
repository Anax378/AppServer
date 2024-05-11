package net.anax.util;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class ByteUtilities {

    public static String toHexString(byte[] bytes){
        StringBuilder builder = new StringBuilder();
        for(byte b : bytes){
            builder.append(String.format("%02X", b));
        }
        return builder.toString();
    }
    public static byte[] fromHexString(String string){
        byte[] bytes = new byte[string.length()/2];

        for(int i = 0; i < string.length(); i += 2){
            String pair = string.substring(i, i+2);
            bytes[i/2] = (byte) Integer.parseInt(pair, 16);
        }

        return bytes;
    }

    public static boolean checkEndEquals(byte[] bytes, String toMatch){
        byte[] match = toMatch.getBytes(StandardCharsets.US_ASCII);
        if (match.length > bytes.length){return false;}
        for(int i = 0; i < match.length; i++){
            if(match[match.length-i-1] != bytes[bytes.length-i-1]){
                return false;
            }
        }
        return true;
    }

    public static byte[] byteListToByteArray(List<Byte> list){
        byte[] bytes = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) {
            bytes[i] = list.get(i);
        }
        return bytes;
    }

}
