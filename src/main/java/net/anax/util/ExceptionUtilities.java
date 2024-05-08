package net.anax.util;

public class ExceptionUtilities {
    public static <E extends Exception> E throwAgain(E e) throws E{
        E exception;
        try {
            exception = (E) e.getClass().getConstructor(String.class).newInstance(e.getMessage());
            exception.initCause(e);
        }catch(Exception ex){
            throw e;
        }
        throw exception;
    }
}
