package com.tonikelope.megabasterd;

import static com.tonikelope.megabasterd.MiscTools.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.logging.Logger;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

/**
 *
 * @author tonikelope
 */
public class MegaMutableTreeNode extends DefaultMutableTreeNode {

    private static final Logger LOG = Logger.getLogger(MegaMutableTreeNode.class.getName());
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

    public MegaMutableTreeNode() {
        super();
    }

    public MegaMutableTreeNode(Object o) {
        super(o);
    }

    @Override
    public String toString() {

        if (userObject instanceof HashMap) {

            HashMap<String, Object> user_object = (HashMap<String, Object>) userObject;

            return user_object.get("name") + ((isLeaf() && user_object.get("size") != null) ? " [" + formatBytes((long) user_object.get("size")) + "]" : "");

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
        Collections.sort(this.children, nodeComparator);
    }

}
