package net.anax.VirtualFileSystem;

public abstract class AbstractVirtualFolder {
    String name;

    public AbstractVirtualFolder(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public VirtualFile getFileFromPATH(VirtualPathNode node, AuthorizationProfile auth){
        if(node == null){return null;}
        if(node.next == null){
            return this.getFile(node.data, auth);
        }
        AbstractVirtualFolder nextFolder;
        if((nextFolder = this.getFolder(node.data, auth)) != null){
            return nextFolder.getFileFromPATH(node.next, auth);
        }
        System.out.println(node.data + "not found in " + this.name);
        return null;
    }

    public abstract AbstractVirtualFolder getFolder(String name, AuthorizationProfile auth);
    public abstract VirtualFile getFile(String name, AuthorizationProfile auth);

}
