package net.anax.VirtualFileSystem;

public abstract class VirtualFile {
    private String name;
    public VirtualFile(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }
    abstract public String readData();
}
