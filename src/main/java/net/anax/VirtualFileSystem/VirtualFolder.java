package net.anax.VirtualFileSystem;

import java.util.HashMap;

public class VirtualFolder extends AbstractVirtualFolder{
    private HashMap<String, AbstractVirtualFolder> folders = new HashMap<>();
    private HashMap<String, VirtualFile> files = new HashMap<>();

    public VirtualFolder(String name){
        super(name);
    }

    public void addFolder(AbstractVirtualFolder folder){
        folders.put(folder.getName(), folder);
    }

    public void removeFolder(String folderName){
        folders.remove(folderName);
    }

    @Override
    public AbstractVirtualFolder getFolder(String folderName,  AuthorizationProfile auth){
        if(folders.containsKey(folderName)){
            return folders.get(folderName);
        }
        return null;
    }

    public void addFile(VirtualFile file){
        files.put(file.getName(), file);
    }

    public void removeFile(String fileName){
        files.remove(fileName);
    }

    @Override
    public VirtualFile getFile(String fileName,  AuthorizationProfile auth){
        if(files.containsKey(fileName)){
            return files.get(fileName);
        }
        return null;
    }
}
