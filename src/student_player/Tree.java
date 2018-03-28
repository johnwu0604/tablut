package student_player;

/**
 * Class for representing a search tree.
 * 
 * @author johnw
 *
 */
public class Tree {
	
    private Node root;
    
    /**
     * Main constructor for Tree object 
     * 
     * @param root
     */
    public Tree(Node root) {
    	this.root = root;
    }
    
    /**
     * Retrieves the root node of the tree.
     * 
     * @return
     */
    public Node getRoot() {
    	return root;
    }
}