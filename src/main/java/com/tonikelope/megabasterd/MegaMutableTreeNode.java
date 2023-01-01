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

import static com.tonikelope.megabasterd.MiscTools.*;
import java.util.Comparator;
import java.util.HashMap;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

/**
 *
 * @author tonikelope
 */
public class MegaMutableTreeNode extends DefaultMutableTreeNode {

    protected long mega_node_size = 0L;

    protected Comparator nodeComparator = new Comparator() {
        @Override
        public int compare(Object o1, Object o2) {

            return MiscTools.naturalCompare(o1.toString(), o2.toString(), true);
        }

        @Override
        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        public boolean equals(Object obj) {
            return false;
        }
    };

    public void setMega_node_size(long mega_node_size) {
        this.mega_node_size = mega_node_size;
    }

    public long getMega_node_size() {
        return mega_node_size;
    }

    public MegaMutableTreeNode() {
        super();
    }

    public MegaMutableTreeNode(Object o) {
        super(o);
        this.mega_node_size = (long) ((HashMap<String, Object>) o).get("size");
    }

    @Override
    public String toString() {

        if (userObject instanceof HashMap) {

            HashMap<String, Object> user_object = (HashMap<String, Object>) userObject;

            return user_object.get("name") + " [" + formatBytes(mega_node_size) + "]";

        } else if (userObject instanceof Object) {

            return userObject.toString();

        } else {

            return "";
        }
    }

    @Override
    public Object clone() {
        return super.clone(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void insert(MutableTreeNode newChild, int childIndex) {
        super.insert(newChild, childIndex);
    }

}
