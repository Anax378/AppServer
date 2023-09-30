package net.anax.VirtualFileSystem;

public class VirtualPathNode {
    public String data;
    public VirtualPathNode next;

    public VirtualPathNode(String data, VirtualPathNode next) {
        this.data = data;
        this.next = next;
    }
}
