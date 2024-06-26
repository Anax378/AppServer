package net.anax.logging;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class Logger {
    public static final IAnsiPainter exceptionPrintColor = AnsiEscapeCode.Magenta;
    public static final IAnsiPainter logPrefixPrintColor = AnsiEscapeCode.Black;
    public static boolean useAnsi = true;
    public static int longestLogPrefixLength = 0;
    public static PrintStream[] printStreams = new PrintStream[]{System.out};

    //used to Log things that are out of the ordinary
    public static void log(String message, long traceId){
        putLn(addDetail("LOG", message, traceId));
    }
    //used to Log things that are normal and expected
    public static void info(String message, long traceId){
        putLn(addDetail("INFO", message, traceId));
    }
    //used to log things that should not happen even with an invalid request
    public static void error(String message, long traceId){
        putLn(addDetail("ERROR", message, traceId));
    }
    //debug statements
    public static void debug(String message, long traceId){
        putLn(addDetail("DEBUG", message, traceId));
    }
    public static void custom(String type, String message, long traceId){
        putLn(addDetail(type, message, traceId));
    }
    private static void putLn(String message){
        for(PrintStream stream : printStreams){
            stream.println(message);
        }
    }
    private static String addDetail(String type, String message, long traceId){

        String callerInfo = "not found";

        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        if(stackTrace.length >= 4){
            StackTraceElement caller = stackTrace[3];
            callerInfo = caller.getClassName() + "." + caller.getMethodName() + "(" + caller.getFileName() + ":" + caller.getLineNumber() + ")";
        }

        String time = new SimpleDateFormat("HH:mm:ss MM-dd-yyyy").format(new Date());

        String logPrefix = colorIfSupport("["+type+"]", getAnsiColorForLogType(type)) + colorIfSupport("["+traceId+"]", getTraceIdColor(traceId)) + colorIfSupport("[at " + callerInfo + " ][" + time + "|" + System.currentTimeMillis() + "] ", logPrefixPrintColor);

        if(logPrefix.length() > longestLogPrefixLength){
            longestLogPrefixLength = logPrefix.length();
        }

        int spacesToAdd = longestLogPrefixLength - logPrefix.length();
        return logPrefix + " ".repeat(spacesToAdd) + message;
    }

    public static IAnsiPainter getTraceIdColor(long traceId){
        return new AdvanceAnsiEscapeCode((byte)traceId);
    }
    public static void logException(Exception e, long traceId){
        putLn(addDetail("EXCEPTION", e.getMessage(), traceId) + ">");
        putLn(colorIfSupport(getPrintedStackTrace(e), exceptionPrintColor));
        putLn("< [Exception end]");
    }
    public static IAnsiPainter getAnsiColorForLogType(String logType){
        switch (logType){
            case "INFO" -> {return AnsiEscapeCode.Cyan;}
            case "DEBUG" -> {return AnsiEscapeCode.Yellow;}
            case "LOG" -> {return AnsiEscapeCode.White;}
            case "ERROR" -> {return AnsiEscapeCode.Red;}
            case "EXCEPTION" -> {return AnsiEscapeCode.Magenta;}
        }
        return AnsiEscapeCode.Default;
    }
    public static String getPrintedStackTrace(Exception e){
        StringBuilder builder = new StringBuilder();
        OutputStream ostream = new OutputStream() {
            @Override
            public void write(int b){
                builder.append((char)b);
            }
        };
        PrintStream printStream = new PrintStream(ostream);
        e.printStackTrace(printStream);
        return builder.toString();
    }

    public static String colorIfSupport(String message, IAnsiPainter code){
        return useAnsi ? code.color(message) : message;
    }
    public static long generateTraceId(){
        return new Random().nextLong();
    }

    interface IAnsiPainter {
        public String color(String message);
    }

    public static class AdvanceAnsiEscapeCode implements IAnsiPainter {
        public byte color;

        public AdvanceAnsiEscapeCode(byte color){
            this.color = color;
        }
        @Override
        public String color(String message) {
            return "\033[38;5;" + (((int)color) & 0xFF) + "m" + message + AnsiEscapeCode.Reset.getEscapeCode();
        }
    }
    public enum AnsiEscapeCode implements IAnsiPainter {
        Reset("0"),
        Black("30"),
        Red("31"),
        Green("32"),
        Yellow("33"),
        Blue("34"),
        Magenta("35"),
        Cyan("36"),
        White("37"),
        Default("39"),
        ;

        private String code;
        AnsiEscapeCode(String code){
            this.code = code;
        }
        public String getEscapeCode(){
            return "\033[" + code + "m";
        }
        public String color(String message){
            return this.getEscapeCode() + message + Reset.getEscapeCode();
        }

    }
}
