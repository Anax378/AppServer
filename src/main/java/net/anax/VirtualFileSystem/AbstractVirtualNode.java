package net.anax.VirtualFileSystem;

public abstract class AbstractVirtualNode {
    public AbstractVirtualNode getChildNode(String name, AuthorizationProfile auth){return  null;}
    public String readData(AuthorizationProfile auth){return null;}
    public boolean setData(String data, AuthorizationProfile auth){return false;}
    public boolean delete(AuthorizationProfile auth){return false;}
    public abstract String getName();

    public AbstractVirtualNode getFileFromPATH(VirtualPathNode node, AuthorizationProfile auth){
        if(node == null){
            return null;
        }
        if(node.next == null){
            return this.getChildNode(node.data, auth);
        }
        AbstractVirtualNode nextFolder;
        if((nextFolder = this.getChildNode(node.data, auth)) != null){
            return nextFolder.getFileFromPATH(node.next, auth);
        }
        System.out.println(node.data + "not found in " + this.getName());
        return null;
    }

}
