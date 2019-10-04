package com.tonikelope.megabasterd;

import java.util.HashMap;
import java.util.logging.Logger;

/**
 *
 * @author tonikelope
 */
public class MegaDirNode {

    private static final Logger LOG = Logger.getLogger(MegaDirNode.class.getName());

    private final String _node_id;

    private final HashMap<String, MegaDirNode> _children;

    public MegaDirNode(String node_id) {

        _node_id = node_id;

        _children = new HashMap<>();
    }

    public String getNode_id() {
        return _node_id;
    }

    public HashMap<String, MegaDirNode> getChildren() {
        return _children;
    }

}
