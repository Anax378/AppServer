package net.anax.endpoint;

import net.anax.VirtualFileSystem.AuthorizationProfile;
import net.anax.database.Authorization;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.beans.PropertyEditorSupport;
import java.sql.*;

public class TaskEndpointManager {
    Connection connection;

    public TaskEndpointManager(Connection connection) {
        this.connection = connection;
    }

    public String getTask(int taskId, AuthorizationProfile auth) throws EndpointFailedException {
        if (!auth.isAdmin() && !Authorization.isParticipantIn(auth, taskId, connection)) {
            throw new EndpointFailedException("Access Denied", EndpointFailedException.Reason.AccessDenied);
        }
        try {
            JSONObject data = new JSONObject();

            PreparedStatement dueStatement = connection.prepareStatement("SELECT due_timestamp FROM task WHERE id=?");
            dueStatement.setInt(1, taskId);
            ResultSet dueSet = dueStatement.executeQuery();
            if (!dueSet.next()) {
                throw new EndpointFailedException("data not found", EndpointFailedException.Reason.DataNotFound);
            }
            data.put("dueTimestamp", dueSet.getLong("due_timestamp"));

            PreparedStatement descriptionStatement = connection.prepareStatement("SELECT description FROM task WHERE id=?");
            descriptionStatement.setInt(1, taskId);
            ResultSet descriptionSet = descriptionStatement.executeQuery();
            if (!descriptionSet.next()) {
                throw new EndpointFailedException("data not found", EndpointFailedException.Reason.DataNotFound);
            }
            data.put("description", descriptionSet.getString("description"));

            PreparedStatement typeStatement = connection.prepareStatement("SELECT type FROM task WHERE id=?");
            typeStatement.setInt(1, taskId);
            ResultSet typeSet = typeStatement.executeQuery();
            if (!typeSet.next()) {
                throw new EndpointFailedException("data not found", EndpointFailedException.Reason.DataNotFound);
            }
            data.put("type", typeSet.getInt("type"));

            PreparedStatement groupIdStatement = connection.prepareStatement("SELECT group_id FROM task WHERE id=?");
            groupIdStatement.setInt(1, taskId);
            ResultSet groupIdSet = groupIdStatement.executeQuery();
            if (!groupIdSet.next()) {
                throw new EndpointFailedException("data not found", EndpointFailedException.Reason.DataNotFound);
            }
            data.put("groupId", groupIdSet.getInt("group_id"));

            PreparedStatement amountStatement = connection.prepareStatement("SELECT amount FROM payment_task WHERE parent_id=?");
            amountStatement.setInt(1, taskId);
            ResultSet amountSet = amountStatement.executeQuery();
            if (amountSet.next()) {
                data.put("amount", amountSet.getInt("amount"));
            }

            PreparedStatement userStatement = connection.prepareStatement("SELECT user_id from user_task WHERE task_id=?");
            userStatement.setInt(1, taskId);
            ResultSet userSet = userStatement.executeQuery();
            JSONArray users = new JSONArray();
            while (userSet.next()) {
                users.add(userSet.getInt("user_id"));
            }
            data.put("userIds", users);

            return data.toJSONString();

        } catch (SQLException e) {
            throw new EndpointFailedException("sql error", EndpointFailedException.Reason.UnexpectedError);
        }
    }

    public String createTask(long dueTimeStamp, String description, int type, Integer groupId, int authorUserId, int[] userIds, AuthorizationProfile auth) throws EndpointFailedException {

        try {
            PreparedStatement statement = connection.prepareStatement("INSERT INTO task (type, due_timestamp, description, groupId) VALUES (?, ?, ?, ?)");
            statement.setInt(1, type);
            statement.setLong(2, dueTimeStamp);
            statement.setString(3, description);

            if (groupId == null) {
                statement.setNull(4, Types.INTEGER);
            } else {
                statement.setInt(4, groupId);
            }

            int affected = statement.executeUpdate();
            if (affected == 0) {
                throw new EndpointFailedException("nothing changed", EndpointFailedException.Reason.NothingChanged);
            }
            int id = statement.getGeneratedKeys().getInt("id");

            if (userIds.length != 0) {
                StringBuilder query = new StringBuilder().append("INSERT INTO user_task (user_id, task_id) VALUES ");

                for (int user : userIds) {
                    query.append("(").append(user).append(", ").append(id).append("), ");
                }
                query.deleteCharAt(query.length() - 1);

                PreparedStatement userAddStatement = connection.prepareStatement(query.toString());
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
        if (!auth.isAdmin() && !Authorization.isParticipantIn(auth, taskId, connection)) {
            throw new EndpointFailedException("Access Denied", EndpointFailedException.Reason.AccessDenied);
        }

        try {
            PreparedStatement statement = connection.prepareStatement("UPDATE task SET due_timestamp=? WHERE id=?");
            statement.setLong(1, timestamp);
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
            PreparedStatement statement = connection.prepareStatement("UPDATE task SET description=? WHERE id=?");
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
            PreparedStatement statement = connection.prepareStatement("UPDATE task SET type=? WHERE id=?");
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
            PreparedStatement statement = connection.prepareStatement("UPDATE task SET group_id=? WHERE id=?");
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
            PreparedStatement statement = connection.prepareStatement("UPDATE task SET group_id=? WHERE id=?");
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
            PreparedStatement statement = connection.prepareStatement("DELETE FROM payment_task WHERE parent_id=?");
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
            PreparedStatement checkStatement = connection.prepareStatement("SELECT parent_id FROM payment_task WHERE parent_id=?");
            checkStatement.setInt(1, taskId);
            ResultSet checkSet = checkStatement.executeQuery();
            if(checkSet.next()){
                PreparedStatement statement = connection.prepareStatement("UPDATE payment_task SET amount=? WHERE parent_id=?");
                statement.setInt(1, amount);
                statement.setInt(2, taskId);

                if(statement.executeUpdate() == 0){throw new EndpointFailedException("nothing changed", EndpointFailedException.Reason.NothingChanged);}

                return "{\"success\":true}";

            }else{
                PreparedStatement statement = connection.prepareStatement("INSERT INTO payment_task (parent_id, amount) VALUES(?, ?)");
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
                PreparedStatement statement = connection.prepareStatement("INSERT INTO user_task (user_id, task_id) VALUES (?, ?)");
                statement.setInt(1, userId);
                statement.setInt(2, taskId);
                if(statement.executeUpdate() == 0){throw new EndpointFailedException("nothing changed", EndpointFailedException.Reason.NothingChanged);}

                return "{\"success\":true}";

            }else{
                PreparedStatement statement = connection.prepareStatement("DELETE FROM user_task WHERE user_id=? AND task_id=?");
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
            PreparedStatement statement = connection.prepareStatement("UPDATE user_task SET is_done=? WHERE user_id=? AND task_id=?");
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
        if (!auth.isAdmin() && !Authorization.isParticipantIn(auth, taskId, connection)) {
            throw new EndpointFailedException("Access Denied", EndpointFailedException.Reason.AccessDenied);
        }

    }
}
