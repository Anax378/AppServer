package net.anax.VirtualFileSystem;

public class UserAuthorizationProfile extends AuthorizationProfile{
    private String name;
    private int id;
    public UserAuthorizationProfile(String name, int id){
        this.name = name;
        this.id = id;
    }
    @Override
    public boolean isAdmin() {
        return false;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }
}
