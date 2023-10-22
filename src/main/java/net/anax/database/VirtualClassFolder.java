package net.anax.database;

import net.anax.VirtualFileSystem.AbstractVirtualFolder;
import net.anax.VirtualFileSystem.AuthorizationProfile;
import net.anax.VirtualFileSystem.VirtualFile;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class VirtualClassFolder extends AbstractVirtualFolder {
    Connection connection;
    public VirtualClassFolder(String name, Connection connection) {
        super(name);
        this.connection = connection;
    }

    @Override
    public AbstractVirtualFolder getFolder(String name,  AuthorizationProfile auth) {
        switch(name){
            case "id" -> {return this;}
            case "admin_user_id" -> {
                try {
                    ResultSet result = regularQuery("admin_user_id");
                    if(!result.next()){return null;}
                    int admin_user_id = result.getInt("admin_user_id");
                    PreparedStatement statement = connection.prepareStatement("SELECT * FROM user WHERE user.id = ?");
                    statement.setString(1, ""+admin_user_id);
                    result = statement.executeQuery();
                    if(!result.next()){return null;}
                    return new VirtualUserFolder(""+admin_user_id, connection);
                } catch (SQLException e) {
                    return null;
                }
            }
        }
        return null;
    }

    @Override
    public VirtualFile getFile(String name,  AuthorizationProfile auth) {
        String data = null;
        switch(name){
            case "name" -> data = getClassName();
            case "admin_user_id" -> data = getAdminUserId();
            case "user_ids" -> data = getUserIds();
            case "task_ids" -> data = getTaskIds();
            case "id" -> data = getId();
        }
        String finalData = data;
        return new VirtualFile(name) {
            @Override
            public String readData() {
                return finalData;
            }
        };
    }

    private String getClassName(){
        try {
            ResultSet result = regularQuery("name");
            if(result == null){return null;}
            if(!result.next()){return null;}
            JSONObject data = new JSONObject();
            data.put("name", result.getString("name"));
            return data.toJSONString();
        } catch (SQLException e) {
            return null;
        }
    }

    private String getAdminUserId(){
        try{
            ResultSet result = regularQuery("admin_user_id");
            if(result == null){return null;}
            if(!result.next()){return null;}
            JSONObject data = new JSONObject();
            data.put("admin_user_id", result.getInt("admin_user_id"));
            return data.toJSONString();
        }catch (SQLException e){
            return null;
        }
    }

    private String getUserIds(){
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT user_class.user_id FROM class INNER JOIN user_class ON user_class.class_id=class.id WHERE class.id=?");
            statement.setString(1, this.getName());
            ResultSet result = statement.executeQuery();
            JSONObject data = new JSONObject();
            JSONArray ids = new JSONArray();
            while(result.next()){
                ids.add(result.getInt("user_id"));
            }
            data.put("user_ids", ids);
            return data.toJSONString();
        } catch (SQLException e) {
            return null;
        }
    }

    private String getTaskIds(){
        try{
            PreparedStatement statement = connection.prepareStatement("SELECT task.id FROM task WHERE task.class_id = ?");
            statement.setString(1, this.getName());
            ResultSet result = statement.executeQuery();
            JSONObject data = new JSONObject();
            JSONArray ids = new JSONArray();
            while(result.next()){
                ids.add(result.getInt("id"));
            }
            data.put("ids", ids);
            return data.toJSONString();
        }catch(SQLException e){
            return null;
        }
    }

    String getId(){
        try {
            ResultSet result = regularQuery("id");
            if(result == null){return null;}
            if(!result.next()){return null;}
            JSONObject data = new JSONObject();
            data.put("id",result.getInt("id"));
            return data.toJSONString();
        } catch (SQLException e) {
            return null;
        }
    }

    ResultSet regularQuery(String name){
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT " + name + " FROM class WHERE id=?");
            statement.setString(1, this.getName());
            ResultSet result = statement.executeQuery();
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}

