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
	private final int MONTE_CARLO_LIMIT = 1000;

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
    	
    	// remove any obvious bad moves
    	//children = removeBadMoves(children);
    	
    	// if there is an obvious good move, make it
    	Move obviousMove = getObviousMove(children, root);
    	if (obviousMove != null) {
    		return obviousMove;
    	}
    	
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
        // Return your move to be processed by the server.
        return getHighestScore(children).getLatestMove();
    }
    
    private Node getHighestScore(List<Node> children) {
    	Node highestScore = children.get(0);
    	for (Node child: children) {
    		if (child.getWinScore() > highestScore.getWinScore()) {
    			highestScore = child;
    		}
    	}
    	return highestScore;
    }
    
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
    
    private int calculateHeuristic(Node child) {
    	TablutBoardState state = child.getState();
    	if (state.gameOver()) {
    		if (state.getWinner() == player) {
    			return 10000;
    		} 
    		if (state.getWinner() == opponent) {
    			return 0;
    		}
    	}
    	//int score = 1000;
    	int score = greedyHeuristic(child);
    	if (score == 0) {
    		return 0;
    	}
    	Coord king = state.getKingPosition(); 
    	HashSet<Coord> playerPieces = state.getPlayerPieceCoordinates();
    	HashSet<Coord> opponentPieces = state.getOpponentPieceCoordinates();
    	score += playerPieces.size();
    	score -= opponentPieces.size();
    	if (player == TablutBoardState.SWEDE) {
    		score -= KING_DISTANCE_WEIGHTING * Coordinates.distanceToClosestCorner(king);
    	} else {
    		score += KING_DISTANCE_WEIGHTING * Coordinates.distanceToClosestCorner(king);
    	}
    	return score;
    }
    
    private void expandNode(Node node) {
        node.createChildNodes();
    }
    
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
     * Determines the baseline heuristic purely based on greed
     * 
     * @return A heuristic based on how severe it is
     */
    private int greedyHeuristic(Node node) {
    	node.createChildNodes();
    	List<Node> children = node.getChildren();
    	int previousNumPieces = node.getState().getNumberPlayerPieces(player);
    	for (Node child: children) {
			TablutBoardState state = child.getState();
			int newNumPieces = child.getState().getNumberPlayerPieces(player);
			if (state.gameOver()) {
    			if (state.getWinner() == opponent) {
    				return 0;
    			}
    		} else {
    			if (previousNumPieces-newNumPieces == 0) {
    				return 1000;
    			}
    		}
		}
    	return 100;
    }
    
    /**
     * Returns the move that leads to a capture if it exists
     * 
     * @param children
     * @param parent
     * @return
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
     * @return
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
   
}