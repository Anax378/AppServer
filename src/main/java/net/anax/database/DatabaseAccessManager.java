package net.anax.database;

import net.anax.VirtualFileSystem.*;
import net.anax.cryptography.KeyManager;
import net.anax.endpoint.EndpointFailedException;
import net.anax.endpoint.EndpointManager;
import net.anax.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;

public class DatabaseAccessManager {
    private static DatabaseAccessManager instance;

    EndpointManager endpointManager;

    Connection connection;

    String database_username;
    String database_password;
    String database_address;

    KeyManager keyManager;

    public void setKeyManager(KeyManager keyManager){
        getInstance().keyManager = keyManager;
    }
    public DatabaseAccessManager(){
        try {
            JSONObject config = (JSONObject) new JSONParser().parse(new FileReader("config.json"));
            database_username = (String) config.get("database_username");
            database_address = (String) config.get("database_address");
            database_password = (String) config.get("database_password");
        } catch (FileNotFoundException e) {
            Logger.log("Could not find config file");
            throw new RuntimeException(e);
        } catch (IOException e) {
            Logger.log("Cannot read config ");
            throw new RuntimeException(e);
        } catch (ParseException e) {
            Logger.log("Could not parse config file");
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://" + database_address + "/schoolapp", database_username, database_password);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        endpointManager = new EndpointManager(connection);
    }


    public static DatabaseAccessManager getInstance(){
        if(instance == null) {
            instance = new DatabaseAccessManager();
        }
        return instance;
    }
    public String handleRequest(String URI, String payload, AuthorizationProfile auth) throws EndpointFailedException {
        if(URI.charAt(0) == '/'){
            URI = URI.substring(1);
        }

        ;System.out.println("URI: " + URI);

        JSONParser parser = new JSONParser();
        JSONObject data = null;

        try {
            data = (JSONObject) parser.parse(payload);
            return endpointManager.callEndpoint(URI, data, auth);

        } catch (ParseException e) {
            throw new EndpointFailedException("invalid payload", EndpointFailedException.Reason.UnexpectedError);
        }catch (EndpointFailedException e){
            Logger.log(e.getMessage());
            throw e;
        }
    }

    public KeyManager getKeyManager(){
        return this.keyManager;
    }

}
