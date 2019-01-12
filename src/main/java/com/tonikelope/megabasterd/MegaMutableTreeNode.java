package com.tonikelope.megabasterd;

import static com.tonikelope.megabasterd.MiscTools.*;
import java.util.HashMap;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * @author tonikelope
 */
public class MegaMutableTreeNode extends DefaultMutableTreeNode {

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

}
