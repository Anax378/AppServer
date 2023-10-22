package net.anax.VirtualFileSystem;

public class UserAuthorizationProfile extends AuthorizationProfile{
    private String name;
    public UserAuthorizationProfile(String name){
        this.name = name;
    }
    @Override
    public boolean isAdmin() {
        return false;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
