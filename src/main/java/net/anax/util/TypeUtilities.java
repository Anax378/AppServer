package net.anax.util;

public class TypeUtilities {
    public static boolean canCast(Object o, Class<?> c){
        try {
            c.cast(o);
            return true;
        }catch (ClassCastException e){
            return false;
        }
    }

}
