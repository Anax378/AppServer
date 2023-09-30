package net.anax.VirtualFileSystem;

public abstract class AbstractVirtualFolder {
    String name;

    public AbstractVirtualFolder(String name){
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public VirtualFile getFileFromPATH(VirtualPathNode node){
        if(node == null){return null;}
        if(node.next == null){
            return this.getFile(node.data);
        }
        AbstractVirtualFolder nextFolder;
        if((nextFolder = this.getFolder(node.data)) != null){
            return nextFolder.getFileFromPATH(node.next);
        }
        System.out.println(node.data + "not found in " + this.name);
        return null;
    }

    public abstract AbstractVirtualFolder getFolder(String name);
    public abstract VirtualFile getFile(String name);

}
