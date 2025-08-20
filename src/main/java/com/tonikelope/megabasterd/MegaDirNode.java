/*
 __  __                  _               _               _ 
|  \/  | ___  __ _  __ _| |__   __ _ ___| |_ ___ _ __ __| |
| |\/| |/ _ \/ _` |/ _` | '_ \ / _` / __| __/ _ \ '__/ _` |
| |  | |  __/ (_| | (_| | |_) | (_| \__ \ ||  __/ | | (_| |
|_|  |_|\___|\__, |\__,_|_.__/ \__,_|___/\__\___|_|  \__,_|
             |___/                                         
© Perpetrated by tonikelope since 2016
 */
package com.tonikelope.megabasterd;

import java.util.HashMap;

/**
 *
 * @author tonikelope
 */
public class MegaDirNode {

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
