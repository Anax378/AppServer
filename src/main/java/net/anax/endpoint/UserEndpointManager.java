package net.anax.endpoint;

import net.anax.VirtualFileSystem.AuthorizationProfile;
import net.anax.cryptography.KeyManager;
import net.anax.database.Authorization;
import net.anax.database.DatabaseAccessManager;
import net.anax.logging.Logger;
import net.anax.magic.MagicStrings;
import net.anax.token.Claim;
import net.anax.token.Token;
import net.anax.token.TokenHeader;
import net.anax.util.ByteUtilities;
import net.anax.util.DatabaseUtilities;
import net.anax.util.JsonUtilities;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.Arrays;
import java.util.UUID;

public class UserEndpointManager {

    public UserEndpointManager(){
    }

    public String callEndpoint(String endpoint, JSONObject data, AuthorizationProfile auth, long traceId) throws EndpointFailedException {

        switch(endpoint){
            case ("getUsername") -> {
                if(!JsonUtilities.validateKeys(new String[]{"id"}, new Class[]{Long.class}, data)){throw new EndpointFailedException("insufficient data", EndpointFailedException.Reason.DataNotFound);}
                return getUsername((int)(long)data.get("id"), auth);
            }
            case("getUser") -> {
                if(!JsonUtilities.validateKeys(new String[]{"id"}, new Class[]{Long.class}, data)){throw new EndpointFailedException("insufficient data", EndpointFailedException.Reason.DataNotFound);}
                return getUser((int)(long)data.get("id"), auth);
            }
            case ("setUsername") -> {
                if(!JsonUtilities.validateKeys(new String[]{"id", "newUsername"}, new Class[]{Long.class, String.class}, data)){throw new EndpointFailedException("insufficient data", EndpointFailedException.Reason.DataNotFound);}
                return setUsername((int)(long)data.get("id"), (String)data.get("newUsername"), auth);
            }
            case("createUser") -> {
                if(!JsonUtilities.validateKeys(new String[]{"username", "password", "name"}, new Class[]{String.class, String.class, String.class}, data)){throw new EndpointFailedException("insufficient data", EndpointFailedException.Reason.DataNotFound);}
                return createUser((String)data.get("username"), (String)data.get("password"), (String)data.get("name"), auth);
            }
            case("getName") -> {
                if(!JsonUtilities.validateKeys(new String[]{"id"}, new Class[]{Long.class}, data)){throw new EndpointFailedException("insufficient data", EndpointFailedException.Reason.DataNotFound);}
                return getName((int)(long)data.get("id"), auth);
            }

            case("setName") -> {
                if(!JsonUtilities.validateKeys(new String[]{"id", "newName"}, new Class[]{Long.class, String.class}, data)){throw new EndpointFailedException("insufficient data", EndpointFailedException.Reason.DataNotFound);}
                return setName((int)(long)data.get("id"), (String)data.get("newName"), auth);
            }

            case("login") -> {
                if(!JsonUtilities.validateKeys(new String[]{"username", "password"}, new Class[]{String.class, String.class}, data)){throw new EndpointFailedException("insufficient data", EndpointFailedException.Reason.DataNotFound);}
                return login((String) data.get("username"), (String)data.get("password"), DatabaseAccessManager.getInstance().getKeyManager());
            }
            case("joinWithAccessCode") -> {
                if(!JsonUtilities.validateKeys(new String[]{"id", "accessCode"}, new Class[]{Long.class, String.class}, data)){throw new EndpointFailedException("insufficient data", EndpointFailedException.Reason.DataNotFound);}
                return joinWithAccessCode((int)(long)data.get("id"), (String)data.get("accessCode"), auth);
            }
            default -> {Logger.log("could not find a endpoint [" + endpoint + "] in user", traceId);}
        }
        return null;
    }

