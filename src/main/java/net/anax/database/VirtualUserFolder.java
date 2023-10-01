package net.anax.database;

import net.anax.VirtualFileSystem.AbstractVirtualFolder;
import net.anax.VirtualFileSystem.VirtualFile;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.sql.*;
import java.util.Objects;

public class VirtualUserFolder extends AbstractVirtualFolder {
    Connection connection;
    public VirtualUserFolder(String name, Connection connection) {
        super(name);
        this.connection = connection;
    }

    @Override
    public AbstractVirtualFolder getFolder(String name) {
        if(Objects.equals(name, "id")){return this;}
        return null;
    }

    @Override
    public VirtualFile getFile(String name) {
        String data = null;
        switch (name){
            case "username" -> data = getUsername();
            case "id" -> data = getId();
            case "password_hash" -> data = getPasswordHash();
            case "class_ids" -> data = getClassIds();
            case "task_ids" -> data = getTaskIds();
            default -> {return null;}
        }
        final String finalData = data;
        return new VirtualFile(name) {
            @Override
            public String readData() {
                return finalData;
            }
        };
    }

    String getUsername(){
        try {
            ResultSet result = regularQuery("username");
            if(result == null){return null;}
            if(!result.next()){return null;}
            JSONObject data = new JSONObject();
            data.put("username", result.getString("username"));
            return data.toJSONString();
        } catch (SQLException e) {
            return null;
        }
    }

    String getPasswordHash(){
        try {
            ResultSet result = regularQuery("password_hash");
            if(!result.next()){return null;}
            JSONObject data = new JSONObject();
            data.put("password_hash", result.getInt("password_hash"));
            return data.toJSONString();
        } catch (SQLException e) {
            return null;
        }
    }

    String getId(){
        try {
            ResultSet result = regularQuery("id");
            if(!result.next()){return null;}
            JSONObject data = new JSONObject();
            data.put("id", result.getInt("id"));
            return data.toJSONString();
        } catch (SQLException e) {
            return null;
        }
    }

    String getClassIds(){
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT class.id FROM class INNER JOIN user_class ON class.id = user_class.class_id WHERE user_class.user_id = ?");
            statement.setString(1, this.getName());
            ResultSet result = statement.executeQuery();
            JSONObject data = new JSONObject();
            JSONArray array = new JSONArray();
            while(result.next()){
                array.add(result.getInt("id"));
            }
            data.put("ids", array);
            return data.toJSONString();

        } catch (SQLException e) {
            return null;
        }
    }

    String getTaskIds(){
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT task.id FROM user INNER JOIN user_task ON user.id = user_task.user_id INNER JOIN task ON user_task.task_id = task.id WHERE user_task.user_id = ?");
            statement.setString(1, this.getName());

            ResultSet result = statement.executeQuery();

            JSONObject data = new JSONObject();
            JSONArray array = new JSONArray();
            while(result.next()){
                array.add(result.getInt("id"));
            }
            data.put("ids", array);
            return data.toJSONString();
        } catch (SQLException e) {
            return null;
        }
    }

    ResultSet regularQuery(String name){
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT " + name + " FROM user WHERE id=?");
            statement.setString(1, this.getName());
            ResultSet result = statement.executeQuery();
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
}
