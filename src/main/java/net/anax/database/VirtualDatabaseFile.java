package net.anax.database;

import net.anax.VirtualFileSystem.AuthorizationProfile;
import net.anax.VirtualFileSystem.VirtualFile;

import java.sql.Connection;

abstract public class VirtualDatabaseFile extends VirtualFile {
    int id;
    Connection connection;
    public VirtualDatabaseFile(int id, Connection connection, String name) {
        super(name);
        this.id = id;
        this.connection = connection;
    }

    public abstract void setData(String data, AuthorizationProfile auth);
    public abstract boolean deleteData(AuthorizationProfile auth);

}
