package net.anax.database;

import net.anax.VirtualFileSystem.AbstractVirtualFolder;
import net.anax.VirtualFileSystem.VirtualFile;
import net.anax.VirtualFileSystem.VirtualFolder;
import net.anax.VirtualFileSystem.VirtualPathNode;
import net.anax.logging.Logger;

import java.sql.*;

public class DatabaseAccessManager {
    private static DatabaseAccessManager instance;

    private final VirtualFolder ROOT_FOLDER = new VirtualFolder("root");
    Connection connection;

    public DatabaseAccessManager(){
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/test", "java", "password");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        AbstractVirtualFolder USERS = new AbstractVirtualFolder("users") {
            @Override
            public AbstractVirtualFolder getFolder(String name) {
                try{
                    int user_id = Integer.parseInt(name);
                    String sdlQuery = "SELECT * FROM user WHERE id = ?";
                    PreparedStatement statement = connection.prepareStatement(sdlQuery);
                    statement.setString(1, ""+user_id);
                    ResultSet result = statement.executeQuery();
                    if(result.next()){
                        return new VirtualUserFolder(name, connection);
                    }
                    Logger.log("no result for the user id " + user_id);
                    return null;
                }catch(NumberFormatException e){
                    Logger.log("invalid username for " + name);
                    return null;
                } catch (SQLException e) {
                    Logger.log("sdl exception for " + name);
                    e.printStackTrace();
                    return null;
                }

            }

            @Override
            public VirtualFile getFile(String name) {
                return null;
            }
        };

        ROOT_FOLDER.addFolder(USERS);




    }
    public static DatabaseAccessManager getInstance(){
        if(instance == null) {
            instance = new DatabaseAccessManager();
        }
        return instance;
    }

    public String getDataFromURI(String URI){
        if(URI.charAt(0) == '/'){
            URI = URI.substring(1);
        }

        String[] parts = URI.split("/");
        if(parts.length == 0){return null;}
        VirtualPathNode lastNode = null;
        for(int i = parts.length - 1; i >= 0; i--){
            lastNode = new VirtualPathNode(parts[i], lastNode);
        }
        VirtualFile file = ROOT_FOLDER.getFileFromPATH(lastNode);
        if(file != null){
            return file.readData();
        }
        System.out.println("404 not found: " + URI);
        return null;
    }

}
