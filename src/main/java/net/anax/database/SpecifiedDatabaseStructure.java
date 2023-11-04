package net.anax.database;

import net.anax.VirtualFileSystem.AbstractVirtualFolder;
import net.anax.VirtualFileSystem.AuthorizationProfile;
import net.anax.VirtualFileSystem.VirtualFile;
import net.anax.VirtualFileSystem.VirtualFolder;
import net.anax.util.DatabaseUtilities;
import net.anax.util.StringUtilities;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SpecifiedDatabaseStructure {

    private static SpecifiedDatabaseStructure INSTANCE = null;
    private Connection connection;
    private VirtualFolder ROOT = new VirtualFolder("root");
    SpecifiedDatabaseStructure(){
        AbstractVirtualFolder USERS = new AbstractVirtualFolder("users"){
            @Override
            public VirtualFile getFile(String fileName, AuthorizationProfile auth) {
                //TODO: implement user summaries;
                return null;
            }
            @Override
            public AbstractVirtualFolder getFolder(String folderName, AuthorizationProfile auth) {
                if(!StringUtilities.isInteger(folderName)){return null;}
                int id = Integer.parseInt(folderName);
                return new VirtualUserFolder(id, connection);
            }
        };
        AbstractVirtualFolder GROUPS = new AbstractVirtualFolder("groups") {
            @Override
            public VirtualFile getFile(String name, AuthorizationProfile auth) {
                //TODO: implement group summaries;
                return null;
            }
            @Override
            public AbstractVirtualFolder getFolder(String folderName, AuthorizationProfile auth) {
                if(!StringUtilities.isInteger(folderName)){return null;}
                int id = Integer.parseInt(folderName);
                return new VirtualGroupFolder(id, connection);
            }
        };
        AbstractVirtualFolder TASKS = new AbstractVirtualFolder("tasks") {
            @Override
            public VirtualFile getFile(String name, AuthorizationProfile auth) {
                //TODO: implement task summaries;
                return null;
            }
            @Override
            public AbstractVirtualFolder getFolder(String name, AuthorizationProfile auth) {
                if(!StringUtilities.isInteger(name)){return null;}
                int id = Integer.parseInt(name);
                return new VirtualTaskFolder(id, connection);
            }

        };

        ROOT.addFolder(USERS);
        ROOT.addFolder(GROUPS);
        ROOT.addFolder(TASKS);

    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }
    public static SpecifiedDatabaseStructure getINSTANCE(){
        if(INSTANCE == null){
            INSTANCE = new SpecifiedDatabaseStructure();
        }
        return INSTANCE;
    }

    public VirtualFolder getROOT(){return this.ROOT;}
    static class VirtualUserFolder extends AbstractVirtualFolder{
        Connection connection;
        int id;
        static final String table = "user";
        public VirtualUserFolder(int id, Connection connection) {
            super(String.valueOf(id));
            this.connection = connection;
            this.id = id;
        }
        @Override
        public AbstractVirtualFolder getFolder(String name, AuthorizationProfile auth) {
            switch (name){
                case "id" -> {return this;}
            }
            return null;
        }
        @Override
        public VirtualFile getFile(String name, AuthorizationProfile auth) {
            switch(name){
                case "id" -> {return new UserIdFile(this.id, connection);}
                case "username" -> {return new UserUsernameFile(this.id, connection);}
                case "group_id" -> {return new UserGroupIdFile(this.id, connection);}
                case "task_id" -> {return new UserTaskIdFile(this.id, connection);}
            }
            return null;
        }
        static class UserIdFile extends VirtualDatabaseFile{
            public UserIdFile(int id, Connection connection) {
                super(id, connection, "id");
            }

            @Override
            public String readData(AuthorizationProfile auth) {
                return StringUtilities.simpleWrapInJson(DatabaseUtilities.queryString("id", table, id, connection), "id");
            }
            @Override
            public void setData(String data, AuthorizationProfile auth) {
                //TODO: implement setting data;
            }
            @Override
            public boolean deleteData(AuthorizationProfile auth) {return false;}
        }
        static class UserUsernameFile extends VirtualDatabaseFile{
            public UserUsernameFile(int id, Connection connection) {
                super(id, connection, "username");
            }

            @Override
            public String readData(AuthorizationProfile auth) {
                return StringUtilities.simpleWrapInJson(DatabaseUtilities.queryString("username", table, this.id, this.connection), "username");
            }

            @Override
            public void setData(String data, AuthorizationProfile auth) {
                //TODO: implement setting user.username
            }
            @Override
            public boolean deleteData(AuthorizationProfile auth) {return false;}
        }
        static class UserGroupIdFile extends VirtualDatabaseFile {

            public UserGroupIdFile(int id, Connection connection) {
                super(id, connection, "group_id");
            }

            @Override
            public String readData(AuthorizationProfile auth) {
                try {
                    PreparedStatement statement = connection.prepareStatement("SELECT group_id from user_group where user_id=?");
                    statement.setString(1, String.valueOf(this.id));
                    ResultSet result = statement.executeQuery();
                    if(result == null){return null;}
                    JSONArray array = new JSONArray();
                    while(result.next()){
                        array.add(result.getString("group_id"));
                    }
                    JSONObject data = new JSONObject();
                    data.put("group_id", array);
                    return data.toJSONString();
                } catch (SQLException e) {
                    return null;
                }
            }

            @Override
            public void setData(String data, AuthorizationProfile auth) {
                //TODO: implement setting user.group_id;
            }

            @Override
            public boolean deleteData(AuthorizationProfile auth) {
                //TODO: implement deleting user.group_id;
                return false;
            }
        }
        static class UserTaskIdFile extends VirtualDatabaseFile{

            public UserTaskIdFile(int id, Connection connection) {
                super(id, connection, "task_id");
            }

            @Override
            public String readData(AuthorizationProfile auth) {
                try {
                    PreparedStatement statement = connection.prepareStatement("SELECT task_id, is_done FROM user_task WHERE user_id=?");
                    statement.setString(1, String.valueOf(this.id));
                    ResultSet result = statement.executeQuery();
                    if(result == null){return null;}
                    JSONArray task_id_array = new JSONArray();
                    JSONArray is_done_array = new JSONArray();
                    while(result.next()){
                        task_id_array.add(result.getString("task_id"));
                        is_done_array.add(result.getString("is_done"));
                    }
                    JSONObject data = new JSONObject();
                    data.put("task_id", task_id_array);
                    data.put("is_done", is_done_array);
                    return data.toJSONString();
                } catch (SQLException e) {
                    return null;
                }
            }

            @Override
            public void setData(String data, AuthorizationProfile auth) {
                //TODO: implement setting user.task_id;
            }

            @Override
            public boolean deleteData(AuthorizationProfile auth) {
                //TODO: implement deleting user.task_id;
                return false;
            }
        }

    }
    static class VirtualGroupFolder extends AbstractVirtualFolder{
        static final String table = "group_table";
        int id;
        Connection connection;
        public VirtualGroupFolder(int id, Connection connection) {
            super(String.valueOf(id));
            this.id = id;
            this.connection = connection;
        }

        @Override
        public AbstractVirtualFolder getFolder(String name, AuthorizationProfile auth) {
            switch(name){
                case "id" -> {return this;}
                case "treasurer_user_id" -> {
                    GroupTreasurerUserIdFile treasurerUserIdFile = new GroupTreasurerUserIdFile(id, connection);
                    String treasurer_id_String = treasurerUserIdFile.readData(auth);
                    if(treasurer_id_String == null || !StringUtilities.isInteger(treasurer_id_String)){return null;}
                    int treasurer_id = Integer.parseInt(treasurer_id_String);
                    return new VirtualUserFolder(treasurer_id, connection);
                }
            }
            return null;
        }

        @Override
        public VirtualFile getFile(String name, AuthorizationProfile auth) {
            switch(name){
                case "id" -> {return new GroupIdFile(this.id, connection);}
                case "name" -> {return new GroupNameFile(this.id, connection);}
                case "treasurer_user_id" -> {return new GroupTreasurerUserIdFile(this.id, connection);}
                case "user_id" -> {return new GroupUserIdFile(this.id, connection);}
                case "task_id" -> {return new GroupTaskIdFile(this.id, connection);}
            }
            return null;
        }

        static class GroupIdFile extends VirtualDatabaseFile{

            public GroupIdFile(int id, Connection connection) {
                super(id, connection, "id");
            }

            @Override
            public String readData(AuthorizationProfile auth) {
                return StringUtilities.simpleWrapInJson(DatabaseUtilities.queryString("id", table, this.id, this.connection), "id");
            }

            @Override
            public void setData(String data, AuthorizationProfile auth) {}

            @Override
            public boolean deleteData(AuthorizationProfile auth) {return false;}
        }
        static class GroupNameFile extends VirtualDatabaseFile {

            public GroupNameFile(int id, Connection connection) {
                super(id, connection, "name");
            }

            @Override
            public String readData(AuthorizationProfile auth) {
                return StringUtilities.simpleWrapInJson(DatabaseUtilities.queryString("name", table, this.id, this.connection), "name");
            }

            @Override
            public void setData(String data, AuthorizationProfile auth) {
                //TODO: implement setting group.name;
            }

            @Override
            public boolean deleteData(AuthorizationProfile auth) {return false;}
        }
        static class GroupTreasurerUserIdFile extends VirtualDatabaseFile{

            public GroupTreasurerUserIdFile(int id, Connection connection) {
                super(id, connection, "treasurer_user_id");
            }

            @Override
            public String readData(AuthorizationProfile auth) {
                return StringUtilities.simpleWrapInJson(DatabaseUtilities.queryString("treasurer_user_id", table, this.id, this.connection), "treasurer_user_id");
            }

            @Override
            public void setData(String data, AuthorizationProfile auth) {
                //TODO: implement setting group.treasurer_user_id;
            }

            @Override
            public boolean deleteData(AuthorizationProfile auth) {
                //TODO: implement deleting group.treasurer_user_id;
                return false;
            }
        }
        static class GroupUserIdFile extends VirtualDatabaseFile {

            public GroupUserIdFile(int id, Connection connection) {
                super(id, connection, "user_id");
            }

            @Override
            public String readData(AuthorizationProfile auth) {
                try {
                    PreparedStatement statement = connection.prepareStatement("SELECT user_id FROM user_group WHERE group_id=?");
                    statement.setString(1, String.valueOf(id));
                    ResultSet result = statement.executeQuery();
                    if(result == null){return null;}
                    JSONArray array = new JSONArray();
                    while(result.next()){
                        array.add(result.getString("user_id"));
                    }
                    JSONObject data = new JSONObject();
                    data.put("user_id", array);
                    return data.toJSONString();
                } catch (SQLException e) {
                    return null;
                }
            }

            @Override
            public void setData(String data, AuthorizationProfile auth) {
                //TODO: implement setting group.user_id;
            }

            @Override
            public boolean deleteData(AuthorizationProfile auth) {
                //TODO: implement deleting group.user_id;
                return false;
            }
        }
        static class GroupTaskIdFile extends VirtualDatabaseFile {

            public GroupTaskIdFile(int id, Connection connection) {
                super(id, connection, "task_id");
            }

            @Override
            public String readData(AuthorizationProfile auth) {
                try {
                    PreparedStatement statement = connection.prepareStatement("SELECT id FROM task WHERE group_id=?");
                    statement.setString(1, String.valueOf(this.id));
                    ResultSet result = statement.executeQuery();
                    if(result == null){return null;}
                    JSONArray array = new JSONArray();
                    while(result.next()){
                        array.add(result.getString("id"));
                    }
                    JSONObject data =  new JSONObject();
                    data.put("task_id", array);
                    return data.toJSONString();

                } catch (SQLException e) {
                    return null;
                }
            }

            @Override
            public void setData(String data, AuthorizationProfile auth) {
                //TODO: implement setting group.task_id;
            }

            @Override
            public boolean deleteData(AuthorizationProfile auth) {
                //TODO: implement deleting group.tasl_id
                return false;
            }
        }

    }
    static class VirtualTaskFolder extends AbstractVirtualFolder{
        static final String table = "task";
        Connection connection;
        int id;
        public VirtualTaskFolder(int id, Connection connection) {
            super(String.valueOf(id));
            this.id = id;
            this.connection = connection;
        }

        @Override
        public AbstractVirtualFolder getFolder(String name, AuthorizationProfile auth) {
            switch (name){
                case "id" -> {return this;}
                case "group_id" -> {
                    TaskGroupIdFile groupIdFile = new TaskGroupIdFile(this.id, connection);
                    String groupIdString = groupIdFile.readData(auth);
                    if(groupIdString == null || !StringUtilities.isInteger(groupIdString)){return null;}
                    int group_id = Integer.parseInt(groupIdString);
                    return new VirtualGroupFolder(group_id, connection);
                }
            }
            return null;
        }

        @Override
        public VirtualFile getFile(String name, AuthorizationProfile auth) {
            switch (name){
                case "id" -> {return new TaskIdFile(id, connection);}
                case "type" -> {return new TaskTypeFile(id, connection);}
                case "due_timestamp" -> {return new TaskDueTimestampFile(id, connection);}
                case "description" -> {return new TaskDescriptionFile(id, connection);}
                case "group_id" -> {return new TaskGroupIdFile(id, connection);}
                case "amount" -> {return  new TaskAmountFile(id, connection);}
                case "user_id" -> {return new TaskUserIdFile(id, connection);}
            }
            return null;
        }

        static class TaskIdFile extends VirtualDatabaseFile {
            public TaskIdFile(int id, Connection connection) {
                super(id, connection, "id");
            }
            @Override
            public String readData(AuthorizationProfile auth) {
                return StringUtilities.simpleWrapInJson(DatabaseUtilities.queryString("id", table, this.id, this.connection), "id");
            }
            @Override
            public void setData(String data, AuthorizationProfile auth) {}
            @Override
            public boolean deleteData(AuthorizationProfile auth) {return false;}
        }
        static class TaskTypeFile extends VirtualDatabaseFile {

            public TaskTypeFile(int id, Connection connection) {
                super(id, connection, "type");
            }

            @Override
            public String readData(AuthorizationProfile auth) {
                return StringUtilities.simpleWrapInJson(DatabaseUtilities.queryString("type", table, this.id, this.connection), "type");
            }

            @Override
            public void setData(String data, AuthorizationProfile auth) {
                //TODO: implement setting task.type;
            }

            @Override
            public boolean deleteData(AuthorizationProfile auth) {return false;}
        }
        static class TaskDueTimestampFile extends VirtualDatabaseFile{
            public TaskDueTimestampFile(int id, Connection connection) {
                super(id, connection, "due_timestamp");
            }

            @Override
            public String readData(AuthorizationProfile auth) {
                return StringUtilities.simpleWrapInJson(DatabaseUtilities.queryString("due_timestamp", table, this.id, this.connection), "due_timestamp");
            }

            @Override
            public void setData(String data, AuthorizationProfile auth) {
                //TODO: implement setting task.due_timestamp;
            }

            @Override
            public boolean deleteData(AuthorizationProfile auth) {return false;}
        }
        static class TaskDescriptionFile extends VirtualDatabaseFile{
            public TaskDescriptionFile(int id, Connection connection) {
                super(id, connection, "description");
            }

            @Override
            public String readData(AuthorizationProfile auth) {
                return StringUtilities.simpleWrapInJson(DatabaseUtilities.queryString("description", table, this.id, this.connection), "description");
            }
            @Override
            public void setData(String data, AuthorizationProfile auth) {
                //TODO: implement setting task.description;
            }
            @Override
            public boolean deleteData(AuthorizationProfile auth) {return false;}
        }
        static class TaskGroupIdFile extends VirtualDatabaseFile{

            public TaskGroupIdFile(int id, Connection connection) {
                super(id, connection, "group_id");
            }

            @Override
            public String readData(AuthorizationProfile auth) {
                return StringUtilities.simpleWrapInJson(DatabaseUtilities.queryString("group_id", table, this.id, this.connection), "group_id");
            }

            @Override
            public void setData(String data, AuthorizationProfile auth) {
                //TODO: implement setting task.group_id;
            }

            @Override
            public boolean deleteData(AuthorizationProfile auth) {return false;}
        }
        static class TaskAmountFile extends VirtualDatabaseFile{
            public TaskAmountFile(int id, Connection connection) {
                super(id, connection, "amount");
            }

            @Override
            public String readData(AuthorizationProfile auth) {
                try {
                    PreparedStatement statement = connection.prepareStatement("SELECT amount FROM payment_task WHERE parent_id=?");
                    statement.setString(1, String.valueOf(this.id));
                    ResultSet result = statement.executeQuery();
                    if(result == null){return null;}
                    if(!result.next()){return null;}
                    JSONObject data = new JSONObject();
                    data.put("amount", result.getString("amount"));
                    return data.toJSONString();
                } catch (SQLException e) {
                    return null;
                }
            }

            @Override
            public void setData(String data, AuthorizationProfile auth) {
                //TODO: implement setting task.amount;
            }
            @Override
            public boolean deleteData(AuthorizationProfile auth) {return false;}
        }
        static class TaskUserIdFile extends VirtualDatabaseFile {

            public TaskUserIdFile(int id, Connection connection) {
                super(id, connection, "user_id");
            }

            @Override
            public String readData(AuthorizationProfile auth) {
                try {
                    PreparedStatement statement = connection.prepareStatement("SELECT user_id, is_done FROM user_task WHERE task_id=?;");
                    statement.setString(1, String.valueOf(this.id));
                    ResultSet result = statement.executeQuery();
                    if(result == null){return null;}
                    JSONArray user_id = new JSONArray();
                    JSONArray is_done = new JSONArray();
                    while(result.next()){
                        user_id.add(result.getString("user_id"));
                        is_done.add(result.getString("is_done"));
                    }
                    JSONObject data = new JSONObject();
                    data.put("user_id", user_id);
                    data.put("is_done", is_done);
                    return data.toJSONString();
                } catch (SQLException e) {
                    return null;
                }
            }

            @Override
            public void setData(String data, AuthorizationProfile auth) {
                //TODO: implement setting task.user_id;
            }

            @Override
            public boolean deleteData(AuthorizationProfile auth) {
                //TODO: implement deleting task.user_id;
                return false;
            }
        }
    }
}
