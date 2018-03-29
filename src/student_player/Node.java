package student_player;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import boardgame.Board;
import boardgame.Move;
import tablut.TablutBoardState;
import tablut.TablutMove;

/**
 * This class represents a node used in our search tree
 * 
 * @author johnw
 *
 */
public class Node {
	TablutBoardState state;
    List<Node> children;
    int winScore = 0;
    Move latestMove;
    
    /**
     * Main constructor for Node object
     * 
     * @param parent
     * @param state
     */
    public Node(Node parent, TablutBoardState state, Move move) {
    	this.state = state;
    	this.latestMove = move;
    }
    
    /**
     * Creates the children nodes and adds them to the children list
     */
    public void createChildNodes() {
    	children = new ArrayList<Node>();
    	List<TablutMove> options = state.getAllLegalMoves();
    	for (TablutMove move : options) {
            TablutBoardState childState = (TablutBoardState) state.clone();
            childState.processMove(move);
            Node childNode = new Node(this, childState, move);
            children.add(childNode);
        }
    }
    
    /**
     * Retrieves all the children nodes.
     * 
     * @return
     */
    public List<Node> getChildren() {
    	return children;
    }
    
    
    /**
     * Retrieves the state of the node.
     * 
     * @return
     */
    public TablutBoardState getState() {
    	return state;
    }
    
    /**
     * Returns a random child node to explore.
     * @return
     */
    public Node getRandomChildNode() {
    	Random rand = new Random();
    	int random = rand.nextInt(children.size());
    	return children.get(random);
    }
    
    /**
     * Gets the win score of the node
     * 
     * @return
     */
    public int getWinScore() {
    	return winScore;
    }
    
    /**
     * Sets the win score of the node.
     * 
     * @param score
     */
    public void setWinScore(int score) {
    	this.winScore = score;
    }
    
    /**
     *  Return the latest move
     * @return
     */
    public Move getLatestMove() {
    	return latestMove;
    }
    
}