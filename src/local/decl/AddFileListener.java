package local.decl;

import data.SharedFile;

public interface AddFileListener {
    public void addedLocalFile(SharedFile sharedFile);
    public void addedRemoteFile(SharedFile sharedFile);
}
