package student_player;

import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import boardgame.Move;
import coordinates.Coord;
import coordinates.Coordinates;
import tablut.TablutBoardState;
import tablut.TablutMove;
import tablut.TablutPlayer;

/** A player file submitted by a student. */
public class StudentPlayer extends TablutPlayer {
	
	private int level;
	private int opponent;
	private int player;
	private final int NUM_PIECES_WEIGHTING = 2;
	private final int KING_DISTANCE_WEIGHTING = 50;
	private final int PIECES_TO_KING_WEIGHTING = 1;
	private final int MONTE_CARLO_LIMIT = 1500;

    /**
     * You must modify this constructor to return your student number. This is
     * important, because this is what the code that runs the competition uses to
     * associate you with your agent. The constructor should do nothing else.
     */
    public StudentPlayer() {
        super("260612056");
    }

    /**
     * This is the primary method that you need to implement. The ``boardState``
     * object contains the current state of the game, which your agent must use to
     * make decisions.
     */
    public Move chooseMove(TablutBoardState boardState) {
    	
    	// set player and opponent ids
    	if (player_id == TablutBoardState.SWEDE) {
    		player = TablutBoardState.SWEDE;
    		opponent = TablutBoardState.MUSCOVITE;
    	} else {
    		player = TablutBoardState.MUSCOVITE;
    		opponent = TablutBoardState.SWEDE;
    	}
    	
    	// create the root node and retrieve its children
    	Node root = new Node(null, boardState, null);
    	root.createChildNodes();
    	List<Node> children = root.getChildren(); 
    	
    	// if there is a winning move, make it
    	Move winningMove = getWinningMove(children);
    	if (winningMove != null) {
    		return winningMove;
    	}
    	
    	// if there is a greedy move to get king closer to corner, make it
    	if (player == TablutBoardState.SWEDE) {
    		Move greedyMove = getGreedyMove(root);
        	if (greedyMove != null) {
        		return greedyMove;
        	}
    	}
    	
    	// if there is an obvious good move, make it
    	Move obviousMove = getObviousMove(children, root);
    	if (obviousMove != null) {
    		return obviousMove;
    	}
    	
    	// if obvious/greedy moves don't exist then run monte carlo simulation
    	long startTime = System.currentTimeMillis();
    	while ( (System.currentTimeMillis()-startTime) < MONTE_CARLO_LIMIT) {
    		for (Node child : children) {
    			Node promisingNode = selectPromisingNode(child);
    			TablutBoardState playoutResult = simulateRandomPlayout(promisingNode.getState());
    			if (playoutResult.getWinner() == player) {
    				child.setWinScore(child.getWinScore()+1);
    			}
    		}
        }
    	
        // return highest scoring move from monte carlo simulation
        return getHighestScore(children).getLatestMove();
    }
    
    /**
     * Retrieve the highest scoring Node out of all the children in random playout
     * 
     * @param children
     * @return Node
     */
    private Node getHighestScore(List<Node> children) {
    	Node highestScore = children.get(0);
    	for (Node child: children) {
    		if (child.getWinScore() > highestScore.getWinScore()) {
    			highestScore = child;
    		}
    	}
    	return highestScore;
    }
    
    /**
     * Select the most promising child node based on heuristic
     * 
     * @param rootNode
     * @return Node
     */
    private Node selectPromisingNode(Node rootNode) {
    	Node node = rootNode;
    	node.createChildNodes();
    	int bestHeuristic = -1;
    	Node bestNode = null;
    	for (Node child: rootNode.getChildren()) {
    		int heuristic = calculateHeuristic(child);
    		if (heuristic > bestHeuristic) {
    			bestNode = child;
    		}
    	}
    	return bestNode;
    }
    