    public String login(String username, String passwordAttempt, KeyManager keyManager) throws EndpointFailedException {
        try {
            PreparedStatement statement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("SELECT password_hash, hash_salt, id FROM user WHERE username=?");
            statement.setString(1, username);
            ResultSet set = statement.executeQuery();

            if(!set.next()){throw new EndpointFailedException("user not found", EndpointFailedException.Reason.DataNotFound);}

            String passwordHash = set.getString("password_hash");
            String hashSalt = set.getString("hash_salt");
            int id = set.getInt("id");

            byte[] passwordHashBytes = ByteUtilities.fromHexString(passwordHash);
            byte[] hashSaltBytes = ByteUtilities.fromHexString(hashSalt);

            byte[] hashedPasswordAttempt = Authorization.generatePasswordHash(passwordAttempt, hashSaltBytes);

            for(int i = 0; i < hashedPasswordAttempt.length; i++){
                if(hashedPasswordAttempt[i] != passwordHashBytes[i]){
                    throw new EndpointFailedException("Access Denied, invalid password", EndpointFailedException.Reason.AccessDenied);
                }
            }

            Token authToken = new Token();

            UUID tokenId = UUID.randomUUID();

            authToken.addClaim(Claim.ExpirationTimestamp, String.valueOf(System.currentTimeMillis() + (1000 * 60 * 10))); //make the token expire after 10 minutes;
            authToken.addClaim(Claim.Subject, String.valueOf(id));
            authToken.addClaim(Claim.IssuedAt, String.valueOf(System.currentTimeMillis()));
            authToken.addClaim(Claim.Identifier, tokenId.toString());
            authToken.addHeader(TokenHeader.Type, MagicStrings.tokenType);
            authToken.addHeader(TokenHeader.Algorithm, "HMACSHA256");
            authToken.sign(keyManager.getHMACSHA256TokenKey());

            JSONObject data = new JSONObject();
            data.put("id", id);
            data.put("token", authToken.getTokenString());

            return data.toJSONString();

        } catch (SQLException e) {
            throw new EndpointFailedException("sql error", EndpointFailedException.Reason.UnexpectedError);
        } catch (UnsupportedEncodingException e) {
            throw new EndpointFailedException("invalid string literal", EndpointFailedException.Reason.UnexpectedError);
        } catch (NoSuchAlgorithmException e) {
            throw new EndpointFailedException("invalid string literal", EndpointFailedException.Reason.UnexpectedError);
        } catch (InvalidKeyException e) {
            throw new EndpointFailedException("invalid key", EndpointFailedException.Reason.UnexpectedError);
        }

    }
    public String getUsername(int id, AuthorizationProfile auth) throws EndpointFailedException {
        if (auth.isAdmin() || auth.getId() == id || Authorization.sharesGroupWith(auth, id)) {
            String username = DatabaseUtilities.queryString("username", "user", id);
            if(username == null){
                throw new EndpointFailedException("could not retrieve data", EndpointFailedException.Reason.DataNotFound);
            }
            JSONObject data = new JSONObject();
            data.put("username", username);
            return data.toJSONString();

        }
        throw new EndpointFailedException("Access Denied", EndpointFailedException.Reason.AccessDenied);
    }

    public String setName(int id, String newName, AuthorizationProfile auth) throws EndpointFailedException {
        if(!auth.isAdmin() && auth.getId() != id){throw new EndpointFailedException("Access Denied", EndpointFailedException.Reason.AccessDenied);}

        try {
            PreparedStatement statement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("UPDATE user SeT name=? WHERE id=?");
            statement.setString(1, newName);
            statement.setInt(2, id);
            if(statement.executeUpdate() == 0){throw new EndpointFailedException("nothing changed" ,EndpointFailedException.Reason.NothingChanged);}

            return "{\"success\":true}";

        } catch (SQLException e) {
            throw new EndpointFailedException("sql exception", EndpointFailedException.Reason.UnexpectedError);
        }
    }

    public String getName(int id, AuthorizationProfile auth) throws EndpointFailedException {
        if(!auth.isAdmin() && auth.getId() != id && !Authorization.sharesGroupWith(auth, id)){
            throw new EndpointFailedException("Access Denied", EndpointFailedException.Reason.AccessDenied);
        }

        String name = DatabaseUtilities.queryString("name", "user", id);
        if(name == null){throw new EndpointFailedException("Data not found", EndpointFailedException.Reason.DataNotFound);}

        JSONObject data = new JSONObject();
        data.put("name", name);
        return data.toJSONString();


    }

