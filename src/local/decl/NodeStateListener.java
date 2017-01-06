package local.decl;

import net.data.Node;

public interface NodeStateListener {
    public void addNode(Node node);
    public void removeNode(Node node);
}
