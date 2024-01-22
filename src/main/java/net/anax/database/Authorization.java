package net.anax.database;

import net.anax.VirtualFileSystem.AuthorizationProfile;
import net.anax.endpoint.EndpointFailedException;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Authorization {
    static final int passwordHashIterations = 1000;
    static final int saltLength = 32;

    public static boolean isMemberOfGroup(AuthorizationProfile auth, int group_id, Connection connection) {
        try {
            PreparedStatement statement = connection.prepareStatement("Select 1 FROM user_group WHERE user_id=? AND group_id=?");
            statement.setInt(1, auth.getId());
            statement.setInt(2, group_id);
            return statement.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }

    }

    public static boolean isParticipantIn(AuthorizationProfile auth, int task_id, Connection connection) {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM user_task WHERE user_id=? AND task_id=?");
            statement.setInt(1, auth.getId());
            statement.setInt(2, task_id);
            return statement.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }
    }

    public static boolean sharesGroupWith(AuthorizationProfile auth, int user_id, Connection connection) {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM user_group WHERE user_id=? AND group_id IN (SELECT group_id FROM user_group WHERE user_id=?)");
            statement.setInt(1, auth.getId());
            statement.setInt(2, user_id);
            return statement.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }
    }

    ;

    public static boolean isAdminInGroup(AuthorizationProfile auth, int group_id, Connection connection) {
        try {
            PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM group_table WHERE id=? AND admin_id=?");
            statement.setInt(1, group_id);
            statement.setInt(2, auth.getId());
            return statement.executeQuery().next();
        } catch (SQLException e) {
            return false;
        }

    }

    public static byte[] generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[saltLength];
        random.nextBytes(salt);
        return salt;
    }

    public static byte[] generatePasswordHash(String password, byte[] salt) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        byte[] toHash = new byte[password.length() + salt.length];
        byte[] passwordBytes = password.getBytes();

        System.arraycopy(passwordBytes, 0, toHash, 0, passwordBytes.length);
        System.arraycopy(salt, 0, toHash, passwordBytes.length, toHash.length);

        byte[] result = toHash;

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (int i = 0; i < passwordHashIterations; i++) {
            result = digest.digest(result);
        }
        return digest.digest(toHash);
    }
}