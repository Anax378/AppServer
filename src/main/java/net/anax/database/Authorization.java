package net.anax.database;

import net.anax.VirtualFileSystem.AuthorizationProfile;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Authorization {
    public static boolean isMemberOfGroup(AuthorizationProfile auth, int group_id, Connection connection){
        try {
            PreparedStatement statement = connection.prepareStatement("Select 1 FROM user_group WHERE user_id=? AND group_id=?");
            statement.setInt(1, auth.getId());
            statement.setInt(2, group_id);
            return statement.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }

    }

    public static boolean isParticipantIn(AuthorizationProfile auth, int task_id, Connection connection){
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM user_task WHERE user_id=? AND task_id=?");
            statement.setInt(1, auth.getId());
            statement.setInt(2, task_id);
            return statement.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean sharesGroupWith(AuthorizationProfile auth, int user_id, Connection connection){
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM user_group WHERE user_id=? AND group_id IN (SELECT group_id FROM user_group WHERE user_id=?)");
            statement.setInt(1, auth.getId());
            statement.setInt(2, user_id);
            return statement.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }
    };
}
