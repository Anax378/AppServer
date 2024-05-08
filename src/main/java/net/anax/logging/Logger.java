package net.anax.logging;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class Logger {
    public static void log(String message, long traceId){
        System.out.println("[LOG] " + addDetail(message, traceId));
    }
    public static void info(String message, long traceId){
        System.out.println("[DETAIL] " + addDetail(message, traceId));
    }

    public static void error(String message, long traceId){
        System.out.println("[ERROR] " + addDetail(message, traceId));
    }
    private static String addDetail(String message, long traceId){
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        String time = new SimpleDateFormat("HH:mm:ss MM-dd-yyyy").format(new Date());
        String className = elements.length > 3 ? elements[3].getClassName() : "unknown";
        String classInstance = elements.length > 3 ? String.valueOf(elements[3].getClass()) : "unknown";
        return "[" + className +  ":" + classInstance +"][" + time + "|" + System.currentTimeMillis() + "] " + "[trace: " + traceId + "]" + message;
    }
    public static void logException(Exception e, long traceId){
        System.out.println("[Exception] " + addDetail(e.getMessage(), traceId));
    }
    public static long generateTraceId(){
        return new Random().nextLong();
    }
}
