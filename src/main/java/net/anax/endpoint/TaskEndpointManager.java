package net.anax.endpoint;

import net.anax.VirtualFileSystem.AuthorizationProfile;
import net.anax.database.Authorization;
import net.anax.database.DatabaseAccessManager;
import net.anax.logging.Logger;
import net.anax.util.JsonUtilities;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.sql.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class TaskEndpointManager {
    public TaskEndpointManager() {
    }

    public String callEndpoint(String endpoint, JSONObject data, AuthorizationProfile auth, long traceId) throws EndpointFailedException {
        switch (endpoint){
            case("getTask") -> {
                if(!JsonUtilities.validateKeys(new String[]{"taskId"}, new Class<?>[]{Long.class}, data)){throw new EndpointFailedException("necessary data not provided", EndpointFailedException.Reason.DataNotFound);}
                return getTask((int)(long)data.get("taskId"), auth);
            }

            case("createTask") -> {
                if(!JsonUtilities.validateKeys(new String[]{"dueTimestamp", "description", "type", "authorUserId", "userIds"}, new Class<?>[]{Long.class, String.class, Long.class, Long.class, JSONArray.class}, data)){throw new EndpointFailedException("necessary data not provided", EndpointFailedException.Reason.DataNotFound);}
                JSONArray array = (JSONArray) data.get("userIds");
                if (!array.isEmpty() && !Long.class.isInstance(array.get(0))){throw new EndpointFailedException("necessary data not provided", EndpointFailedException.Reason.DataNotFound);}
                int[] userIds = new int[array.size()];
                for(int i = 0; i < userIds.length; i++){userIds[i] = (int)(long) array.get(i);}

                return createTask((long) data.get("dueTimestamp"), (String) data.get("description"), (int)(long) data.get("type"), (int)(long) data.get("authorUserId"), userIds, auth);

            }
            case("setDueTimestamp") -> {
                if(!JsonUtilities.validateKeys(new String[]{"taskId", "timestamp"}, new Class<?>[]{Long.class, Long.class}, data)){throw new EndpointFailedException("necessary data not provided", EndpointFailedException.Reason.DataNotFound);}
                return setDueTimestamp((int)(long) data.get("taskId"), (int)(long) data.get("timestamp"), auth);
            }
            case("setDescription") -> {
                if(!JsonUtilities.validateKeys(new String[]{"taskId", "newDescription"}, new Class<?>[]{Long.class, String.class}, data)){throw new EndpointFailedException("necessary data not provided", EndpointFailedException.Reason.DataNotFound);}
                return setDescription((int)(long) data.get("taskId"), (String) data.get("newDescription"), auth);
            }
            case("setType") -> {
                if(!JsonUtilities.validateKeys(new String[]{"taskId", "newType"}, new Class<?>[]{Long.class, Long.class}, data)){throw new EndpointFailedException("necessary data not provided", EndpointFailedException.Reason.DataNotFound);}
                return setType((int)(long)data.get("taskId"), (int)(long)data.get("newType"), auth);
            }
            case("setGroupId") -> {
                if(!JsonUtilities.validateKeys(new String[]{"taskId", "newGroupId"}, new Class<?>[]{Long.class, Long.class}, data)){throw new EndpointFailedException("necessary data not provided", EndpointFailedException.Reason.DataNotFound);}
                return setGroupId((int)(long)data.get("taskId"), (int)(long)data.get("newGroupId"), auth);
            }
            case("removeGroupId") -> {
                if(!JsonUtilities.validateKeys(new String[]{"taskId"}, new Class<?>[]{Long.class}, data)){throw new EndpointFailedException("necessary data not provided", EndpointFailedException.Reason.DataNotFound);}
                return removeGroupId((int)(long)data.get("taskId"), auth);
            }
            case("removeAmount") -> {
                if(!JsonUtilities.validateKeys(new String[]{"taskId"}, new Class<?>[]{Long.class}, data)){throw new EndpointFailedException("necessary data not provided", EndpointFailedException.Reason.DataNotFound);}
                return removeAmount((int)(long)data.get("taskId"), auth);
            }
            case("setAmount") -> {
                if(!JsonUtilities.validateKeys(new String[]{"taskId", "amount"}, new Class<?>[]{Long.class, Long.class}, data)){throw new EndpointFailedException("necessary data not provided", EndpointFailedException.Reason.DataNotFound);}
                return setAmount((int)(long)data.get("taskId"), (int)(long)data.get("amount"), auth);
            }
            case("setHasUserTask") -> {
                if(!JsonUtilities.validateKeys(new String[]{"taskId", "userId", "hasTask"}, new Class<?>[]{Long.class, Long.class, Boolean.class}, data)){throw new EndpointFailedException("necessary data not provided", EndpointFailedException.Reason.DataNotFound);}
                return setHasUserTask((int)(long)data.get("taskId"), (int)(long)data.get("userId"), (boolean)data.get("hasTask"), auth);
            }
            case("setCompleteness") -> {
                if(!JsonUtilities.validateKeys(new String[]{"taskId", "userId", "isComplete"}, new Class<?>[]{Long.class, Long.class, Boolean.class}, data)){throw new EndpointFailedException("necessary data not provided", EndpointFailedException.Reason.DataNotFound);}
                return setCompleteness((int)(long)data.get("userId"), (int)(long)data.get("taskId"), (boolean)data.get("isComplete"), auth);
            }
            default -> Logger.log("could not find ednpoint [" + endpoint + "] in task", traceId);
        }
        return null;
    }

    public String getTask(int taskId, AuthorizationProfile auth) throws EndpointFailedException {
        if (!auth.isAdmin() && !Authorization.isParticipantIn(auth, taskId)) {
            throw new EndpointFailedException("Access Denied", EndpointFailedException.Reason.AccessDenied);
        }
        try {
            JSONObject data = new JSONObject();

            PreparedStatement dueStatement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("SELECT due_timestamp FROM task WHERE id=?");
            dueStatement.setInt(1, taskId);
            ResultSet dueSet = dueStatement.executeQuery();
            if (!dueSet.next()) {
                throw new EndpointFailedException("data not found", EndpointFailedException.Reason.DataNotFound);
            }
            data.put("dueTimestamp", dueSet.getTimestamp("due_timestamp").getTime());

            PreparedStatement descriptionStatement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("SELECT description FROM task WHERE id=?");
            descriptionStatement.setInt(1, taskId);
            ResultSet descriptionSet = descriptionStatement.executeQuery();
            if (!descriptionSet.next()) {
                throw new EndpointFailedException("data not found", EndpointFailedException.Reason.DataNotFound);
            }
            data.put("description", descriptionSet.getString("description"));

            PreparedStatement typeStatement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("SELECT type FROM task WHERE id=?");
            typeStatement.setInt(1, taskId);
            ResultSet typeSet = typeStatement.executeQuery();
            if (!typeSet.next()) {
                throw new EndpointFailedException("data not found", EndpointFailedException.Reason.DataNotFound);
            }
            data.put("type", typeSet.getInt("type"));

            PreparedStatement groupIdStatement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("SELECT group_id FROM task WHERE id=?");
            groupIdStatement.setInt(1, taskId);
            ResultSet groupIdSet = groupIdStatement.executeQuery();
            if (!groupIdSet.next()) {
                throw new EndpointFailedException("data not found", EndpointFailedException.Reason.DataNotFound);
            }
            data.put("groupId", groupIdSet.getInt("group_id"));

            PreparedStatement amountStatement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("SELECT amount FROM payment_task WHERE parent_id=?");
            amountStatement.setInt(1, taskId);
            ResultSet amountSet = amountStatement.executeQuery();
            if (amountSet.next()) {
                data.put("amount", amountSet.getInt("amount"));
            }

            PreparedStatement userStatement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("SELECT user_id from user_task WHERE task_id=?");
            userStatement.setInt(1, taskId);
            ResultSet userSet = userStatement.executeQuery();
            JSONArray users = new JSONArray();
            while (userSet.next()) {
                users.add(userSet.getInt("user_id"));
            }
            data.put("userIds", users);

            return data.toJSONString();

        } catch (SQLException e) {
            ;e.printStackTrace();
            throw new EndpointFailedException("sql error", EndpointFailedException.Reason.UnexpectedError);
        }
    }

    public String createTask(long dueTimeStamp, String description, int type, int authorUserId, int[] userIds, AuthorizationProfile auth) throws EndpointFailedException {
        try {
            PreparedStatement statement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("INSERT INTO task (type, due_timestamp, description) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
            statement.setInt(1, type);
            statement.setTimestamp(2, new Timestamp(dueTimeStamp));
            statement.setString(3, description);

            HashSet<Integer> idSet = new HashSet<>();
            for(int id : userIds){idSet.add(id);}
            idSet.add(authorUserId);

            userIds = idSet.stream().mapToInt(Number::intValue).toArray();

            int affected = statement.executeUpdate();
            if (affected == 0) {
                throw new EndpointFailedException("nothing changed", EndpointFailedException.Reason.NothingChanged);
            }
            ResultSet generatedKeys = statement.getGeneratedKeys();
            generatedKeys.next();
            int id = generatedKeys.getInt(1);

            if (userIds.length != 0) {
                StringBuilder query = new StringBuilder().append("INSERT INTO user_task (user_id, task_id) VALUES ");

                for (int user : userIds) {
                    query.append("(").append(user).append(", ").append(id).append("),");
                }
                query.deleteCharAt(query.length() - 1);

                PreparedStatement userAddStatement = DatabaseAccessManager.getInstance().getConnection().prepareStatement(query.toString());
                userAddStatement.executeUpdate();
            }

            JSONObject data = new JSONObject();
            data.put("id", id);
            return data.toJSONString();

        } catch (SQLException e) {
            throw new EndpointFailedException("sql error", EndpointFailedException.Reason.UnexpectedError);
        }
    }

    public String setDueTimestamp(int taskId, long timestamp, AuthorizationProfile auth) throws EndpointFailedException {
        if (!auth.isAdmin() && !Authorization.isParticipantIn(auth, taskId)) {
            throw new EndpointFailedException("Access Denied", EndpointFailedException.Reason.AccessDenied);
        }

        try {
            PreparedStatement statement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("UPDATE task SET due_timestamp=? WHERE id=?");
            statement.setTimestamp(1, new Timestamp(timestamp));
            statement.setInt(2, taskId);

            int affected = statement.executeUpdate();
            if (affected == 0) {
                throw new EndpointFailedException("nothing changed", EndpointFailedException.Reason.NothingChanged);
            }
            return "{\"success\":true}";

        } catch (SQLException e) {
            throw new EndpointFailedException("sql error", EndpointFailedException.Reason.UnexpectedError);
        }
    }

    public String setDescription(int taskId, String description, AuthorizationProfile auth) throws EndpointFailedException {
        authorizeParticipant(taskId, auth);
        try {
            PreparedStatement statement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("UPDATE task SET description=? WHERE id=?");
            statement.setString(1, description);
            statement.setInt(2, taskId);

            int affected = statement.executeUpdate();
            if (affected == 0) {
                throw new EndpointFailedException("nothing changed", EndpointFailedException.Reason.NothingChanged);
            }

            return "{\"success\":true}";

        } catch (SQLException e) {
            throw new EndpointFailedException("sql error", EndpointFailedException.Reason.UnexpectedError);
        }
    }

    public String setType(int taskId, int newType, AuthorizationProfile auth) throws EndpointFailedException {
        authorizeParticipant(taskId, auth);

        try {
            PreparedStatement statement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("UPDATE task SET type=? WHERE id=?");
            statement.setInt(1, newType);
            statement.setInt(2, taskId);

            int affected = statement.executeUpdate();
            if (affected == 0) {
                throw new EndpointFailedException("nothing changed", EndpointFailedException.Reason.NothingChanged);
            }

            return "{\"success\":true}";

        } catch (SQLException e) {
            throw new EndpointFailedException("sql error", EndpointFailedException.Reason.UnexpectedError);
        }

    }

    public String setGroupId(int taskId, int newGroupId, AuthorizationProfile auth) throws EndpointFailedException {
        authorizeParticipant(taskId, auth);

        try {
            PreparedStatement statement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("UPDATE task SET group_id=? WHERE id=?");
            statement.setInt(1, newGroupId);
            statement.setInt(2, taskId);

            if (statement.executeUpdate() == 0) {
                throw new EndpointFailedException("nothing changed", EndpointFailedException.Reason.NothingChanged);
            }

            return "{\"success\":true}";

        } catch (SQLException e) {
            throw new EndpointFailedException("sql error", EndpointFailedException.Reason.UnexpectedError);
        }

    }

    public String removeGroupId(int taskId, AuthorizationProfile auth) throws EndpointFailedException {
        authorizeParticipant(taskId, auth);
        try {
            PreparedStatement statement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("UPDATE task SET group_id=? WHERE id=?");
            statement.setNull(1, Types.INTEGER);
            statement.setInt(2, taskId);

            if(statement.executeUpdate() == 0){throw new EndpointFailedException("nothing changed", EndpointFailedException.Reason.NothingChanged);}
            return "{\"success\":true}";

        } catch (SQLException e) {
            throw new EndpointFailedException("sql error", EndpointFailedException.Reason.UnexpectedError);
        }
    }

    public String removeAmount(int taskId, AuthorizationProfile auth) throws EndpointFailedException {
        authorizeParticipant(taskId, auth);

        try {
            PreparedStatement statement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("DELETE FROM payment_task WHERE parent_id=?");
            statement.setInt(1, taskId);

            if(statement.executeUpdate() == 0){throw new EndpointFailedException("nothing changed", EndpointFailedException.Reason.NothingChanged);}
            return "{\"success\":true}";

        } catch (SQLException e) {
            throw new EndpointFailedException("sql error", EndpointFailedException.Reason.UnexpectedError);
        }
    }

    public String setAmount(int taskId, int amount, AuthorizationProfile auth) throws EndpointFailedException {
        authorizeParticipant(taskId, auth);
        try {
            PreparedStatement checkStatement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("SELECT parent_id FROM payment_task WHERE parent_id=?");
            checkStatement.setInt(1, taskId);
            ResultSet checkSet = checkStatement.executeQuery();
            if(checkSet.next()){
                PreparedStatement statement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("UPDATE payment_task SET amount=? WHERE parent_id=?");
                statement.setInt(1, amount);
                statement.setInt(2, taskId);

                if(statement.executeUpdate() == 0){throw new EndpointFailedException("nothing changed", EndpointFailedException.Reason.NothingChanged);}

                return "{\"success\":true}";

            }else{
                PreparedStatement statement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("INSERT INTO payment_task (parent_id, amount) VALUES(?, ?)");
                statement.setInt(1, taskId);
                statement.setInt(2, amount);
                if(statement.executeUpdate() == 0){throw new EndpointFailedException("nothing changed", EndpointFailedException.Reason.NothingChanged);}
                return "{\"success\":true}";
            }

        } catch (SQLException e) {
            throw new EndpointFailedException("sql error", EndpointFailedException.Reason.UnexpectedError);
        }

    }

    public String setHasUserTask(int taskId, int userId, boolean hasTask, AuthorizationProfile auth) throws EndpointFailedException {
        authorizeParticipant(taskId, auth);
        try{
            if(hasTask){
                PreparedStatement statement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("INSERT INTO user_task (user_id, task_id) VALUES (?, ?)");
                statement.setInt(1, userId);
                statement.setInt(2, taskId);
                if(statement.executeUpdate() == 0){throw new EndpointFailedException("nothing changed", EndpointFailedException.Reason.NothingChanged);}

                return "{\"success\":true}";

            }else{
                PreparedStatement statement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("DELETE FROM user_task WHERE user_id=? AND task_id=?");
                statement.setInt(1, userId);
                statement.setInt(2, taskId);

                if(statement.executeUpdate() == 0){
                    throw new EndpointFailedException("nothing changed", EndpointFailedException.Reason.NothingChanged);
                }

                return "{\"success\":true}";

            }
        }catch(SQLException e){
            throw new EndpointFailedException("sql error", EndpointFailedException.Reason.UnexpectedError);
        }
    }

    public String setCompleteness(int userId, int taskId, boolean isComplete, AuthorizationProfile auth) throws EndpointFailedException {
        if(auth.getId() != userId && !auth.isAdmin()){throw new EndpointFailedException("Access Denied", EndpointFailedException.Reason.AccessDenied);}

        try {
            PreparedStatement statement = DatabaseAccessManager.getInstance().getConnection().prepareStatement("UPDATE user_task SET is_done=? WHERE (user_id=? AND task_id=?)");
            statement.setBoolean(1, isComplete);
            statement.setInt(2, userId);
            statement.setInt(3, taskId);

            if(statement.executeUpdate() == 0){throw new EndpointFailedException("nothing changed", EndpointFailedException.Reason.NothingChanged);}
            return "{\"success\":true}";

        } catch (SQLException e) {
            throw new EndpointFailedException("sql error", EndpointFailedException.Reason.UnexpectedError);
        }
    }
    public void authorizeParticipant(int taskId, AuthorizationProfile auth) throws EndpointFailedException {
        if (!auth.isAdmin() && !Authorization.isParticipantIn(auth, taskId)) {
            throw new EndpointFailedException("Access Denied", EndpointFailedException.Reason.AccessDenied);
        }

    }
}
