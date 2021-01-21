import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

public class MatchFacilitator {

	Strategy blackStrategy;
	Strategy whiteStrategy;

	public MatchFacilitator(Strategy forBlack, Strategy forWhite)
	{
		blackStrategy = forBlack;
		whiteStrategy = forWhite;
	}

	public MatchFacilitator(Strategy sharedStrategy)
	{
		blackStrategy = sharedStrategy;
		whiteStrategy = sharedStrategy;
	}

	public void initializeGame(int boardSize, double komi)
	{
		if(blackStrategy == whiteStrategy)
			blackStrategy.initializeGame(boardSize, komi);
		else {
			blackStrategy.initializeGame(boardSize, komi);
			whiteStrategy.initializeGame(boardSize, komi);
		}
	}

	public MatchRecord facilitateGame(GameConfiguration gameConfig)
	{
		var matchRecord = new MatchRecord(gameConfig.boardSize, gameConfig.komi);

		initializeGame(gameConfig.boardSize, gameConfig.komi);

		int nextToMove = 1;
		int totalMoves = 0;
		int consecutivePasses = 0;
		boolean playerResigns = false;
		while(consecutivePasses < 2 && totalMoves < gameConfig.maxMoves && !playerResigns) {

			Optional<Integer> nextMove;
			if (nextToMove == 1) {
				nextMove = blackStrategy.getNextMove(1);
			}
			else {
				nextMove = whiteStrategy.getNextMove(-1);
			}

			if (nextMove.isEmpty()) {
				playerResigns = true;
				matchRecord.concludeInResignationBy(nextToMove);
			}
			else {
				if (nextMove.get() == gameConfig.boardArea) {
					consecutivePasses++;
				}
				else {
					consecutivePasses = 0;
				}

				matchRecord.recordMove(nextMove.get(), nextToMove);

				totalMoves ++;
				if(blackStrategy == whiteStrategy)
					blackStrategy.applyMoveForPlayer(nextMove.get(), nextToMove);
				else {
					blackStrategy.applyMoveForPlayer(nextMove.get(), nextToMove);
					whiteStrategy.applyMoveForPlayer(nextMove.get(), nextToMove);
				}

				nextToMove = -nextToMove;
			}
		}

		matchRecord.concludeGame();

		return matchRecord;
	}
}
