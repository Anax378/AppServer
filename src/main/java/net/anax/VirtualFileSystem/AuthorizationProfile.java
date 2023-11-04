package net.anax.VirtualFileSystem;

public abstract class AuthorizationProfile {
    public abstract boolean isAdmin();
    public abstract String getName();

    public abstract int getId();
}
