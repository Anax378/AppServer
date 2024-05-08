package net.anax.util;

import org.json.simple.JSONArray;
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
    public static boolean isBooleanArray(JSONArray array){
        if(array.isEmpty()){return true;}
        return array.get(0) instanceof Boolean;
    }

    public static boolean isLongArray(JSONArray array){
        if(array.isEmpty()){return true;}
        return array.get(0) instanceof Long;
    }

    public static <E extends Exception> int extractInt(JSONObject data, String intName, E e) throws E{

        if(!data.containsKey(intName)){throw ExceptionUtilities.throwAgain(e);}
        if(!(data.get(intName) instanceof Long)){
            if(data.get(intName) instanceof String){
                if(StringUtilities.isInteger((String) data.get(intName))){
                    return Integer.parseInt((String)data.get(intName));
                }
            }
        }else{
            return (int)(long)data.get(intName);
        }
        throw ExceptionUtilities.throwAgain(e);
    }

    public static <E extends Exception> String extractString(JSONObject data, String stringName, E e) throws E {
        if(!data.containsKey(stringName)){throw ExceptionUtilities.throwAgain(e);}
        if(data.get(stringName) instanceof String){return (String) data.get(stringName);}
        throw ExceptionUtilities.throwAgain(e);
    }

    public static <E extends Exception> boolean extractBoolean(JSONObject data, String name, E e) throws E{
        if(!data.containsKey(name)){throw ExceptionUtilities.throwAgain(e);}
        if(data.get(name) instanceof Boolean){return (Boolean)data.get(name);}
        if(data.get(name) instanceof String){
            if(StringUtilities.isBoolean((String) data.get(name))){
                return Boolean.parseBoolean((String) data.get(name));
            }
        }
        throw e;
    }

    public static <E extends Exception> JSONArray extractJSONArray(JSONObject data, String arrayName, E e) throws E{
        if(!data.containsKey(arrayName)){throw ExceptionUtilities.throwAgain(e);}
        if(!(data.get(arrayName) instanceof JSONArray)){throw ExceptionUtilities.throwAgain(e);}
        return (JSONArray) data.get(arrayName);
    }

    public static <E extends Exception> JSONObject extractJSONObject(JSONObject data, String name, E e) throws E {
        if(!data.containsKey(name)){throw e;}
        if(data.get(name) instanceof JSONObject){return (JSONObject) data.get(name);}
        throw e;
    }
    public static <E extends Exception> JSONObject extractJSONObjectFromJSONArray(JSONArray array, int index, E e) throws E {
        if(index >= array.size() || index < 0){throw e;}
        if(array.get(index) instanceof JSONObject){return (JSONObject) array.get(index);}
        throw e;
    }
}
