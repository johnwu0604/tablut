package student_player;

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
	private int NUM_PIECES_WEIGHTING = 2;
	private int KING_DISTANCE_WEIGHTING = 10;
	private int PIECES_TO_KING_WEIGHTING = 1;

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
    	
    	int end = 1000;
    	Node root = new Node(null, boardState, player, null);
    	root.createChildNodes();
    	
    	for (int i=0; i<root.getChildren().size(); i++) {
			if (root.getChildren().get(i).getState().getWinner() == player) {
				return root.getChildren().get(i).getLatestMove();
			} 
			if (root.getChildren().get(i).getState().getWinner() == opponent) {
				root.getChildren().remove(i);
			}
		}
    	
    	int opponentPieces = root.getState().getNumberPlayerPieces(opponent);
    	int playerPieces = root.getState().getNumberPlayerPieces(player);
    	for (int i=0; i<root.getChildren().size(); i++) {
    		if (opponentPieces-root.getChildren().get(i).getState().getNumberPlayerPieces(opponent) != 0) {
    			return root.getChildren().get(i).getLatestMove();
    		}
    		if (playerPieces-root.getChildren().get(i).getState().getNumberPlayerPieces(player) != 0) {
    			root.getChildren().remove(i);
    		}
    	}
    	
    	long startTime = System.currentTimeMillis();
    	while ( (System.currentTimeMillis()-startTime) < end) {
    		for (Node child : root.getChildren()) {
    			Node promisingNode = selectPromisingNode(child);
    			TablutBoardState playoutResult = simulateRandomPlayout(promisingNode.getState());
    			if (playoutResult.getWinner() == player) {
    				child.setWinScore(child.getWinScore()+1);
    			}
    		}
        }
        // Return your move to be processed by the server.
        return getHighestScore(root.getChildren()).getLatestMove();
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
    		int heuristic = calculateHeuristic(child.getState());
    		if (heuristic > bestHeuristic) {
    			bestNode = child;
    		}
    	}
    	return bestNode;
    }
    
    private int calculateHeuristic(TablutBoardState state) {
    	int score = 1000;
    	if (state.gameOver()) {
    		if (state.getWinner() == player) {
    			return 10000;
    		} 
    		if (state.getWinner() == opponent) {
    			return 0;
    		}
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
   
}