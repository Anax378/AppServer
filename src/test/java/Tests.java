import org.example.http.*;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class Tests {
    private static final char CR = 13;
    private static final char LF = 10;
    private static final char SP = 32;

    private static final String CRLF = ""+CR+LF;

    TestInput[] testInputs = {
            new TestInput("GET v1/api/database/user HTTP/1.1" + CRLF +
                    "Host: www.example.host" + CRLF +
                    "Content-Length: 12" + CRLF +
                    CRLF +
                    "{user_id: 0}", HTTPStatusCode.OK_200, "Valid Request"),

            new TestInput("TROLL v1/api/database/user HTTP/1.1" + CRLF +
                    "Host: www.example.host" + CRLF +
                    "Content-Length: 12" + CRLF +
                    CRLF +
                    "{user_id: 0}", HTTPStatusCode.SERVER_ERROR_501_NOT_IMPLEMENTED, "Invalid Method"),

            new TestInput("GET  v1/api/database/user HTTP/1.1" + CRLF +
                    "Host: www.example.host" + CRLF +
                    "Content-Length: 12" + CRLF +
                    CRLF +
                    "{user_id: 0}", HTTPStatusCode.OK_200, "too many spaces after method", true),

            new TestInput("GET v1/api/database/user  HTTP/1.1" + CRLF +
                    "Host: www.example.host" + CRLF +
                    "Content-Length: 12" + CRLF +
                    CRLF +
                    "{user_id: 0}", HTTPStatusCode.OK_200, "too many spaces after URI", true),

            new TestInput("i am a stick" + CRLF +
                    "Host: www.example.host" + CRLF +
                    "Content-Length: 12" + CRLF +
                    CRLF +
                    "{user_id: 0}", HTTPStatusCode.SERVER_ERROR_501_NOT_IMPLEMENTED, "invalid line"),

            new TestInput("LOL Take THis", HTTPStatusCode.SERVER_ERROR_501_NOT_IMPLEMENTED, "no CRLFs"),
            new TestInput("TEXT_BLOCK", HTTPStatusCode.CLIENT_ERROR_400_BAD_REQUEST, "no spaces nor CRLFs"),
            new TestInput("GET v1/api/database/user HTTP/1.1" + CRLF +
                    "Host: www.example.host" + CRLF +
                    "Content-Length: 272" + CRLF +
                    CRLF +
                    "{\n" +
                    "users_ids:[0, 8, 9, 2, 48, 61]\n" +
                    "user_names:[\"user0\", \"name_of_user\", \"potato456456\", \"grrrrfasd\"]\n" +
                    "class:{\n" +
                    "name: \"name of the class or smt\"\n" +
                    "tasks:[\n" +
                    "\"do this and that\",\n" +
                    "\"do that and this\",\n" +
                    "\"dont not do not doing the task not incomplete\"\n" +
                    "\"TEST TASK OR SMT\"\n" +
                    "]\n" +
                    "}\n" +
                    "}", HTTPStatusCode.OK_200, "json in body", true),

    };

    @Test
    void test() throws IOException {
        HTTPParser parser = new HTTPParser();
        for(int i = 0; i < testInputs.length; i++){
            InputStream inputStream = new ByteArrayInputStream(testInputs[i].request.getBytes(StandardCharsets.US_ASCII));

            if(testInputs[i].isExperimental){
                try {
                    HTTPRequest request = parser.parseRequest(inputStream);
                    System.out.println("------------");
                    System.out.println("Test input " + i + " " + testInputs[i].message + " Report: ");
                    System.out.println("method: " + request.getMethod().name() + ", URI: \"" + request.getURI()+"\"");
                    for(HTTPHeaderType type : HTTPHeaderType.values()){
                        System.out.println("// " + type.name() + ": " + request.getHeader(type));
                    }
                    String body = request.getBody().replace("\n", "\n// ");
                    System.out.println("// " + body);
                    System.out.println("------------");
                } catch (HTTPParsingException e) {
                    e.printStackTrace();
                }
                continue;

            }
            try{
                parser.parseRequest(inputStream);
                if(testInputs[i].responseStatusCode != HTTPStatusCode.OK_200){
                    System.out.println("test input " + i + " " + testInputs[i].message + " failed");
                    System.out.println("Expected " + testInputs[i].responseStatusCode + " got " + HTTPStatusCode.OK_200.name());
                    assertEquals(testInputs[i].responseStatusCode, HTTPStatusCode.OK_200);
                }else{
                    System.out.println("Passed test " + i + " " + testInputs[i].message);
                }
            }catch (HTTPParsingException e){
                if(e.getStatusCode() != testInputs[i].responseStatusCode){
                    System.out.println("test input " + i + " " + testInputs[i].message + " failed");
                    System.out.println("Expected " + testInputs[i].responseStatusCode + " got " + e.getStatusCode().name());
                    e.printStackTrace();
                    assertEquals(testInputs[i].responseStatusCode, e.getStatusCode());
                }else{
                    System.out.println("Passed test " + i + " " + testInputs[i].message);
                }
            }
        }

    }

    static class TestInput{
        public String request;
        public HTTPStatusCode responseStatusCode;
        public boolean isExperimental;
        public String message;

        public TestInput(String request, HTTPStatusCode response, String message) {
            this.request = request;
            this.responseStatusCode = response;
            this.message = message;
            this.isExperimental = false;
        }
        public TestInput(String request, HTTPStatusCode response, String message, boolean isExperimental) {
            this.request = request;
            this.responseStatusCode = response;
            this.message = message;
            this.isExperimental = isExperimental;
        }
    }

}
