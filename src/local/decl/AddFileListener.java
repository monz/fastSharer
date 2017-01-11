package local.decl;

import com.sun.istack.internal.NotNull;
import data.SharedFile;

public interface AddFileListener {
    @NotNull public void addedLocalFile(SharedFile sharedFile);
    @NotNull public void addedRemoteFile(SharedFile sharedFile);
}