    /**
     * Calculate heuristic based on state of the board
     * 
     * @param child
     * @return int - heuristic calculation
     */
    private int calculateHeuristic(Node child) {
    	TablutBoardState state = child.getState();
    	// trivial heuristic
    	if (state.gameOver()) {
    		if (state.getWinner() == player) {
    			return 10000;
    		} 
    		if (state.getWinner() == opponent) {
    			return 0;
    		}
    	}
    	// start with initial score of 1000
    	int score = 1000;
    	// get the king position
    	Coord king = state.getKingPosition(); 
    	// gain points for remaining pieces, lose points for opponents remaining pieces
    	HashSet<Coord> playerPieces = state.getPlayerPieceCoordinates();
    	HashSet<Coord> opponentPieces = state.getOpponentPieceCoordinates();
    	score += NUM_PIECES_WEIGHTING * playerPieces.size();
    	score -= NUM_PIECES_WEIGHTING * opponentPieces.size();
    	// gain points for pieces being close to king, lose points for opponent being close to king
    	for (Coord coord: state.getPlayerPieceCoordinates()) {
			score -= PIECES_TO_KING_WEIGHTING * coord.distance(king);
		}
		for (Coord coord: state.getOpponentPieceCoordinates()) {
			score += PIECES_TO_KING_WEIGHTING * coord.distance(king);
		}
		// gain/lose points for king being close to corner
    	if (player == TablutBoardState.SWEDE) {
    		score -= KING_DISTANCE_WEIGHTING * Coordinates.distanceToClosestCorner(king);

    	} else {
    		score += KING_DISTANCE_WEIGHTING * Coordinates.distanceToClosestCorner(king);
    	}
    	return score;
    }
    
    /**
     * Simulate a random play out until game ends
     * @param state
     * @return TablutBoardState - ending state of the game
     */
    private TablutBoardState simulateRandomPlayout(TablutBoardState state) {
    	TablutBoardState tempState = (TablutBoardState) state.clone();
        if (tempState.gameOver()) {
            return state;
        }
        while (!tempState.gameOver()) {
        	Random rand = new Random();
        	List<TablutMove> moves = tempState.getAllLegalMoves();
        	tempState.processMove(moves.get(rand.nextInt(moves.size())));
        }
        return tempState;
    }
    
    /**
     * Returns the move that leads to a capture if it exists
     * 
     * @param children
     * @param parent
     * @return Move - or null if it doesn't exist
     */
    private Move getObviousMove(List<Node> children, Node parent) {
    	int previousOpponentPieces = parent.getState().getNumberPlayerPieces(opponent);
    	for (Node child: children) {
    		int newOpponentPieces = child.getState().getNumberPlayerPieces(opponent);
    		if (previousOpponentPieces-newOpponentPieces != 0) {
    			return child.getLatestMove();
    		}
    	}
    	return null;
    }
    
    /**
     * Returns the winning move if it exists
     * 
     * @param children
     * @param parent
     * @return Move - or null if it doesn't exist
     */
    private Move getWinningMove(List<Node> children) {
    	for (Node child: children) {
    		TablutBoardState state = child.getState();
    		if (state.getWinner() == player) {
    			return child.getLatestMove();
    		}
    	}
    	return null;
    }
    
    /**
     * Determine if greedy move of moving king closer to corner is available
     * or moving closer to king if on defense
     * 
     * @param children
     * @return Move - or null if it doesn't exist
     */
    private Move getGreedyMove(Node parent) {
    	Move bestMove = null;
    	Coord king = parent.getState().getKingPosition();
    	int minDistance = Coordinates.distanceToClosestCorner(king);
        for (TablutMove move : parent.getState().getLegalMovesForPosition(king)) {
            int moveDistance = Coordinates.distanceToClosestCorner(move.getEndPosition());
            if (moveDistance < minDistance) {
            	TablutBoardState childState = (TablutBoardState) parent.getState().clone();
                childState.processMove(move);
            	if (isMoveSafe(childState)) {
            		minDistance = moveDistance;
                    bestMove = move;
            	}
            }
        }
        return bestMove;
    }
    
    /**
     * Determines if a state is safe for at least one turn cycle
     * 
     * @param parent
     * @return boolean 
     */
    private boolean isMoveSafe(TablutBoardState state) {
    	int originalNumPieces = state.getNumberPlayerPieces(player);
    	for (TablutMove move: state.getAllLegalMoves()) {
    		TablutBoardState childState = (TablutBoardState) state.clone();
            childState.processMove(move);
    		int newNumPieces = childState.getNumberPlayerPieces(player);
    		if (originalNumPieces-newNumPieces != 0) {
    			return false;
    		}
    	}
    	return true;
    }
   
}