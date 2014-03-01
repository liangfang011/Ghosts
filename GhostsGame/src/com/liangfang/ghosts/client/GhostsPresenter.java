package com.liangfang.ghosts.client;

import java.util.List;
import java.util.Map;

import com.liangfang.ghosts.client.GameApi.Container;
import com.liangfang.ghosts.client.GameApi.Operation;
import com.liangfang.ghosts.client.GameApi.Set;
import com.liangfang.ghosts.client.GameApi.SetTurn;
import com.liangfang.ghosts.client.GameApi.UpdateUI;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class GhostsPresenter {

	public interface View {
		/**
		 * Sets the presenter. The viewer will call certain methods on the
		 * presenter.
		 * 
		 * The process of making a move looks as follows to the viewer: 
		 * 1) The viewer calls {@link #pieceSelectedToMove} to select one piece to move 
		 * 2) The viewer calls {@link #squareSelectedToMove} to pass the selected square to move, which sends the move.
		 * 
		 * The process of making a move looks as follows to the presenter: 
		 * 1) The presenter calls {@link #chooseNextPieceToMove} and passes the piece.
		 * 2) The presenter  calls {@link #chooseNextSquareToMove} and passes the position.
		 * 
		 * The process of deploying a piece looks as follows to the viewer: 
		 * 1) The viewer calls {@link #pieceSelectedToDeploy} to select one piece to move 
		 * 2) The viewer calls {@link #squareSelectedToDeploy} to pass the selected square to move, which sends the move.
		 * 3) The viewer calls {@link #deployFinished} to send make move
		 * 
		 * The process of deploying a piece looks as follows to the presenter: 
		 * 1) The presenter calls {@link #chooseNextPieceToDeploy} and passes the piece.
		 * 2) The presenter  calls {@link #chooseNextSquareToDeploy} and passes the position.
		 */
		void setPresenter(GhostsPresenter ghostsPresenter);

		/** Sets the state for a viewer, i.e., not one of the players. */
		void setViewerState(Map<Position, String> squares);																		

		/**
		 * Sets the state for a player (whether the player has the turn or not).
		 * pass only this player's own piece and entire squares
		 */
		void setPlayerState(List<Piece> pieces, Map<Position, String> squares);

		/**
		 * Asks the player to choose the next piece to move. We pass what piece
		 * are selected, and what pieces will remain in the player hands. The
		 * user can select a piece (by calling {@link #pieceSelectedToMove) If the
		 * user selects a piece which is already in selectedPieces, then it
		 * moves that piece to remainingPieces. If the user selects a piece from
		 * remainingPieces, then selectedPiece clear and add this new piece.
		 * selectedPieces can only have no more than one piece
		 */
		void chooseNextPieceToMove(List<Piece> selectedPiece,
				List<Piece> remainingPiece);

		/**
		 * After the player finished selecting a piece, the player needs to
		 * choose the square to move.
		 */
		void chooseSquareToMove(List<Position> possiblePositions);
		
		/**
		 * Asks the player to choose a piece to deploy
		 */
		void chooseNextPieceToDeploy(List<Piece> seletedPieceToDeploy,
				List<Boolean> pieceDeployed);
		
		/**
		 * After the player finished deploy(by calling {@link #pieceSelectedToDeploy})
		 */
		void chooseSquareToDeploy(List<Position> possiblePositions);
	}

	private final GhostsLogic ghostsLogic = new GhostsLogic();
	private final View view;
	private final Container container;
	/** A viewer doesn't have a color. */
	private Optional<Color> myColor;
	private GhostsState ghostsState;
	private List<Piece> selectedPieceToMove;
	private List<Piece> selectedPieceToDeploy;
	private List<Boolean> pieceDeployed = Lists.newArrayList();
	private List<Operation> deployOperations = Lists.newArrayList();			// Store deploy operations "Set(Sxx, Pxx)"

	
	public GhostsPresenter(View view, Container container) {
		this.view = view;
		this.container = container;
		view.setPresenter(this);
	}

	/** Updates the presenter and the view with the state in updateUI. */
	public void updateUI(UpdateUI updateUI) {
		List<Integer> playerIds = updateUI.getPlayerIds();
	    int yourPlayerId = updateUI.getYourPlayerId();
	    int yourPlayerIndex = updateUI.getPlayerIndex(yourPlayerId);
	    myColor = yourPlayerIndex == 0 ? Optional.of(Color.W)
	        : yourPlayerIndex == 1 ? Optional.of(Color.B) : Optional.<Color>absent();
	    selectedPieceToMove = Lists.newArrayList();
	    selectedPieceToDeploy = Lists.newArrayList();
//	    selectedPieceToDeploy = Lists.newArrayList();
//		pieceDeployed = Lists.newArrayList();
//		deployOperations = Lists.newArrayList();
	    for (int i = 0; i < 16; i++) {								// Initialize pieceDeployed
			pieceDeployed.add(false);
		}
	    
	    if (updateUI.getState().isEmpty()) {						// Game board intialization
	        
	        if (myColor.isPresent() && myColor.get().isWhite()) {
	        	sendBoardInitialMove(playerIds);
	        }
	        return;
	    }
	    Color turnOfColor = null;
	    for (Operation operation : updateUI.getLastMove()) {
	    	if (operation instanceof SetTurn) {
	    		turnOfColor = Color.values()[playerIds.indexOf(((SetTurn) operation).getPlayerId())];
	      }
	    }	    
	    ghostsState = ghostsLogic.gameApiStateToGhostsState(updateUI.getState(), turnOfColor, playerIds);
	    
	    if (!ghostsState.isWhiteDeployed()) {						// The W player initialize board and deploy
	    	if (myColor.isPresent() && myColor.get().isWhite()) {
	        	chooseNextPieceToDeploy();
	        }
	        return;
	        
	    } else if (!ghostsState.isBlackDeployed()) {				// The B player start to deploy
	    	if (myColor.isPresent() && myColor.get().isBlack()) {
	        	chooseNextPieceToDeploy();
	        }
	        return;
	    }
	    
	    if (updateUI.isViewer()) {
	    	view.setViewerState(ghostsState.getSquares());
	        return;
	    }
	    if (updateUI.isAiPlayer()) {
	        // TODO: implement AI in a later HW!
	        //container.sendMakeMove(..);
	        return;
	    }
	    view.setPlayerState(getMyPieces(), ghostsState.getSquares());
	    
	    if (isMyTurn()) {
	    	chooseNextPieceToMove();
	    }
	}

	private void chooseNextPieceToDeploy() {
//		System.out.println(selectedPieceToDeploy + " " + pieceDeployed);							mock treat this call as same one
		view.chooseNextPieceToDeploy(Lists.newArrayList(selectedPieceToDeploy), pieceDeployed);
	}
	
	private void chooseNextPieceToMove() {
		view.chooseNextPieceToMove(Lists.newArrayList(selectedPieceToMove),
				ghostsLogic.subtract(getMyPieces(), selectedPieceToMove));
	}

	/**
	 * Adds/remove the piece from the {@link #selectedPieceToMove}, only one piece can be selected. The view can only
	 * call this method if the presenter called {@link View#chooseNextPiece}.
	 */
	public void pieceSelectedToMove(Piece piece) {			 
		check(isMyTurn());
		if (myColor.get().isWhite()) {								
			check(piece.isWhitePiece());
		} else {
			check(piece.isBlackPiece());
		}
		if (selectedPieceToMove.contains(piece)) {
			selectedPieceToMove.remove(piece);
		} else if (!selectedPieceToMove.contains(piece) && selectedPieceToMove.size() < 1) {
			selectedPieceToMove.add(piece);
		}
		check(!selectedPieceToMove.isEmpty());		// If already choose a piece, then can choose where to move
		view.chooseSquareToMove(getPossiblePositionsToMove());						
	}

	/**
	 * Selects a destination square and sends a move. The view can only call this method if
	 * the presenter called {@link View#chooseSquareToMove}.
	 */
	public void squareSelectedToMove(Position endPosition) {
		check(isMyTurn() && !selectedPieceToMove.isEmpty()
				&& getPossiblePositionsToMove().contains(endPosition));
		Piece p = selectedPieceToMove.get(0);
		String movingPiece, startSquare;
		movingPiece = p.getPieceName();
		startSquare = getSquarePositionFromPieceName(movingPiece).toSquareString();
		
		container.sendMakeMove(ghostsLogic.getMove(movingPiece, startSquare, 
								endPosition.toSquareString(), ghostsState));
	}
	
	
	public void pieceSelectedToDeploy(Piece piece) {
		check(isMyTurn());
		if (myColor.get().isWhite()) {								// Maybe we need to check if choose a piece of right color
			check(piece.isWhitePiece());
		} else {
			check(piece.isBlackPiece());
		}
		
		if (selectedPieceToDeploy.contains(piece)) {
			selectedPieceToDeploy.remove(piece);
		} else if (!selectedPieceToDeploy.contains(piece) && selectedPieceToDeploy.size() < 1) {
			selectedPieceToDeploy.add(piece);
		}
		check(!selectedPieceToDeploy.isEmpty());		// If already choose a piece, then can choose where to deploy
//		System.out.println(getPossiblePositionsToDeploy());
		view.chooseSquareToDeploy(getPossiblePositionsToDeploy());
	}
	
	public void squareSelectedToDeploy(Position deployPosition) {
		check(isMyTurn() && !selectedPieceToDeploy.isEmpty()
				&& getPossiblePositionsToDeploy().contains(deployPosition));
		Piece p = selectedPieceToDeploy.get(0);
		deployOperations.add(new Set(deployPosition.toSquareString(), p.getPieceName()));
//		System.out.println(ghostsLogic.getIndexFromPieceName(p.getPieceName()));
		pieceDeployed.set(ghostsLogic.getIndexFromPieceName(p.getPieceName()), true);
		selectedPieceToDeploy.clear();
		chooseNextPieceToDeploy();
	}
	
	public void deployFinished() {
		check(isMyTurn());
		List<Operation> operations = Lists.newArrayList();
		if (myColor.get().isWhite()) {
			for (int i = 0; i < 8; i++) {
				check(pieceDeployed.get(i));		//check if all pieces are deployed
			}
			operations.add(new SetTurn(ghostsState.getPlayerId(myColor.get().getOpposite())));			
		} else {
			for (int i = 8; i < 16; i++) {
				check(pieceDeployed.get(i));		//check if all pieces are deployed
			}
			operations.add(new SetTurn(ghostsState.getPlayerId(myColor.get().getOpposite())));					
		}
		
		check(!deployOperations.isEmpty());
		for (int i = 0; i < deployOperations.size(); i++) {
			operations.add(deployOperations.get(i));															
		}
		
		if (myColor.get().isWhite())
			operations.add(new Set("WDeployed", "true"));
		else
			operations.add(new Set("BDeployed", "true"));
		container.sendMakeMove(operations);
	}
	
	
	/**
	 * Return possible position to move, each position is
	 * 1) inside game board 
	 * 2) not occupied by piece in same color
	 * Position scan order: down, up, left, right
	 */
	private List<Position> getPossiblePositionsToMove() {		
		Piece p = selectedPieceToMove.get(0);
		String movingPiece;
		movingPiece = p.getPieceName();										
		Position origin = getSquarePositionFromPieceName(movingPiece);
		int row = origin.getRow();
		int col = origin.getCol();
		List<Position> possiblePositions = Lists.newArrayList();
		List<Position> list = Lists.newArrayList();
		list.add(new Position(row - 1, col));
		list.add(new Position(row + 1, col));
		list.add(new Position(row, col - 1));
		list.add(new Position(row, col + 1));
		for (Position pos : list) {
			if (isInsideBoard(pos) && !ghostsLogic.sameSideCapture(pos.toSquareString(), ghostsState)) {
				possiblePositions.add(pos);
			}
		}
		return possiblePositions;
	}
	
	/**
	 * Return possible position to deploy, each position is
	 * 1) on the right side
	 * 2) not already deployed by a piece
	 */
	private List<Position> getPossiblePositionsToDeploy() {
		List<Position> possiblePositions = Lists.newArrayList();
		Map<Position, String> squares = ghostsState.getSquares();
		if (myColor.get().isWhite()) {					// check white side
			for (int i = 0; i < 2; i++) {
				for (int j = 1; j < 5; j++) {
					Position pos = new Position(i, j);
					if (squares.get(pos) == null) {		// null means not deployed by others
						possiblePositions.add(pos);
					}
				}
			}
		} else {										// check black side
			for (int i = 4; i < 6; i++) {
				for (int j = 1; j < 5; j++) {
					Position pos = new Position(i, j);
					if (squares.get(pos) == null) {		// null means not deployed by others
						possiblePositions.add(pos);
					}
				}
			}
		}
		return possiblePositions;
	}
	
	private boolean isInsideBoard(Position p) {
		int row = p.getRow();
		int col = p.getCol();
		return row >= 0 && row <= 5 && col >= 0 && col <= 5;
	}
	
	private boolean isMyTurn() {
	    return myColor.isPresent() && myColor.get() == ghostsState.getTurn();
	}

	private Position getSquarePositionFromPieceName(String piece) {			// Must can return the square string from name of selected piece
		Map<Position, String> squares = ghostsState.getSquares();
		for (Position p : squares.keySet()) {
			if (squares.get(p) != null) {
				if (squares.get(p).compareTo(piece) == 0)
					return p;
			}
		}
		return null;
	}
	
	private List<Piece> getMyPieces() {
		List<Piece> myPieces = Lists.newArrayList();
		ImmutableList<Optional<Piece>> pieces = ghostsState.getPieces();
		if (myColor.get().isBlack()) {
			for (int i = 8; i < 16; i++) {
				if (pieces.get(i).isPresent()) {
					myPieces.add(pieces.get(i).get());
				}
			}
		} else {
			for (int i = 0; i < 8; i++) {
				if (pieces.get(i).isPresent()) {
					myPieces.add(pieces.get(i).get());
				}
			}
		}
		return myPieces;
	}
	
	private void check(boolean val) {
		if (!val) {
			throw new IllegalArgumentException();
		}
	}
	
	private void sendBoardInitialMove(List<Integer> playerIds) {
		container.sendMakeMove(ghostsLogic.getBoardInitialOperations(playerIds));
	}
}