    public String getUser(int id, AuthorizationProfile auth) throws EndpointFailedException {
        if(auth.isAdmin() || auth.getId() == id){
            try {
                JSONObject data = new JSONObject();
                String username = DatabaseUtilities.queryString("username", "user", id);
                data.put("username", username);

                PreparedStatement statement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("SELECT task_id, is_done FROM user_task WHERE user_id=?");
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

                PreparedStatement group_statement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("SELECT group_id FROM user_group WHERE user_id=?");
                group_statement.setInt(1, id);
                ResultSet result = group_statement.executeQuery();

                JSONArray group_id_array = new JSONArray();

                while(result.next()){
                    group_id_array.add(result.getInt("group_id"));
                }

                data.put("name", DatabaseUtilities.queryString("name", "user", id));

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
                PreparedStatement statement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("UPDATE user SET username=? WHERE id=?");
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

    public String joinWithAccessCode(int id, String accessCode, AuthorizationProfile auth) throws EndpointFailedException {
        if(!auth.isAdmin() && auth.getId() != id){throw new EndpointFailedException("Access Denied", EndpointFailedException.Reason.AccessDenied);}
        try {
            int groupId = GroupEndpointManager.getIdFromAccessCode(accessCode);
            String groupAccessCodeRand = DatabaseUtilities.queryString("access_code_rand", "group_table", groupId);
            if(groupAccessCodeRand == null){throw new EndpointFailedException("could not query sql data", EndpointFailedException.Reason.AccessDenied);}

            byte[] groupAccessCodeRandBytes = ByteUtilities.fromHexString(groupAccessCodeRand);

            String groupAccessCode = GroupEndpointManager.getAccessCode(groupId, groupAccessCodeRandBytes);

            if(!Arrays.equals(groupAccessCode.getBytes(), accessCode.getBytes())){throw new EndpointFailedException("Access Denied", EndpointFailedException.Reason.AccessDenied);}

            PreparedStatement statement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("INSERT INTO user_group (user_id, group_id) VALUES (?, ?)");
            statement.setInt(1, id);
            statement.setInt(2, groupId);
            int affected = statement.executeUpdate();
            if(affected == 0){throw new EndpointFailedException("nothing changed", EndpointFailedException.Reason.NothingChanged);}
            return "{\"success\":true}";

        } catch (SQLException e) {
            throw new EndpointFailedException("sql error", EndpointFailedException.Reason.UnexpectedError, e);
        }


    }

    public String createUser(String username, String password, String name, AuthorizationProfile auth) throws EndpointFailedException {
        try {

            byte[] salt = Authorization.generateSalt();
            byte[] passwordHash = Authorization.generatePasswordHash(password, salt);

            PreparedStatement statement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("INSERT INTO user (username, password_hash, hash_salt, name) VALUES (?, ?, ?, ?);", Statement.RETURN_GENERATED_KEYS);

            statement.setString(1, username);
            statement.setString(2, ByteUtilities.toHexString(passwordHash));
            statement.setString(3, ByteUtilities.toHexString(salt));
            statement.setString(4, name);

            int affected = statement.executeUpdate();
            ResultSet result = statement.getGeneratedKeys();

            if(affected == 0){
                throw new EndpointFailedException("nothing changed", EndpointFailedException.Reason.UnexpectedError);
            }


            if(!result.next()){throw new EndpointFailedException("cannot find the id of the user just created", EndpointFailedException.Reason.UnexpectedError);}

            JSONObject data = new JSONObject();
            data.put("id", result.getInt(1));
            return data.toJSONString();

        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new EndpointFailedException("Invalid String literal, this should never happen", EndpointFailedException.Reason.UnexpectedError);
        } catch (SQLException e) {
            ;e.printStackTrace();
            throw new EndpointFailedException("sql unexpected error", EndpointFailedException.Reason.UnexpectedError);

        }
    }



}
