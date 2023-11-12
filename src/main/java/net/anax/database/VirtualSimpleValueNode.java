package net.anax.database;

import net.anax.VirtualFileSystem.AbstractVirtualNode;
import net.anax.VirtualFileSystem.AuthorizationProfile;
import net.anax.logging.Logger;
import net.anax.util.DatabaseUtilities;
import net.anax.util.StringUtilities;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class VirtualSimpleValueNode extends AbstractVirtualNode {
    private Connection connection;
    int id;
    String field;
    String table;

    public VirtualSimpleValueNode(Connection connection, int id, String field, String table) {
        this.connection = connection;
        this.id = id;
        this.field = field;
        this.table = table;
    }
    @Override
    public String readData(AuthorizationProfile auth) {
        if(!this.authRead(auth)){
            return null;
        }
        return StringUtilities.simpleWrapInJson(DatabaseUtilities.queryString(field, table, id, connection), field);
    }

    @Override
    public boolean setData(String data, AuthorizationProfile auth) {
        try {
            if(!authSet(auth)){return false;}
            JSONParser parser = new JSONParser();
            JSONObject jsonData = (JSONObject) parser.parse(data);

            if(!jsonData.containsKey(field)){return false;}

            Object newData = jsonData.get(field);
            if(!(newData instanceof String)){return false;}
            if(((String)newData).isEmpty()){return false;}

            PreparedStatement statement = connection.prepareStatement("UPDATE " + table + " SET " + field + " =? WHERE id=?");

            statement.setString(1, (String) newData);
            statement.setInt(2, id);
            statement.executeQuery();
            return true;
        } catch (SQLException | ParseException e) {
            return false;
        }
    }

    @Override
    public boolean delete(AuthorizationProfile auth) {
        if(!authDelete(auth)){return false;}
        //TODO: implement deleting simple data;
        return false;
    }
    public boolean authRead(AuthorizationProfile auth){return false;};
    public boolean authSet(AuthorizationProfile auth){return false;};
    public boolean authDelete(AuthorizationProfile auth){return false;};
    @Override
    public String getName() {return field;}
}
