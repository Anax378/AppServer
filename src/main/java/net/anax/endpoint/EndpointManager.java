package net.anax.endpoint;

import net.anax.VirtualFileSystem.AuthorizationProfile;
import net.anax.cryptography.KeyManager;
import org.json.simple.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;

public class EndpointManager {
    Connection connection;
    UserEndpointManager userEndpointManager;
    GroupEndpointManager groupEndpointManager;
    TaskEndpointManager taskEndpointManager;

    public EndpointManager(Connection connection){
        this.connection = connection;
        userEndpointManager = new UserEndpointManager(connection);
        groupEndpointManager = new GroupEndpointManager(connection);
        taskEndpointManager = new TaskEndpointManager(connection);
    }

    public String callEndpoint(String path, JSONObject data, AuthorizationProfile auth) throws EndpointFailedException {
        String[] parts = path.split("/");
        if(parts.length < 2){throw new EndpointFailedException("invalid url", EndpointFailedException.Reason.DataNotFound);}
        String ret = null;


        try {
            PreparedStatement statement = connection.prepareStatement("START TRANSACTION;");
            statement.execute();
        } catch (SQLException e) {
            throw new EndpointFailedException("could not start sql transaction", EndpointFailedException.Reason.UnexpectedError);
        }


        try {
            switch(parts[0]){
                case ("user") -> ret = userEndpointManager.callEndpoint(parts[1], data, auth);
                case ("group") -> ret = groupEndpointManager.callEndpoint(parts[1], data, auth);
                case ("task") -> ret = taskEndpointManager.callEndpoint(parts[1], data, auth);
            }
        } catch(EndpointFailedException e){
            try {
                PreparedStatement rollback = connection.prepareStatement("ROLLBACK;");
                rollback.execute();
                throw e;
            } catch (SQLException ex) {
                throw new EndpointFailedException("FAILED TO ROLLBACK FAILED ENDPOINT EXECUTION", EndpointFailedException.Reason.UnexpectedError);
            }
        }

        try {
            PreparedStatement commit = connection.prepareStatement("COMMIT;");
            commit.execute();
        } catch (SQLException e) {
            throw new EndpointFailedException("failed to commit transaction", EndpointFailedException.Reason.UnexpectedError);
        }

        return ret;
    }


}
