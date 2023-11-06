package net.anax.VirtualFileSystem;

public class UserAuthorizationProfile extends AuthorizationProfile{
    private int id;
    public UserAuthorizationProfile(int id){
        this.id = id;
    }
    @Override
    public boolean isAdmin() {
        return false;
    }
    @Override
    public int getId() {
        return id;
    }

}
