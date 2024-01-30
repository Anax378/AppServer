package net.anax.util;

import org.json.simple.JSONObject;

import java.util.Arrays;

public class JsonUtilities {
    public static boolean validateKeys(String[] keys, Class<?>[] types, JSONObject data){
        ;System.out.println("-------------validateKeys start---------");
        ;System.out.println(Arrays.toString(keys));
        ;System.out.println(Arrays.toString(types));

        for(int i = 0; i < keys.length; i++){
            if(!data.containsKey(keys[i])){
                ;System.out.println("does not contain key: " + keys[i]);
                return false;
            }
            if(!TypeUtilities.canCast(data.get(keys[i]), types[i])){
                ;System.out.println(data.get(keys[i]) + " cannot be cast to : " + types[i]);
                return false;
            }
        }
        ;System.out.println("-------------validateKeys end---------");
        return true;
    }
}
