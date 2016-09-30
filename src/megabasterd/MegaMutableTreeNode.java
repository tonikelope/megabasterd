package megabasterd;

import java.util.HashMap;
import javax.swing.tree.DefaultMutableTreeNode;
import static megabasterd.MiscTools.formatBytes;

/**
 *
 * @author tonikelope
 */
public final class MegaMutableTreeNode extends DefaultMutableTreeNode {
    
    public MegaMutableTreeNode() {
        super();
    }
    
    public MegaMutableTreeNode(Object o) {
        super(o);
    }
    
    @Override
    public String toString() {
        
        if(userObject instanceof HashMap) {
            
            HashMap<String,Object> user_object = (HashMap<String,Object>)userObject;
        
            return user_object.get("name") + ((isLeaf() && user_object.get("size")!=null)?" ["+formatBytes((long)user_object.get("size"))+"]":"");
        
        } else if(userObject instanceof Object) {
            
            return userObject.toString();
            
        } else {
            
            return "";
        }
    }
    
}
