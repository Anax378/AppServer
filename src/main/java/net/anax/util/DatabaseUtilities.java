package net.anax.util;

import net.anax.logging.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseUtilities {
    public static ResultSet regularQuery(String name, String table, int id, Connection connection){
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT " + name + " FROM " + table + " WHERE id=?");
            statement.setString(1, String.valueOf(id));
            ResultSet result = statement.executeQuery();
            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String queryString(String name, String table, int id, Connection connection){
        try {
            ResultSet result = regularQuery(name, table, id, connection);
            if(result == null){return null;}
            if(!result.next()){return null;}
            return result.getString(name);
        } catch (SQLException e) {
            return null;
        }
    }
}
