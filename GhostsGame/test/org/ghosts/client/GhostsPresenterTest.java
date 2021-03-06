package org.ghosts.client;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.game_api.GameApi;
import org.game_api.GameApi.*;
import org.ghosts.client.GhostsLogic;
import org.ghosts.client.GhostsPresenter;
import org.ghosts.client.Piece;
import org.ghosts.client.GhostsPresenter.View;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Tests for {@link GhostPresenter}. Test plan: There are several interesting states: 
 * 1) empty state 
 * 2) white deployed but black don't deploy 
 * 3) normal move state
 * There are several interesting yourPlayerId:
 * 1) white player
 * 2) black player
 * 3) viewer
 * For each one of these states and for each yourPlayerId,
 * I will test what methods the presenters calls on the view and container.
 * In addition I will also test the interactions between the presenter and view, i.e.,
 * the view can call one of these methods:
 * 1) pieceSelectedToMove 
 * 2) pieceSelectedToDeploy 
 * 3) squareSelectedToMove 
 * 4) squareSelectedToDeploy 
 * 5) deployFinished
 */
public class GhostsPresenterTest {
	/** The class under test. */
	private GhostsPresenter ghostsPresenter;
	private final GhostsLogic ghostsLogic = new GhostsLogic();
	private View mockView;
	private Container mockContainer;

	private static final String W = "W"; // White hand
	private static final String B = "B"; // Black hand
	private final String viewerId = GameApi.VIEWER_ID;
	private final String wId = "42";
	private final String bId = "43";
	private static final String WDeployed = "WDeployed"; // white deploy key
	private static final String BDeployed = "BDeployed"; // black deploy key
	private final ImmutableList<String> playerIds = ImmutableList.of(wId, bId);
	private final ImmutableMap<String, Object> wInfo = ImmutableMap
			.<String, Object> of(PLAYER_ID, wId);
	private final ImmutableMap<String, Object> bInfo = ImmutableMap
			.<String, Object> of(PLAYER_ID, bId);
	private final ImmutableList<Map<String, Object>> playersInfo = ImmutableList
			.<Map<String, Object>> of(wInfo, bInfo);

	private static final String PLAYER_ID = "playerId";
	
	private String[] P = createP();
	private String[][] S = createS();

	private String[] createP() {
		String[] arr = new String[16];
		for (int i = 0; i < 16; i++) {
			arr[i] = "P" + i;
		}
		return arr;
	}

	private String[][] createS() {
		String[][] arr = new String[6][6];
		for (int i = 0; i < 6; i++)
			for (int j = 0; j < 6; j++)
				arr[i][j] = ("S" + i) + j;
		return arr;
	}

	/* The interesting states that I'll test. */
	private final ImmutableMap<String, Object> emptyState = ImmutableMap
			.<String, Object> of();
	
	/** Assume game board has already been initialized and 
	 *  all pieces are visible, visibility is not relevant
	 */
	private final ImmutableMap<String, Object> whiteDeployState = ImmutableMap
			.<String, Object> builder()
			.put(P[0], "WGood").put(P[1], "WGood").put(P[2], "WGood")
			.put(P[3], "WGood").put(P[4], "WEvil").put(P[5], "WEvil")
			.put(P[6], "WEvil").put(P[7], "WEvil").put(P[8], "BGood")
			.put(P[9], "BGood").put(P[10], "BGood").put(P[11], "BGood")
			.put(P[12], "BEvil").put(P[13], "BEvil").put(P[14], "BEvil")
			.put(P[15], "BEvil")
			.build();
	
