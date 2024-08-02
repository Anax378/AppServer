package net.anax.endpoint;

import net.anax.VirtualFileSystem.AuthorizationProfile;
import net.anax.database.Authorization;
import net.anax.database.DatabaseAccessManager;
import net.anax.logging.Logger;
import net.anax.util.ByteUtilities;
import net.anax.util.DatabaseUtilities;
import net.anax.util.JsonUtilities;
import net.anax.util.StringUtilities;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.sql.*;
import java.util.Random;

public class GroupEndpointManager {
    public GroupEndpointManager(){
    }

    public String callEndpoint(String endpoint, JSONObject data, AuthorizationProfile auth, long traceId) throws EndpointFailedException {
        switch(endpoint){
            case("getGroup") -> {
                if(!JsonUtilities.validateKeys(new String[]{"groupId"}, new Class<?>[]{Long.class}, data)){throw new EndpointFailedException("necessary data not found", EndpointFailedException.Reason.DataNotFound);}
                return getGroup((int)(long)data.get("groupId"), auth);
            }
            case("setName") ->{
                if(!JsonUtilities.validateKeys(new String[]{"groupId", "newName"}, new Class<?>[]{Long.class, String.class}, data)){throw new EndpointFailedException("necessary data not found", EndpointFailedException.Reason.DataNotFound);}
                return setName((int)(long)data.get("groupId"), (String) data.get("newName"), auth);
            }
            case("setTreasurerUserId") -> {
                if(!JsonUtilities.validateKeys(new String[]{"groupId", "newTreasurerUserId"}, new Class<?>[]{Long.class, Long.class}, data)){throw new EndpointFailedException("necessary data not found", EndpointFailedException.Reason.DataNotFound);}
                return setTreasurerUserId((int)(long) data.get("groupId"), (int)(long) data.get("newTreasurerUserId"), auth);
            }
            case("setAdminUserId") -> {
                if(!JsonUtilities.validateKeys(new String[]{"groupId", "newAdminId"}, new Class<?>[]{Long.class, Long.class}, data)){throw new EndpointFailedException("necessary data not found", EndpointFailedException.Reason.DataNotFound);}
                return setAdminUserId((int)(long) data.get("groupId"), (int)(long) data.get("newAdminId"), auth);
            }
            case("setIsInGroup") ->{
                if(!JsonUtilities.validateKeys(new String[]{"groupId","userId", "isInGroup"}, new Class<?>[]{Long.class, Long.class, Boolean.class}, data)){throw new EndpointFailedException("necessary data not found", EndpointFailedException.Reason.DataNotFound);}
                return setIsInGroup((int)(long) data.get("groupId"), (int)(long) data.get("userId"), (boolean) data.get("isInGroup"), auth);
            }
            case("createGroup") -> {
                if(!JsonUtilities.validateKeys(new String[]{"name", "authorUserId"}, new Class<?>[]{String.class, Long.class}, data)){throw new EndpointFailedException("necessary data not found", EndpointFailedException.Reason.DataNotFound);}
                return createGroup((String) data.get("name"), (int)(long) data.get("authorUserId"), auth);
            }
            case("removeTreasurer") -> {
                if(!JsonUtilities.validateKeys(new String[]{"groupId"}, new Class<?>[]{Long.class}, data)){throw new EndpointFailedException("necessary data not found", EndpointFailedException.Reason.DataNotFound);}
                return removeTreasurer((int)(long) data.get("groupId"), auth);
            }
            case("rerollAccessCode") -> {
                if(!JsonUtilities.validateKeys(new String[]{"groupId"}, new Class<?>[]{String.class}, data)){throw new EndpointFailedException("necessary data not found", EndpointFailedException.Reason.DataNotFound);}
                return rerollAccessCode((int)(long) data.get("groupId"), auth);
            }
            default -> {Logger.log("could not find endpoint [" + endpoint + "] in grouop", traceId);}
        }
        return null;
    }


