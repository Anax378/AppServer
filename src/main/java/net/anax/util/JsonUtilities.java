package net.anax.util;

import org.json.simple.JSONObject;

import java.util.Arrays;

public class JsonUtilities {
    public static boolean validateKeys(String[] keys, Class<?>[] types, JSONObject data){

        for(int i = 0; i < keys.length; i++){
            if(!data.containsKey(keys[i])){
                return false;
            }
            if(!TypeUtilities.canCast(data.get(keys[i]), types[i])){
                return false;
            }
        }
        return true;
    }
}