	/** Assume white player has already deployed and 
	 *  all pieces are visible, visibility is not relevant
	 */
	private final ImmutableMap<String, Object> whiteDeployedBlackNotDeployed = ImmutableMap
			.<String, Object> builder()
			.put(P[0], "WGood").put(P[1], "WGood").put(P[2], "WGood")
			.put(P[3], "WGood").put(P[4], "WEvil").put(P[5], "WEvil")
			.put(P[6], "WEvil").put(P[7], "WEvil").put(P[8], "BGood")
			.put(P[9], "BGood").put(P[10], "BGood").put(P[11], "BGood")
			.put(P[12], "BEvil").put(P[13], "BEvil").put(P[14], "BEvil")
			.put(P[15], "BEvil")
			.put(S[5][1], P[0]) 
			.put(S[5][2], P[1]) 
			.put(S[5][3], P[2]) 
			.put(S[5][4], P[3]) 
			.put(S[4][1], P[4]) 
			.put(S[4][2], P[5]) 
			.put(S[4][3], P[6]) 
			.put(S[4][4], P[7]) 
			.put(WDeployed, "true")
			.build();
	
	
	/** Assume all player has already deployed and 
	 *  all pieces are visible, visibility is not relevant
	 */
	private final ImmutableMap<String, Object> beginState = ImmutableMap
			.<String, Object> builder()
			.put(P[0], "WGood").put(P[1], "WGood").put(P[2], "WGood")
			.put(P[3], "WGood").put(P[4], "WEvil").put(P[5], "WEvil")
			.put(P[6], "WEvil").put(P[7], "WEvil").put(P[8], "BGood")
			.put(P[9], "BGood").put(P[10], "BGood").put(P[11], "BGood")
			.put(P[12], "BEvil").put(P[13], "BEvil").put(P[14], "BEvil")
			.put(P[15], "BEvil")
			.put(S[5][1], P[0]) 
			.put(S[5][2], P[1]) 
			.put(S[5][3], P[2]) 
			.put(S[5][4], P[3]) 
			.put(S[4][1], P[4]) 
			.put(S[4][2], P[5]) 
			.put(S[4][3], P[6]) 
			.put(S[4][4], P[7])
			.put(S[1][1], P[8]) 
			.put(S[1][2], P[9]) 
			.put(S[1][3], P[10]) 
			.put(S[1][4], P[11]) 
			.put(S[0][1], P[12]) 
			.put(S[0][2], P[13]) 
			.put(S[0][3], P[14]) 
			.put(S[0][4], P[15]) 
			.put(WDeployed, "true")
			.put(BDeployed, "true")
			.build();
	

	@Before
	public void runBefore() {
		mockView = Mockito.mock(View.class);
		mockContainer = Mockito.mock(Container.class);
		ghostsPresenter = new GhostsPresenter(mockView, mockContainer);
		verify(mockView).setPresenter(ghostsPresenter);
	}