    public String getGroup(int groupId, AuthorizationProfile auth) throws EndpointFailedException {
        if(!Authorization.isMemberOfGroup(auth, groupId) && !auth.isAdmin()){throw new EndpointFailedException("Access Denied", EndpointFailedException.Reason.AccessDenied);}

        String name = DatabaseUtilities.queryString("name", "group_table", groupId);
        String treasurerUserId = DatabaseUtilities.queryString("treasurer_user_id", "group_table", groupId);
        String adminUserId = DatabaseUtilities.queryString("admin_id", "group_table", groupId);
        if(name == null || adminUserId == null){throw new EndpointFailedException("data not found", EndpointFailedException.Reason.DataNotFound);}

        try {
            PreparedStatement statement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("SELECT user_id FROM user_group WHERE group_id=?");
            statement.setInt(1, groupId);
            ResultSet result = statement.executeQuery();
            JSONArray userIds = new JSONArray();
            while(result.next()){
                userIds.add(result.getInt("user_id"));
            }

            PreparedStatement taskStatement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("SELECT id FROM task WHERE group_id=?");
            taskStatement.setInt(1, groupId);
            ResultSet taskResult = taskStatement.executeQuery();
            JSONArray taskIds = new JSONArray();
            while(taskResult.next()){
                taskIds.add(taskResult.getInt("id"));
            }

            String accessCodeRand = DatabaseUtilities.queryString("access_code_rand", "group_table", groupId);

            if(accessCodeRand == null){
                throw new EndpointFailedException("sql error", EndpointFailedException.Reason.UnexpectedError);
            }

            byte[] accessCodeRandBytes = ByteUtilities.fillToLen(ByteUtilities.fromHexString(accessCodeRand), 5, (byte) 0);
            String accessCode = getAccessCode(groupId, accessCodeRandBytes);

            JSONObject data = new JSONObject();
            data.put("name", name);
            data.put("treasurerUserId", treasurerUserId);
            data.put("userIds", userIds);
            data.put("taskIds", taskIds);
            data.put("admin_id", adminUserId);
            data.put("accessCode", accessCode);

            return data.toJSONString();

        } catch (SQLException e) {
            throw new EndpointFailedException("sql error", EndpointFailedException.Reason.UnexpectedError);
        }

    }

