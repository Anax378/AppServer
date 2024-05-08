package net.anax.util;

import org.json.simple.JSONObject;

public class StringUtilities {
    public static boolean isInteger(String string){
        try{
            Integer.parseInt(string);
            return true;
        }catch (NumberFormatException e){
            return false;
        }
    }
    public static boolean isBoolean(String bool){
        if(bool == null){return false;}
        if(bool.equalsIgnoreCase("true") || bool.equalsIgnoreCase("false")){return true;}
        return false;
    }
    public static boolean isLong(String string){
        try{
            Long.parseLong(string);
            return true;
        }catch (NumberFormatException e){
            return false;
        }
    };
    public static String simpleWrapInJson(String value, String key){
        if(value == null){return null;}
        JSONObject json = new JSONObject();
        json.put(key, value);
        return json.toJSONString();
    }

}
