package net.anax.util;

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

}
