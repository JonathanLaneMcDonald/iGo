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

	public void initializeGame(MatchConfiguration matchConfig)
	{
		if(blackStrategy == whiteStrategy)
			blackStrategy.initializeGame(matchConfig);
		else {
			blackStrategy.initializeGame(matchConfig);
			whiteStrategy.initializeGame(matchConfig);
		}
	}

	public MatchRecord facilitateMatch(MatchConfiguration gameConfig) {
		return facilitateGame(gameConfig, false);
	}

	public MatchRecord facilitateGame(MatchConfiguration matchConfig, boolean displayBoard)
	{
		var matchRecord = new MatchRecord(matchConfig);

		initializeGame(matchConfig);

		int nextToMove = 1;
		int totalMoves = 0;
		int consecutivePasses = 0;
		boolean playerResigns = false;
		while(consecutivePasses < 2 && totalMoves < matchConfig.maxMoves && !playerResigns) {

			if(displayBoard)
				matchRecord.displayBoardForHuman();

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
				if (nextMove.get() == matchConfig.boardArea) {
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
