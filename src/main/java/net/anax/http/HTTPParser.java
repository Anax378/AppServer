package net.anax.http;

import net.anax.logging.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

public class HTTPParser {
    private static final int CR = 13; // Carriage return
    private static final int LF = 10; // Line Feed
    private static final int SP = 32; // Space
    private static final int CL = 58; // Colon

    private int maxSectionLength = 8050;
    private int maxBodyLength = 1048576; // 1 MB worth of bytes

    public int getMaxSectionLength() {
        return maxSectionLength;
    }

    public void setMaxSectionLength(int maxSectionLength) {
        this.maxSectionLength = maxSectionLength;
    }

    int readByte(InputStream istream, ArrayList<Byte> bytes) throws IOException {
        int _byte = istream.read();
        bytes.add((byte) _byte);
        return _byte;
    }
    public HTTPRequest parseRequest(InputStream inputStream) throws IOException, HTTPParsingException {

        HTTPRequest request = new HTTPRequest();

        ArrayList<Byte> istreamBytes = new ArrayList<>();
        parseLine(inputStream, request, istreamBytes);

        if(request.getMethod() == null){
            throw new HTTPParsingException(HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST, "Bad request, no method");
        }
        if(request.getURI() == null){
            throw new HTTPParsingException(HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST, "Bad request, no URI");
        }

        parseHeaders(inputStream, request, istreamBytes);
        parseBody(inputStream, request, istreamBytes);

        byte[] bytes = new byte[istreamBytes.size()];
        for(int i = 0; i < istreamBytes.size(); i++){
            bytes[i] = istreamBytes.get(i);
        }

        request.rawInput = bytes;

        return request;
    }

    private void parseLine(InputStream inputStream, HTTPRequest request, ArrayList<Byte> bytes) throws IOException, HTTPParsingException {
        int _byte;
        StringBuilder stringBuilder = new StringBuilder();
        boolean parsedMethod = false;
        try {
            while ((_byte = readByte(inputStream, bytes)) >= 0) {
                if(stringBuilder.length() > maxSectionLength){
                    throw new HTTPParsingException(HTTPStatusCode.CLIENT_ERROR_413_REQUEST_TOO_LARGE, "Request too large, line section too long");
                }
                if (_byte == LF && !stringBuilder.isEmpty() && stringBuilder.charAt(stringBuilder.length() - 1) == CR) {
                    return;
                }
                else if(_byte == SP){
                    if(parsedMethod){
                        request.setURI(stringBuilder.toString());
                        stringBuilder.setLength(0);
                    }else{
                        HTTPMethod method = getMethod(stringBuilder.toString());
                        request.setMethod(method);
                        stringBuilder.setLength(0);
                        parsedMethod = true;
                    }
                }
                else{
                    stringBuilder.append((char)_byte);
                }


            }
        }catch (SocketTimeoutException e){
            throw new HTTPParsingException(HTTPStatusCode.CLIENT_ERROR_408_REQUEST_TIMEOUT, "Request Timed Out");
        }
    }

    private void parseHeaders(InputStream inputStream, HTTPRequest request, ArrayList<Byte> istreamBytes) throws HTTPParsingException, IOException {
        try{
            int _byte;
            StringBuilder stringBuilder = new StringBuilder();
            while((_byte = readByte(inputStream, istreamBytes)) >= 0){
                if(stringBuilder.length() > maxSectionLength){
                    throw new HTTPParsingException(HTTPStatusCode.CLIENT_ERROR_413_REQUEST_TOO_LARGE, "Header line too long");
                }
                if(_byte == LF && !stringBuilder.isEmpty() && stringBuilder.charAt(stringBuilder.length()-1) == CR){
                    stringBuilder.deleteCharAt(stringBuilder.length()-1);
                    if(stringBuilder.isEmpty()){return;}
                    parseHttpHeader(stringBuilder.toString(), request);
                    stringBuilder.setLength(0);
                }
                else{
                    stringBuilder.append((char)_byte);
                }

            }
        }catch(SocketTimeoutException e){
            throw new HTTPParsingException(HTTPStatusCode.CLIENT_ERROR_408_REQUEST_TIMEOUT, "Request Timed out");
        }

    }

    private void parseHttpHeader(String headerLine, HTTPRequest request) throws HTTPParsingException {
        String[] halves = headerLine.split(": ");
        if(halves.length != 2){
            throw new HTTPParsingException(HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST, "invalid header: " + headerLine);
        }
        HTTPHeaderType type = getHeaderType(halves[0]);
        if(type != null){
            request.setHeader(type, halves[1]);
        }
    }

    private HTTPHeaderType getHeaderType(String headerName){
        for(HTTPHeaderType type : HTTPHeaderType.values()){
            if(type.name.equals(headerName)){
                return type;
            }
        }
        return null;
    }

    private void parseBody(InputStream inputStream, HTTPRequest request, ArrayList<Byte> istreamBytes) throws HTTPParsingException {
        String contentLength = request.getHeader(HTTPHeaderType.Content_Length);
        try{
            int bytes = Integer.parseInt(contentLength);
            int _byte;
            StringBuilder stringBuilder = new StringBuilder();
            for(int i = 0; i < bytes; i++){
                _byte = readByte(inputStream, istreamBytes);

                if(stringBuilder.length() > maxBodyLength){
                    throw new HTTPParsingException(HTTPStatusCode.CLIENT_ERROR_413_REQUEST_TOO_LARGE, "body length exceeded 1 MB");
                }
                if(_byte >= 0){
                    stringBuilder.append((char)_byte);
                }else{
                    break;
                }
            }
            Logger.log("setting request body with: " + stringBuilder.toString(), -2);
            request.setBody(stringBuilder.toString());

        }catch (NumberFormatException e){
            Logger.logException(e, -2);
            request.setBody("");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

    private HTTPMethod getMethod(String name) throws HTTPParsingException{
        for(HTTPMethod method : HTTPMethod.values()){
            if (method.name().equals(name)){
                return method;
            }
        }
        throw new HTTPParsingException(HTTPStatusCode.SERVER_ERROR_501_NOT_IMPLEMENTED, "not implemented method: " + name);
    };

}
