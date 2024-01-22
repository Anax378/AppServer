package net.anax.endpoint;

import net.anax.VirtualFileSystem.AuthorizationProfile;
import net.anax.database.Authorization;
import net.anax.util.DatabaseUtilities;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserEndpointManager {
    Connection connection;

    public UserEndpointManager(Connection connection){
        this.connection = connection;
    }

    public String getUsername(int id, AuthorizationProfile auth) throws EndpointFailedException {
        if (auth.isAdmin() || auth.getId() == id || Authorization.sharesGroupWith(auth, id, connection)) {
            String username = DatabaseUtilities.queryString("username", "user", id, connection);
            if(username == null){
                throw new EndpointFailedException("could not retrieve data", EndpointFailedException.Reason.DataNotFound);
            }
            JSONObject data = new JSONObject();
            data.put("username", username);
            return data.toJSONString();

        }
        throw new EndpointFailedException("Access Denied", EndpointFailedException.Reason.AccessDenied);
    }

    public String getUser(int id, AuthorizationProfile auth) throws EndpointFailedException {
        if(auth.isAdmin() || auth.getId() == id){
            try {
                JSONObject data = new JSONObject();
                String username = DatabaseUtilities.queryString("username", "user", id, connection);
                data.put("username", username);

                PreparedStatement statement = connection.prepareStatement("SELECT task_id, is_done FROM user_task WHERE user_id=?");
                statement.setInt(1, id);
                ResultSet resultSet = statement.executeQuery();

                JSONArray task_id_array = new JSONArray();
                JSONArray is_done_array = new JSONArray();

                while(resultSet.next()){
                    task_id_array.add(resultSet.getInt("task_id"));
                    is_done_array.add(resultSet.getBoolean("is_done"));
                }


                data.put("taskIds", task_id_array);
                data.put("isTaskDone", is_done_array);

                PreparedStatement group_statement = connection.prepareStatement("SELECT group_id FROM user_group WHERE user_id=?");
                group_statement.setInt(1, id);
                ResultSet result = group_statement.executeQuery();

                JSONArray group_id_array = new JSONArray();

                while(result.next()){
                    group_id_array.add(result.getInt("group_id"));
                }

                data.put("groupIds", group_id_array);

                return data.toJSONString();

            } catch (SQLException e) {
                throw new EndpointFailedException("sql exception", EndpointFailedException.Reason.UnexpectedError);
            }


        }
        throw new EndpointFailedException("Access Denied", EndpointFailedException.Reason.AccessDenied);
    }

    public String setUsername(int id, String newUsername, AuthorizationProfile auth) throws EndpointFailedException {
        if(auth.isAdmin() || auth.getId() == id){
            try {
                PreparedStatement statement = connection.prepareStatement("UPDATE user SET username=? WHERE id=?");
                statement.setString(1, newUsername);
                statement.setInt(2, id);
                int affected = statement.executeUpdate();
                if(affected != 0){
                    return "{\"success\":true}";
                }
                throw new EndpointFailedException("nothing changed", EndpointFailedException.Reason.NothingChanged);

            } catch (SQLException e) {
                throw new EndpointFailedException("sql exception", EndpointFailedException.Reason.UnexpectedError);
            }
        }
        throw new EndpointFailedException("Access Denied", EndpointFailedException.Reason.AccessDenied);
    }

    public String createUser(String username, String password, AuthorizationProfile auth) throws EndpointFailedException {
        try {

            byte[] salt = Authorization.generateSalt();
            byte[] passwordHash = Authorization.generatePasswordHash(password, salt);

            PreparedStatement statement = connection.prepareStatement("INSERT INTO user (username, password_hash, hash_salt) VALUES (?, ?, ?);");
            statement.setString(1, username);
            statement.setBytes(2, passwordHash);
            statement.setBytes(3, salt);
            int affected = statement.executeUpdate();
            if(affected == 0){
                throw new EndpointFailedException("nothing changed", EndpointFailedException.Reason.UnexpectedError);
            }

            PreparedStatement idStatement = connection.prepareStatement("SELECT id FROM user WHERE username=?");
            idStatement.setString(1, username);
            ResultSet result = idStatement.executeQuery();
            if(!result.next()){throw new EndpointFailedException("cannot find the id of the user just created", EndpointFailedException.Reason.UnexpectedError);}
            JSONObject data = new JSONObject();
            data.put("id", result.getInt("id"));
            return data.toJSONString();

        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new EndpointFailedException("Invalid String literal, this should never happen", EndpointFailedException.Reason.UnexpectedError);
        } catch (SQLException e) {
            throw new EndpointFailedException("sql unexpected error", EndpointFailedException.Reason.UnexpectedError);
        }
    }
}