    public String setName(int groupId, String newName, AuthorizationProfile auth) throws EndpointFailedException {
        if(!auth.isAdmin() && !Authorization.isAdminInGroup(auth, groupId)){throw new EndpointFailedException("Access Denied", EndpointFailedException.Reason.AccessDenied);}

        try {

            PreparedStatement statement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("UPDATE group_table SET name=? WHERE id=?");
            statement.setString(1, newName);
            statement.setInt(2, groupId);
            int affected = statement.executeUpdate();
            if(affected == 0){throw new EndpointFailedException("nothing changed when chaning group name", EndpointFailedException.Reason.NothingChanged);}
            return "{\"success\":true}";

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String setTreasurerUserId(int groupId, int newTreasurerUserId, AuthorizationProfile auth) throws EndpointFailedException {
        if(!auth.isAdmin() && !Authorization.isAdminInGroup(auth, groupId)){throw new EndpointFailedException("Access Denied", EndpointFailedException.Reason.AccessDenied);};
        try {
            PreparedStatement statement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("UPDATE group_table SET treasurer_user_id=? WHERE id=?");
            statement.setInt(1, newTreasurerUserId);
            statement.setInt(2, groupId);
            int affected = statement.executeUpdate();
            if(affected == 0){throw new EndpointFailedException("nothing changed", EndpointFailedException.Reason.NothingChanged);}
            return "{\"success\":true}";
        } catch (SQLException e) {
            throw new EndpointFailedException("sql error", EndpointFailedException.Reason.UnexpectedError);
        }
    }

    public String setAdminUserId(int groupId, int newAdminUserID, AuthorizationProfile auth) throws EndpointFailedException {
        if(!auth.isAdmin() && !Authorization.isAdminInGroup(auth, groupId)){throw new EndpointFailedException("Access Denied", EndpointFailedException.Reason.AccessDenied);}
        try {
            PreparedStatement statement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("UPDATE group_table SET admin_id=? WHERE id=?");
            statement.setInt(1, newAdminUserID);
            statement.setInt(2, groupId);
            int affected = statement.executeUpdate();
            if(affected == 0){throw new EndpointFailedException("nothing changed", EndpointFailedException.Reason.NothingChanged);}
            return "{\"success\":true}";

        } catch (SQLException e) {
            throw new EndpointFailedException("sql error", EndpointFailedException.Reason.UnexpectedError);
        }
    }

    public String setIsInGroup(int groupId, int userId, boolean isInGroup, AuthorizationProfile auth) throws EndpointFailedException {
        if(!auth.isAdmin() && !(isInGroup ? Authorization.isMemberOfGroup(auth, groupId) : (Authorization.isAdminInGroup(auth, groupId) || auth.getId() == userId))){throw new EndpointFailedException("Access Denied", EndpointFailedException.Reason.AccessDenied);}
        try{
            if(isInGroup){
                PreparedStatement statement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("INSERT INTO user_group (user_id, group_id) VALUES (?, ?);");
                statement.setInt(1, userId);
                statement.setInt(2, groupId);
                int affected = statement.executeUpdate();
                if(affected == 0){throw new EndpointFailedException("nothing changed", EndpointFailedException.Reason.NothingChanged);}
                return "{\"success\":true}";
            }else{
                PreparedStatement statement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("DELETE FROM user_group WHERE (user_id=? AND group_id=?)");
                statement.setInt(1, userId);
                statement.setInt(2, groupId);
                int affected = statement.executeUpdate();
                if(affected == 0){throw new EndpointFailedException("nothing changed", EndpointFailedException.Reason.NothingChanged);}
                return "{\"success\":true}";
            }
        }catch(SQLException e){
            throw new EndpointFailedException("sql error", EndpointFailedException.Reason.UnexpectedError);
        }
    }

    public String rerollAccessCode(int groupId, AuthorizationProfile auth) throws EndpointFailedException {
        if (!auth.isAdmin() && !Authorization.isAdminInGroup(auth, groupId)){throw new EndpointFailedException("Access Denied", EndpointFailedException.Reason.AccessDenied);}
        try{
            Random random = new Random();
            byte[] accessCodeRand = new byte[5];
            random.nextBytes(accessCodeRand);

            String accessCodeRandHex = ByteUtilities.toHexString(accessCodeRand);
            PreparedStatement statement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("UPDATE group_table SET access_code_rand=? WHERE id=?;");
            statement.setString(1, accessCodeRandHex);
            statement.setInt(2, groupId);
            int affected = statement.executeUpdate();

            if(affected == 0){
                throw new EndpointFailedException("nothing changed", EndpointFailedException.Reason.NothingChanged);
            }
            String accessCode = getAccessCode(groupId, accessCodeRand);
            return "{\"newAccessCode\":\"" + accessCode + "\"}";

        } catch (SQLException e) {
            throw new EndpointFailedException("sql error", EndpointFailedException.Reason.UnexpectedError, e);
        }

    }
    public String createGroup(String name, int authorUserId, AuthorizationProfile auth) throws EndpointFailedException {
        if(!auth.isAdmin() && auth.getId() != authorUserId){throw new EndpointFailedException("Access Denied",EndpointFailedException.Reason.AccessDenied);}
        try {
            Random random = new Random();
            byte[] accessCodeRandBytes = new byte[5];
            random.nextBytes(accessCodeRandBytes);
            String accessCodeRand = ByteUtilities.toHexString(accessCodeRandBytes);

            PreparedStatement statement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("INSERT INTO group_table (name, admin_id, access_code_rand) VALUES (?, ?, ?);", Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, name);
            statement.setInt(2, authorUserId);
            statement.setString(3, accessCodeRand);
            statement.executeUpdate();
            ResultSet result = statement.getGeneratedKeys();
            if(!result.next()){throw new EndpointFailedException("no keys generated, unable to create group", EndpointFailedException.Reason.UnexpectedError);}
            int id = result.getInt(1);

            PreparedStatement add_user_statement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("INSERT INTO user_group (user_id, group_id) VALUES (?, ?);");
            add_user_statement.setInt(1, authorUserId);
            add_user_statement.setInt(2, id);
            int affected = add_user_statement.executeUpdate();
            if(affected == 0){throw new EndpointFailedException("no user added to newly generated group", EndpointFailedException.Reason.UnexpectedError);}

            JSONObject data = new JSONObject();
            data.put("id", id);
            return data.toJSONString();

        } catch (SQLException e) {
            throw new EndpointFailedException("sql error", EndpointFailedException.Reason.UnexpectedError, e);
        }
    }

    public String removeTreasurer(int groupId, AuthorizationProfile auth) throws EndpointFailedException {
        if(!auth.isAdmin() && !Authorization.isAdminInGroup(auth, groupId)){
            throw new EndpointFailedException("Access Denied", EndpointFailedException.Reason.AccessDenied);
        }

        try {
            PreparedStatement statement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("UPDATE group_table SET treasurer_user_id=NULL WHERE id=?");
            statement.setInt(1, groupId);
            int affected = statement.executeUpdate();
            if(affected == 0){throw new EndpointFailedException("nothing changed", EndpointFailedException.Reason.NothingChanged);}
            return "{\"success\":true}";
        } catch (SQLException e) {
            throw new EndpointFailedException("sql error", EndpointFailedException.Reason.UnexpectedError);
        }
    }

    public static int getIdFromAccessCode(String accessCode){
        byte[] accessCodeBytes = StringUtilities.fromAccessCodeFormat(accessCode);
        byte[] idBytes = new byte[4];
        System.arraycopy(accessCodeBytes, 5, idBytes, 0, 4);
        return ByteUtilities.bytesToInt(idBytes);

    }

    public static String getAccessCode(int id, byte[] accessCodeRand){
        byte[] idBytes = ByteUtilities.intToBytes(id);
        byte[] accessCodeBytes = ByteUtilities.concatenateByteArrays(accessCodeRand, idBytes);
        return StringUtilities.toAccessCodeFormat(accessCodeBytes);

    }

}
