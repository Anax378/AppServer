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
        byte[] match = toMatch.getBytes(StandardCharsets.UTF_8);
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

    public static byte[] intToBytes(int int_){
        byte[] ret = new byte[4];

        ret[0] = (byte) (int_ >>> 24);
        ret[1] = (byte) (int_ >>> 16);
        ret[2] = (byte) (int_ >>> 8);
        ret[3] = (byte) int_;

        return ret;
    }

    public static int bytesToInt(byte[] bytes){
        int length = Math.min(bytes.length, 4);
        int result = 0;

        for (int i = 0; i < length; i++) {
            result |= (bytes[i] & 0xFF) << (8 * (3 - i));
        }
        return result;
    }

    public static byte[] fillToLen(byte[] bytes, int length, byte fillWith){
        byte[] ret = new byte[length];
        for(int i = 0; i < length; i++){
            if(i < bytes.length){
                ret[i] = bytes[i];
            }else{
                ret[i] = fillWith;
            }
        }
        return ret;
    }

    public static byte[] concatenateByteArrays(byte[]... arrays){
        int length = 0;
        for(byte[] array : arrays){
            length += array.length;
        }
        byte[] ret = new byte[length];

        int index = 0;
        for(byte[] array : arrays){
            System.arraycopy(array, 0, ret, index, array.length);
            index += array.length;
        }

        return ret;

    }

}
