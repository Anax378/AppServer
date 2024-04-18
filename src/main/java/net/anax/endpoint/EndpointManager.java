package net.anax.endpoint;

import net.anax.VirtualFileSystem.AuthorizationProfile;
import net.anax.cryptography.KeyManager;
import net.anax.logging.Logger;
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

    public String callEndpoint(String path, JSONObject data, AuthorizationProfile auth, long traceId) throws EndpointFailedException {
        String[] parts = path.split("/");
        if(parts.length < 2){
            throw new EndpointFailedException("invalid url", EndpointFailedException.Reason.DataNotFound);
        }

        String ret = null;

        try {
            PreparedStatement statement = connection.prepareStatement("START TRANSACTION;");
            statement.execute();
        } catch (SQLException e) {
            throw new EndpointFailedException("could not start sql transaction", EndpointFailedException.Reason.UnexpectedError);
        }


        try {
            Logger.info("attempting to call " + parts[0] + "/" + parts[1], traceId);
            switch(parts[0]){
                case ("user") -> ret = userEndpointManager.callEndpoint(parts[1], data, auth, traceId);
                case ("group") -> ret = groupEndpointManager.callEndpoint(parts[1], data, auth, traceId);
                case ("task") -> ret = taskEndpointManager.callEndpoint(parts[1], data, auth, traceId);
                default -> {Logger.log("not a valid endpoint category: " + parts[0], traceId);}
            }
        } catch(EndpointFailedException e){
            try {
                PreparedStatement rollback = connection.prepareStatement("ROLLBACK;");
                rollback.execute();
                Logger.log("rolling back unsuccessful endpoint call", traceId);
                throw e;
            } catch (SQLException ex) {
                Logger.error("FAILED TO ROLLBACK FAILED ENDPOINT EXECUTION, THE DATABASE MIGHT BE COMPROMISED", traceId);
                throw new EndpointFailedException("FAILED TO ROLLBACK FAILED ENDPOINT EXECUTION", EndpointFailedException.Reason.UnexpectedError);
            }
        }

        try {
            PreparedStatement commit = connection.prepareStatement("COMMIT;");
            commit.execute();
        } catch (SQLException e) {
            Logger.error("failed to commit transaction", traceId);
            throw new EndpointFailedException("failed to commit transaction", EndpointFailedException.Reason.UnexpectedError);
        }

        return ret;
    }


}
