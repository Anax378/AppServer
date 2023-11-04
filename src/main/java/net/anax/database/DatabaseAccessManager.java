package net.anax.database;

import net.anax.VirtualFileSystem.*;
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
    private VirtualFolder ROOT_FOLDER;

    private SpecifiedDatabaseStructure specificDatabase = null;

    Connection connection;

    String database_username;
    String database_password;
    String database_address;

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

        specificDatabase = SpecifiedDatabaseStructure.getINSTANCE();
        specificDatabase.setConnection(connection);
        ROOT_FOLDER = specificDatabase.getROOT();

    }


    public static DatabaseAccessManager getInstance(){
        if(instance == null) {
            instance = new DatabaseAccessManager();
        }
        return instance;
    }

    public String getDataFromURI(String URI,  AuthorizationProfile auth){
        if(URI.charAt(0) == '/'){
            URI = URI.substring(1);
        }

        String[] parts = URI.split("/");
        if(parts.length == 0){return null;}
        VirtualPathNode lastNode = null;
        for(int i = parts.length - 1; i >= 0; i--){
            lastNode = new VirtualPathNode(parts[i], lastNode);
        }
        VirtualFile file = ROOT_FOLDER.getFileFromPATH(lastNode, auth);
        if(file != null){
            return file.readData(auth);
        }
        System.out.println("404 not found: " + URI);
        return null;
    }

}