	@After
	public void runAfter() {
		// This will ensure I didn't forget to declare any extra interaction the
		// mocks have.
		verifyNoMoreInteractions(mockContainer);
		verifyNoMoreInteractions(mockView);
	}
/*
	@Test
	public void testEmptyStateForW() {
		ghostsPresenter.updateUI(createUpdateUI(wId, "0", emptyState));									// 0 means AI player???
		verify(mockContainer).sendMakeMove(ghostsLogic.getBoardInitialOperations(playerIds));
	}

	@Test
	public void testEmptyStateForB() {
		ghostsPresenter.updateUI(createUpdateUI(bId, "0", emptyState));
	}

	@Test
	public void testEmptyStateForViewer() {
		ghostsPresenter.updateUI(createUpdateUI(viewerId, "0", emptyState));
	}
	
	@Test
	public void testWhiteDeployStateForW() {
		UpdateUI updateUI = createUpdateUI(wId, wId, whiteDeployState);
		GhostsState ghostsState =
		        ghostsLogic.gameApiStateToGhostsState(updateUI.getState(), Color.W, playerIds);
		ghostsPresenter.updateUI(updateUI);
		List<Boolean> pieceDeployed = Lists.newArrayList();
		for (int i = 0; i < 16; i++) {								
			pieceDeployed.add(false);
		}
		verify(mockView).chooseNextPieceToDeploy(getPiecesList(ghostsState.getPieces()), ghostsState.getSquares(), ghostsState.getTurn(), pieceDeployed); 
	}
	
	@Test
	public void testWhiteDeployStateForB() {
		ghostsPresenter.updateUI(createUpdateUI(bId, wId, whiteDeployState));
	}

	@Test
	public void testWhiteDeployStateForViewer() {
		ghostsPresenter.updateUI(createUpdateUI(viewerId, wId, whiteDeployState));
	}
	
	@Test
	public void testWhiteDeployedBlackNotDeployedForB() {
		UpdateUI updateUI = createUpdateUI(bId, bId, whiteDeployedBlackNotDeployed);
		GhostsState ghostsState =
		        ghostsLogic.gameApiStateToGhostsState(updateUI.getState(), Color.B, playerIds);
		ghostsPresenter.updateUI(updateUI);
		List<Boolean> pieceDeployed = Lists.newArrayList();
		for (int i = 0; i < 16; i++) {								
			pieceDeployed.add(false);
		}
		verify(mockView).chooseNextPieceToDeploy(getPiecesList(ghostsState.getPieces()), ghostsState.getSquares(), ghostsState.getTurn(), pieceDeployed); 
	}
	
	@Test
	public void testWhiteDeployedBlackNotDeployedForW() {
		ghostsPresenter.updateUI(createUpdateUI(wId, bId, whiteDeployedBlackNotDeployed));									
	}
	
	@Test
	public void testWhiteDeployedBlackNotDeployedForViewer() {								
		ghostsPresenter.updateUI(createUpdateUI(viewerId, bId, whiteDeployedBlackNotDeployed));									
	}

	
	@Test
	public void testBeginStateForWTurnOfW() {
		UpdateUI updateUI = createUpdateUI(wId, wId, beginState);
		GhostsState ghostsState =
		        ghostsLogic.gameApiStateToGhostsState(updateUI.getState(), Color.W, playerIds);
		ghostsPresenter.updateUI(updateUI);
		List<Boolean> pieceDeployed = Lists.newArrayList();
		for (int i = 0; i < 16; i++) {								
			pieceDeployed.add(false);
		}
		verify(mockView).setPlayerState(getPiecesList(ghostsState.getPieces()), ghostsState.getSquares(), ghostsState.getTurn(), pieceDeployed);
		verify(mockView).chooseNextPieceToMove(getPiecesList(ghostsState.getPieces()), ghostsState.getSquares(), ghostsState.getTurn()); 
	}
	
	@Test
	public void testBeginStateForBTurnOfW() {
		UpdateUI updateUI = createUpdateUI(bId, wId, beginState);
		GhostsState ghostsState =
		        ghostsLogic.gameApiStateToGhostsState(updateUI.getState(), Color.W, playerIds);
		ghostsPresenter.updateUI(updateUI);
		List<Boolean> pieceDeployed = Lists.newArrayList();
		for (int i = 0; i < 16; i++) {								
			pieceDeployed.add(false);
		}
		verify(mockView).setPlayerState(getPiecesList(ghostsState.getPieces()), ghostsState.getSquares(), ghostsState.getTurn(), pieceDeployed);
	}
	
	@Test
	public void testBeginStateForViewerTurnOfW() {
		UpdateUI updateUI = createUpdateUI(viewerId, wId, beginState);
		GhostsState ghostsState =
		        ghostsLogic.gameApiStateToGhostsState(updateUI.getState(), Color.W, playerIds);
		ghostsPresenter.updateUI(updateUI);
		verify(mockView).setViewerState(ghostsState.getSquares());
	}
	
	@Test
	public void testBeginStateForWTurnOfB() {
		UpdateUI updateUI = createUpdateUI(wId, bId, beginState);
		GhostsState ghostsState =
		        ghostsLogic.gameApiStateToGhostsState(updateUI.getState(), Color.B, playerIds);
		ghostsPresenter.updateUI(updateUI);
		List<Boolean> pieceDeployed = Lists.newArrayList();
		for (int i = 0; i < 16; i++) {								
			pieceDeployed.add(false);
		}
		verify(mockView).setPlayerState(getPiecesList(ghostsState.getPieces()), ghostsState.getSquares(), ghostsState.getTurn(), pieceDeployed);
	}
	
	@Test
	public void testBeginStateForBTurnOfB() {
		UpdateUI updateUI = createUpdateUI(bId, bId, beginState);
		GhostsState ghostsState =
		        ghostsLogic.gameApiStateToGhostsState(updateUI.getState(), Color.B, playerIds);
		ghostsPresenter.updateUI(updateUI);
		List<Boolean> pieceDeployed = Lists.newArrayList();
		for (int i = 0; i < 16; i++) {								
			pieceDeployed.add(false);
		}
		verify(mockView).setPlayerState(getPiecesList(ghostsState.getPieces()), ghostsState.getSquares(), ghostsState.getTurn(), pieceDeployed);
		verify(mockView).chooseNextPieceToMove(getPiecesList(ghostsState.getPieces()), ghostsState.getSquares(), ghostsState.getTurn());
	}
	
	@Test
	public void testBeginStateForViewerTurnOfB() {
		UpdateUI updateUI = createUpdateUI(viewerId, bId, beginState);
		GhostsState ghostsState =
		        ghostsLogic.gameApiStateToGhostsState(updateUI.getState(), Color.B, playerIds);
		ghostsPresenter.updateUI(updateUI);
		verify(mockView).setViewerState(ghostsState.getSquares());
	}
	
	@Test
	public void testBeginStateForWSelectedOnePiecePrepareSelectSquare() {
		UpdateUI updateUI = createUpdateUI(wId, wId, beginState);
		GhostsState ghostsState =
		        ghostsLogic.gameApiStateToGhostsState(updateUI.getState(), Color.W, playerIds);
		ghostsPresenter.updateUI(updateUI);
		List<Piece> myPieces = getPiecesList(ghostsState.getPieces());
		ghostsPresenter.pieceSelectedToMove(myPieces.get(4));  // choose P4
		
		List<Boolean> pieceDeployed = Lists.newArrayList();
		for (int i = 0; i < 16; i++) {								
			pieceDeployed.add(false);
		}
		verify(mockView).setPlayerState(myPieces, ghostsState.getSquares(), ghostsState.getTurn(), pieceDeployed);
		verify(mockView).chooseNextPieceToMove(myPieces, ghostsState.getSquares(), ghostsState.getTurn());
		List<Position> possibleSquares = ImmutableList.<Position> of(
				new Position(3, 1),
				new Position(4, 0));
		verify(mockView).chooseSquareToMove(possibleSquares);
		
	}
	
	@Test
	public void testBeginStateForWSelectedPieceAndSquarePrepareMove() {
		UpdateUI updateUI = createUpdateUI(wId, wId, beginState);
		GhostsState ghostsState =
		        ghostsLogic.gameApiStateToGhostsState(updateUI.getState(), Color.W, playerIds);
		ghostsPresenter.updateUI(updateUI);
		List<Piece> myPieces = getPiecesList(ghostsState.getPieces());
		ghostsPresenter.pieceSelectedToMove(myPieces.get(4));  // choose P4
		ghostsPresenter.squareSelectedToMove(new Position(3, 1));
		List<Boolean> pieceDeployed = Lists.newArrayList();
		for (int i = 0; i < 16; i++) {								
			pieceDeployed.add(false);
		}
		verify(mockView).setPlayerState(myPieces, ghostsState.getSquares(), ghostsState.getTurn(), pieceDeployed);
		verify(mockView).chooseNextPieceToMove(myPieces, ghostsState.getSquares(), ghostsState.getTurn());
		List<Position> possibleSquares = ImmutableList.<Position> of(
				new Position(3, 1),
				new Position(4, 0));
		verify(mockView).chooseSquareToMove(possibleSquares);
		List<Operation> operations = Lists.newArrayList();
		operations.add(new SetTurn(bId));
		operations.add(new Set("S31", "P4"));
		operations.add(new Delete("S41"));
		verify(mockContainer).sendMakeMove(operations);
	}
	
	@Test
	public void testWhiteDeployStateForWSelectedPiecePrepareSelectSquare() {
		UpdateUI updateUI = createUpdateUI(wId, wId, whiteDeployState);
		GhostsState ghostsState =
		        ghostsLogic.gameApiStateToGhostsState(updateUI.getState(), Color.W, playerIds);
		ghostsPresenter.updateUI(updateUI);
		List<Piece> myPieces = getPiecesList(ghostsState.getPieces());
		ghostsPresenter.pieceSelectedToDeploy(myPieces.get(0));
		ghostsPresenter.squareSelectedToDeploy(new Position(5, 1));
		ghostsPresenter.pieceSelectedToDeploy(myPieces.get(1));
		ghostsPresenter.squareSelectedToDeploy(new Position(4, 1));
//		ghostsPresenter.pieceSelectedToDeploy(myPieces.get(2));
//		ghostsPresenter.squareSelectedToDeploy(new Position(4, 2));
		
		List<Boolean> pieceDeployed = ImmutableList.<Boolean> of(
				true, true, false, false, false, false, false, false,
				false, false, false, false, false, false, false, false);
		Hashtable<Position, Piece> deployTable = new Hashtable<Position, Piece>();
		deployTable.put(new Position(5, 1), myPieces.get(0));
		deployTable.put(new Position(4, 1), myPieces.get(1));
		verify(mockView, times(3)).chooseNextPieceToDeploy(myPieces, deployTable, ghostsState.getTurn(), pieceDeployed);		// not clear*********************
		List<Position> possibleSquares = ImmutableList.<Position> of(
//				new Position(4, 1),
				new Position(4, 2),
				new Position(4, 3),
				new Position(4, 4),
//				new Position(5, 1),
				new Position(5, 2),
				new Position(5, 3),
				new Position(5, 4));
		verify(mockView, times(2)).chooseSquareToDeploy(possibleSquares);				// Something wrong!!!!!!!!!!*********************

		
//		pieceDeployed.set(0, true);
//		verify(mockView).chooseNextPieceToDeploy(ImmutableList.<Piece>of(), pieceDeployed);
	}
*/
	private UpdateUI createUpdateUI(String yourPlayerId, String turnOfPlayerId,
			Map<String, Object> state) {
		// Our UI only looks at the current state
		// (we ignore: lastState, lastMovePlayerId,
		// playerIdToNumberOfTokensInPot)
		return new UpdateUI(yourPlayerId, playersInfo, state,
				emptyState, // we ignore lastState
				ImmutableList.<Operation> of(new SetTurn(turnOfPlayerId)), "0",
				ImmutableMap.<String, Integer> of());
	}
	
	/*
	 * Return a List<Piece> form of piecelist, if not visible then it's null
	 */
	private List<Piece> getPiecesList(ImmutableList<Optional<Piece>> pieces) {
		List<Piece> myPieces = Lists.newArrayList();
		for (int i = 0; i < 16; i++) {
			if (pieces.get(i).isPresent()) {
				myPieces.add(pieces.get(i).get());
			} else {
				myPieces.add(null);
			}
		}
		return myPieces;
	}
}
