package net.anax.database;

import net.anax.VirtualFileSystem.*;
import net.anax.util.DatabaseUtilities;
import net.anax.util.StringUtilities;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SpecifiedDatabaseStructure {

    private static SpecifiedDatabaseStructure INSTANCE = null;
    private Connection connection;
    private AbstractVirtualNode ROOT_NODE;
    SpecifiedDatabaseStructure(){
        AbstractVirtualNode USERS = new AbstractVirtualNode() {
            @Override
            public AbstractVirtualNode getChildNode(String name, AuthorizationProfile auth) {
                if(!StringUtilities.isInteger(name)){return null;}
                return new VirtualUserNode(Integer.parseInt(name), connection);}
            @Override
            public String getName() {return "users";}
        };
        AbstractVirtualNode GROUPS = new AbstractVirtualNode() {
            @Override
            public AbstractVirtualNode getChildNode(String name, AuthorizationProfile auth) {
                if(!StringUtilities.isInteger(name));
                return new VirtualGroupNode(Integer.parseInt(name), connection);
            }
            @Override public String getName() {return "null";}
        };
        AbstractVirtualNode TASKS = new AbstractVirtualNode() {
            @Override
            public AbstractVirtualNode getChildNode(String name, AuthorizationProfile auth) {
                if(!StringUtilities.isInteger(name)){return null;}
                return new VirtualTaskNode(Integer.parseInt(name), connection);
            }

            @Override public String getName() {return "tasks";}
        };
        ROOT_NODE = new AbstractVirtualNode() {
            @Override
            public AbstractVirtualNode getChildNode(String name, AuthorizationProfile auth) {
                switch (name){
                    case "users" -> {return USERS;}
                    case "groups" -> {return GROUPS;}
                    case "tasks" -> {return TASKS;}
                }
                return null;
            }
            @Override public String getName() {return "root";}
        };
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
    public AbstractVirtualNode getROOT(){return this.ROOT_NODE;}

    static class VirtualUserNode extends AbstractVirtualNode{
        private Connection connection;
        private int id;
        private static final String table = "user";
        public VirtualUserNode(int id, Connection connection){
            this.id = id;
            this.connection = connection;
        }
        @Override
        public AbstractVirtualNode getChildNode(String name, AuthorizationProfile auth) {
            if(auth.getId() != id && !auth.isAdmin() && !Authorization.sharesGroupWith(auth, id, connection)){return null;}
            switch(name){
                case "id" -> {return new UserIdValueNode();}
                case "username" -> {return new UserUsernameValueNode();}
                case "task_id" -> {return new UserTaskIdNode();}
                case "group_id" -> {return new UserGroupIdNode();}
            }
            return null;
        }
        @Override public String readData(AuthorizationProfile auth) {return null;} //TODO: implement user summary;
        @Override public boolean setData(String data, AuthorizationProfile auth) {return false;}//TODO: implement creating  user;
        @Override public boolean delete(AuthorizationProfile auth) {return false;} //TODO: implement deleting user;
        @Override public String getName() {return String.valueOf(id);}
        class UserIdValueNode extends VirtualSimpleValueNode{
            public UserIdValueNode() {
                super(connection, VirtualUserNode.this.id, "id", VirtualUserNode.table);
            }
            @Override
            public String getName() {return "id";}
            @Override
            public AbstractVirtualNode getChildNode(String name, AuthorizationProfile auth) {
                return VirtualUserNode.this.getChildNode(name, auth);
            }

            @Override public boolean authRead(AuthorizationProfile auth) {return true;}
            @Override public boolean authSet(AuthorizationProfile auth) {return false;}
        }
        class UserUsernameValueNode extends VirtualSimpleValueNode{
            public UserUsernameValueNode() {
                super(connection, VirtualUserNode.this.id, "username", VirtualUserNode.table);
            }
            @Override public boolean authRead(AuthorizationProfile auth) {return true;}
            @Override
            public boolean authSet(AuthorizationProfile auth) {return auth.isAdmin() || auth.getId() == id;}

            @Override
            public String getName() {
                return "username";
            }
        }
        class UserTaskIdNode extends AbstractVirtualNode{
            public UserTaskIdNode() {}
            @Override
            public AbstractVirtualNode getChildNode(String name, AuthorizationProfile auth) {
                if(auth.getId() != id && !auth.isAdmin()){return null;}
                if(!StringUtilities.isInteger(name)){return null;}
                return new UserTaskIdIdNode(Integer.parseInt(name));
            }
            @Override
            public String readData(AuthorizationProfile auth) {
                if(auth.getId() != id && !auth.isAdmin()){return null;}
                try {
                    PreparedStatement statement = connection.prepareStatement("SELECT task_id, is_done FROM user_task WHERE user_id=?");
                    statement.setString(1, String.valueOf(VirtualUserNode.this.id));
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
            @Override public String getName() {return "task_id";}
            class UserTaskIdIdNode extends AbstractVirtualNode{
                private int task_id;
                public UserTaskIdIdNode(int task_id) {this.task_id = task_id;}
                @Override
                public AbstractVirtualNode getChildNode(String name, AuthorizationProfile auth) {
                    switch(name){case "is_done" -> {return new UserTaskIdIdIsDoneValueNode();}}
                    return null;
                }
                @Override public String readData(AuthorizationProfile auth) {
                    try {
                        PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM user_task WHERE task_id=? AND user_id=?");
                        statement.setInt(1, task_id);
                        statement.setInt(2, VirtualUserNode.this.id);
                        ResultSet result = statement.executeQuery();
                        if(!result.next()){return null;}
                        JSONObject data = new JSONObject();
                        data.put("id", task_id);
                        return data.toJSONString();

                    } catch (SQLException e) {
                        return null;
                    }
                }
                @Override public boolean setData(String data, AuthorizationProfile auth) {return false;}
                @Override public boolean delete(AuthorizationProfile auth) {return false;} //TODO: implement deleting user.task_id.[task_id]
                @Override public String getName() {return String.valueOf(task_id);}
                class UserTaskIdIdIsDoneValueNode extends AbstractVirtualNode {
                    @Override public AbstractVirtualNode getChildNode(String name, AuthorizationProfile auth) {return null;}
                    @Override
                    public String readData(AuthorizationProfile auth) {
                        try {
                            PreparedStatement statement = connection.prepareStatement("SELECT is_done FROM user_task WHERE user_id=? AND task_id=?");
                            statement.setInt(1, id);
                            statement.setInt(2, task_id);
                            ResultSet result = statement.executeQuery();
                            if(!result.next()){return null;}
                            JSONObject data = new JSONObject();
                            data.put("is_done", result.getString("is_done"));
                            return data.toJSONString();
                        } catch (SQLException e) {
                            return null;
                        }
                    }

                    @Override
                    public boolean setData(String data, AuthorizationProfile auth) {
                        try {
                            if(!auth.isAdmin() && auth.getId() != id){return false;}
                            JSONParser parser = new JSONParser();
                            JSONObject jsonData = (JSONObject) parser.parse(data);

                            if(!jsonData.containsKey("is_done")){return false;}
                            Object isDone = jsonData.get("is_done");
                            if(!(isDone instanceof String)){return false;}
                            if(!StringUtilities.isInteger((String) isDone)){return false;}

                            PreparedStatement statement = connection.prepareStatement("UPDATE user_task SET is_done = ?  WHERE user_id = ? AND task_id = ?");
                            statement.setString(1, (String) isDone);
                            statement.setInt(2, id);
                            statement.setInt(3, task_id);
                            statement.executeQuery();
                            return true;
                        } catch (ParseException e) {
                            return false;
                        } catch (SQLException e) {
                            return false;
                        }

                    }
                    @Override
                    public String getName() {
                        return null;
                    }
                }
            }
        }
        class UserGroupIdNode extends AbstractVirtualNode{
            @Override
            public AbstractVirtualNode getChildNode(String name, AuthorizationProfile auth) {
                if(auth.getId() != id && !auth.isAdmin()){return null;}
                if(!StringUtilities.isInteger(name)){return null;}
                return new UserGroupIdIDValueNode(Integer.parseInt(name));
            }
            @Override public String readData(AuthorizationProfile auth) {
                if(auth.getId() != id && !auth.isAdmin()){return null;}
                try {
                    PreparedStatement statement = connection.prepareStatement("SELECT group_id from user_group where user_id=?");
                    statement.setString(1, String.valueOf(id));
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
            @Override public String getName() {return "group_id";}
            class UserGroupIdIDValueNode extends AbstractVirtualNode{
                int group_id;
                UserGroupIdIDValueNode(int group_id){this.group_id = group_id;}
                @Override public String readData(AuthorizationProfile auth) {
                    try {
                        PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM user_group WHERE user_id=? AND group_id=?");
                        statement.setInt(1, VirtualUserNode.this.id);
                        statement.setInt(2, group_id);
                        ResultSet result = statement.executeQuery();
                        if(!result.next()){return null;}
                        JSONObject data = new JSONObject();
                        data.put("id", group_id);
                        return data.toJSONString();

                    } catch (SQLException e) {
                        return  null;
                    }
                }
                @Override public String getName() {return String.valueOf(group_id);}
            }
        }
    }
    static class VirtualGroupNode extends AbstractVirtualNode{
        int id;
        Connection connection;
        static final String table = "group_table";
        public VirtualGroupNode(int id, Connection connection){
            this.id = id;
            this.connection = connection;
        }
        @Override public String getName() {return String.valueOf(id);}
        @Override
        public AbstractVirtualNode getChildNode(String name, AuthorizationProfile auth) {
            if(!auth.isAdmin() && !Authorization.isMemberOfGroup(auth, id, connection)){return null;}
            switch (name){
                case "id" -> {return new GroupIdValueNode();}
                case "name" -> {return new GroupNameValueNode();}
                case "treasurer_user_id" -> {return new GroupTreasurerUserIdNode();}
                case "user_id" -> {return new GroupUserIdNode();}
                case "task_id" -> {return new GroupTaskIdNode();}
                case "admin_id" -> {return new GroupAdminIdValueNode();}
            }
            return null;
        }
        class GroupIdValueNode extends VirtualSimpleValueNode{
            public GroupIdValueNode() {
                super(connection, VirtualGroupNode.this.id, "id", VirtualGroupNode.table);
            }
            @Override
            public AbstractVirtualNode getChildNode(String name, AuthorizationProfile auth) {
                return VirtualGroupNode.this.getChildNode(name, auth);
            }
            @Override
            public String getName() {return "id";}
            @Override
            public boolean authRead(AuthorizationProfile auth) {return true;}
        }
        class GroupNameValueNode extends VirtualSimpleValueNode{
            public GroupNameValueNode() {super(connection, VirtualGroupNode.this.id, "name", VirtualGroupNode.table);}
            @Override public boolean authRead(AuthorizationProfile auth) {return true;}
            @Override public boolean authSet(AuthorizationProfile auth) {return Authorization.isAdminInGroup(auth, id, connection);}
        }
        class GroupTreasurerUserIdNode extends VirtualSimpleValueNode{
            @Override
            public AbstractVirtualNode getChildNode(String name, AuthorizationProfile auth) {

                String treasurerIdString = DatabaseUtilities.queryString("treasurer_user_id", table, id, connection);
                if(treasurerIdString == null){return null;}
                return new VirtualUserNode(Integer.parseInt(treasurerIdString), connection);
            }
            public GroupTreasurerUserIdNode() {super(connection, VirtualGroupNode.this.id, "treasurer_user_id", VirtualGroupNode.table);}
            @Override public boolean authRead(AuthorizationProfile auth) {return true;}
            @Override public boolean authSet(AuthorizationProfile auth) {return Authorization.isAdminInGroup(auth, id, connection);}
        }
        class GroupUserIdNode extends AbstractVirtualNode{
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
            @Override public String getName() {return "user_id";}

            @Override
            public AbstractVirtualNode getChildNode(String name, AuthorizationProfile auth) {
                if(!StringUtilities.isInteger(name)){return null;}
                return new GroupUserIdIdNode(Integer.parseInt(name));
            }

            class GroupUserIdIdNode extends AbstractVirtualNode{
                int user_id;
                public GroupUserIdIdNode(int user_id){this.user_id = user_id;}
                @Override public String readData(AuthorizationProfile auth) {
                    try {
                        PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM user_group WHERE user_id=? AND group_id=?");
                        statement.setInt(1, user_id);
                        statement.setInt(2, id);
                        ResultSet result = statement.executeQuery();
                        if(!result.next()){return null;}
                        JSONObject data = new JSONObject();
                        data.put("user_id", user_id);
                        return data.toJSONString();

                    } catch (SQLException e) {
                        return null;
                    }

                }

                @Override
                public boolean setData(String data, AuthorizationProfile auth) {
                    //TODO: improve user group adding system;
                    try {
                        if(!Authorization.isMemberOfGroup(auth, id, connection)){return false;}
                        String content = this.readData(auth);
                        if(content != null){return false;}
                        JSONParser parser = new JSONParser();
                        JSONObject value = (JSONObject) parser.parse(data);

                        if(!value.containsKey("user_id")){return false;}

                        Object newValue = value.get("user_id");

                        if(!(newValue instanceof String)){return false;}
                        if(!StringUtilities.isInteger((String) newValue)){return false;}

                        PreparedStatement statement = connection.prepareStatement("INSERT INTO user_group (user_id, group_id) VALUES (?, ?)");
                        statement.setString(1, (String) newValue);
                        statement.setInt(2, id);
                        statement.executeQuery();
                        return true;
                    } catch (SQLException | ParseException e) {
                        return false;
                    }
                }

                @Override public boolean delete(AuthorizationProfile auth) {return false;} //TODO: implement deleting group.user_id;
                @Override public String getName() {return String.valueOf(user_id);}
            }
        }
        class GroupTaskIdNode extends AbstractVirtualNode{
            @Override public String getName() {return "task_id";}
            @Override public String readData(AuthorizationProfile auth) {
                try {
                    PreparedStatement statement = connection.prepareStatement("SELECT id FROM task WHERE group_id=?");
                    statement.setString(1, String.valueOf(id));
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
            public AbstractVirtualNode getChildNode(String name, AuthorizationProfile auth) {
                if(!StringUtilities.isInteger(name)){return null;}
                return new GroupTaskIdIdNode(Integer.parseInt(name));}
            class GroupTaskIdIdNode extends AbstractVirtualNode{
                int task_id;
                public GroupTaskIdIdNode(int task_id){this.task_id = task_id;}
                @Override public String getName() {return String.valueOf(task_id);}
                @Override
                public String readData(AuthorizationProfile auth) {
                    try {
                        PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM task WHERE task.id=? AND task.group_id=?");
                        statement.setInt(1, task_id);
                        statement.setInt(2, id);
                        ResultSet result = statement.executeQuery();
                        if(!result.next()){return null;}
                        JSONObject data = new JSONObject();
                        data.put("task_id", task_id);
                        return data.toJSONString();

                    } catch (SQLException e) {
                        return null;
                    }
                }
            }

        }
        class GroupAdminIdValueNode extends VirtualSimpleValueNode{
            public GroupAdminIdValueNode() {
                super(connection, VirtualGroupNode.this.id, "admin_id", VirtualGroupNode.table);
            }
            @Override public boolean authRead(AuthorizationProfile auth) {return true;}
            @Override public boolean authSet(AuthorizationProfile auth) {return Authorization.isAdminInGroup(auth, id, connection);}
        }
    }
    static class VirtualTaskNode extends AbstractVirtualNode{
        int id;
        Connection connection;
        static final String table = "task";
        public VirtualTaskNode(int id, Connection connection){
            this.id = id;
            this.connection = connection;
        }
        @Override
        public AbstractVirtualNode getChildNode(String name, AuthorizationProfile auth) {
            if(!auth.isAdmin() && !Authorization.isParticipantIn(auth, id, connection)){return null;}
            switch(name){
                case "id" -> {return new TaskIdVirtualNode();}
                case "type" -> {return new TaskTypeVirtualValueNode();}
                case "due_timestamp" -> {return new TaskDueTimeStampValueNode();}
                case "description" -> {return new TaskDescriptionValueNode();}
                case "group_id" -> {return new TaskGroupIdValueNode();}
                case "amount" -> {return new TaskAmountValueNode();}
                case "user_id" -> {return new TaskUserIdNode();}
            }
            return null;
        }
        @Override public String getName() {return String.valueOf(id);}
        class TaskIdVirtualNode extends VirtualSimpleValueNode{
            @Override
            public AbstractVirtualNode getChildNode(String name, AuthorizationProfile auth) {return VirtualTaskNode .this.getChildNode(name, auth);}
            @Override public boolean authRead(AuthorizationProfile auth) {return true;}//TODO: implement task read auth;
            public TaskIdVirtualNode() {super(connection, VirtualTaskNode.this.id, "id", VirtualTaskNode.table);}
        }
        class TaskTypeVirtualValueNode extends VirtualSimpleValueNode{
            public TaskTypeVirtualValueNode() {super(connection, VirtualTaskNode.this.id, "type", VirtualTaskNode.table);}
            @Override public boolean authRead(AuthorizationProfile auth) {return true;}//TODO: implement task read auth;
            @Override public boolean authSet(AuthorizationProfile auth) {return Authorization.isParticipantIn(auth, id, connection);}

            public enum Type {
                Homework(0),
                Payment (1),
                Test(2)
                ;
                public final int type;
                Type(int type){this.type = type;}
            }
        }
        class TaskDueTimeStampValueNode extends VirtualSimpleValueNode{
            public TaskDueTimeStampValueNode() {super(connection, VirtualTaskNode.this.id,"due_timestamp", VirtualTaskNode.table);}
            @Override public boolean authRead(AuthorizationProfile auth) {return true;}//TODO: implement task read auth;
            @Override public boolean authSet(AuthorizationProfile auth) {return Authorization.isParticipantIn(auth, id, connection);}
        }
        class TaskDescriptionValueNode extends VirtualSimpleValueNode{
            public TaskDescriptionValueNode() {super(connection, VirtualTaskNode.this.id, "description", VirtualTaskNode.table);}
            @Override public boolean authRead(AuthorizationProfile auth) {return true;}//TODO: implement task read auth;
            @Override public boolean authSet(AuthorizationProfile auth) {return Authorization.isParticipantIn(auth, id, connection);}
        }
        class TaskGroupIdValueNode extends VirtualSimpleValueNode{
            public TaskGroupIdValueNode() {super(connection, VirtualTaskNode.this.id, "group_id", VirtualTaskNode.table);}
            @Override public boolean authRead(AuthorizationProfile auth) {return true;}//TODO: implement task read auth;
            @Override public boolean authSet(AuthorizationProfile auth) {return Authorization.isParticipantIn(auth, id, connection);}
            @Override
            public AbstractVirtualNode getChildNode(String name, AuthorizationProfile auth) {
                String groupIdString = DatabaseUtilities.queryString("group_id", table, id, connection);
                if(groupIdString == null){return null;}
                return new VirtualGroupNode(Integer.parseInt(groupIdString), connection).getChildNode(name, auth);
            }
        }
        class TaskAmountValueNode extends AbstractVirtualNode{
            @Override
            public String readData(AuthorizationProfile auth) {
                try {
                    PreparedStatement statement = connection.prepareStatement("SELECT amount FROM payment_task WHERE parent_id=?");
                    statement.setInt(1, id);
                    ResultSet result = statement.executeQuery();
                    if(!result.next()){return null;}
                    JSONObject data = new JSONObject();
                    data.put("amount", result.getString("amount"));
                    return data.toJSONString();

                } catch (SQLException e) {
                    return null;
                }
            }

            @Override
            public boolean setData(String data, AuthorizationProfile auth) {
                try {
                    if(!auth.isAdmin() && !Authorization.isParticipantIn(auth, id, connection)){return false;}
                    String currentData = this.readData(auth);

                    JSONParser parser = new JSONParser();
                    JSONObject dataJson = (JSONObject) parser.parse(data);

                    if(!dataJson.containsKey("amount")){return false;}

                    Object amount = dataJson.get("amount");

                    if(!(amount instanceof String)){return false;}
                    if(VirtualTaskNode.this.getChildNode("id", auth) == null){return false;}

                    PreparedStatement statement;
                    if(currentData == null){
                        statement = connection.prepareStatement("INSERT INTO payment_task (parent_id, amount) VALUES (?, ?)");
                        statement.setInt(1, id);
                        statement.setString(2, (String) amount);
                    }else{
                        statement = connection.prepareStatement("UPDATE payment_task SET amount = ? WHERE id = ?");
                        statement.setString(1, (String) amount);
                        statement.setInt(2, id);
                    }
                    statement.executeQuery();
                    return true;
                } catch (SQLException | ParseException e) {
                    return false;
                }
            }

            @Override public String getName() {return "amount";}
        }
        class TaskUserIdNode extends  AbstractVirtualNode{
            @Override
            public String readData(AuthorizationProfile auth) {
                try {
                    PreparedStatement statement = connection.prepareStatement("SELECT user_id, is_done FROM user_task WHERE task_id=?;");
                    statement.setString(1, String.valueOf(id));
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
            @Override public String getName() {return "user_Id";}

            @Override
            public AbstractVirtualNode getChildNode(String name, AuthorizationProfile auth) {
                if(!StringUtilities.isInteger(name)){return null;}
                return new TaskUserIdIdNode(Integer.parseInt(name));
            }
            class TaskUserIdIdNode extends AbstractVirtualNode{
                int user_id;
                public TaskUserIdIdNode(int task_id){this.user_id = task_id;}
                @Override public String readData(AuthorizationProfile auth) {
                    try {
                        PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM user_task WHERE user_id=? AND task_id=?");
                        statement.setInt(1, user_id);
                        statement.setInt(2, id);
                        ResultSet result = statement.executeQuery();
                        if(!result.next()){return null;}
                        JSONObject data = new JSONObject();
                        data.put("user_id", user_id);
                        return data.toJSONString();

                    } catch (SQLException e) {
                        return null;
                    }
                }
                @Override public String getName() {return String.valueOf(user_id);}
                @Override public AbstractVirtualNode getChildNode(String name, AuthorizationProfile auth) {
                    switch (name){case "is_done" -> {return new TaskUserIdIdIsDoneNode();}}
                    return null;
                }
                @Override public boolean setData(String data, AuthorizationProfile auth) {
                    try {
                        if(!auth.isAdmin() && !Authorization.isParticipantIn(auth, id, connection)){return false;}

                        String currentData = this.readData(auth);
                        if(currentData != null){return false;}

                        JSONParser parser = new JSONParser();
                        JSONObject jsonData = (JSONObject) parser.parse(data);

                        if(!jsonData.containsKey("id")){return false;}

                        Object newId = jsonData.get("id");

                        if(!(newId instanceof String)){return false;}

                        PreparedStatement statement = connection.prepareStatement("INSERT INTO user_task (user_id, task_id) VALUES (?, ?)");
                        statement.setString(1, (String) newId);
                        statement.setInt(2, id);
                        statement.executeQuery();

                        return true;
                    } catch (ParseException e) {
                        return false;
                    } catch (SQLException e) {
                        return false;
                    }
                }
                class TaskUserIdIdIsDoneNode extends AbstractVirtualNode{
                    @Override
                    public String readData(AuthorizationProfile auth) {
                        try {
                            PreparedStatement statement = connection.prepareStatement("SELECT is_done FROM user_task WHERE user_id=? AND task_id=?");
                            statement.setInt(1, user_id);
                            statement.setInt(2, id);
                            ResultSet result = statement.executeQuery();
                            if(!result.next()){return null;}
                            JSONObject data = new JSONObject();
                            data.put("is_done", result.getString("is_done"));
                            return data.toJSONString();
                        } catch (SQLException e) {
                            return null;
                        }
                    }

                    @Override
                    public boolean setData(String data, AuthorizationProfile auth) {

                        try {
                            if(!auth.isAdmin() && auth.getId() != user_id){return false;}

                            JSONParser parser = new JSONParser();
                            JSONObject jsonData = (JSONObject) parser.parse(data);

                            if(!jsonData.containsKey("is_done")){return false;}
                            Object newData = jsonData.get("is_done");
                            if(!(newData instanceof String)){return false;}

                            if(!StringUtilities.isInteger((String) newData)){return false;}
                            PreparedStatement statement = connection.prepareStatement("UPDATE user_task SET is_done = ? WHERE task_id = ? AND user_id = ?");
                            statement.setString(1, (String) newData);
                            statement.setInt(2, id);
                            statement.setInt(3, user_id);
                            statement.executeQuery();
                            return true;
                        } catch (ParseException e) {
                            return false;
                        } catch (SQLException e) {
                            return false;
                        }
                    }

                    @Override public String getName() {return "is_done";}
                }
            }
        }

    }
}
