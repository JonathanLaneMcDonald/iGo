import java.util.ArrayList;
import java.util.stream.Collectors;

public class MatchRecord {
	/*
	a match record is really just going to keep records of what player moved where and who eventually won the game.

	so the class itself isn't going to store more than a few actual pieces of data, but it'll be able to export game data in a variety of ways...

	i'm thinking, for example, that a match record will save a format that can be sent directly to the static function in sgfparser that writes the sgf data
	then that'll basically be the entire dataset and this class will really just be here to read and write that stuff and to
	 */

	private class Move
	{
		public int move;
		public int player;

		public Move(int move, int player)
		{
			this.move = move;
			this.player = player;
		}
	}

	iGo game;

	ArrayList<Move> moveset;

	double finalScore;
	int gameOutcome;
	int simulationErrors;
	boolean gameEndedInResignation;
	boolean gamePlayedToConclusion;

	public MatchRecord(int boardSize, double komi)
	{
		game = new iGo(boardSize, komi);

		moveset = new ArrayList<>();

		finalScore = 0;
		gameOutcome = 0;
		simulationErrors = 0;
		gameEndedInResignation = false;
		gamePlayedToConclusion = false;
	}

	public void recordMove(int move, int player)
	{
		moveset.add(new Move(move, player));
		if(!game.placeStone(move, player))
			simulationErrors ++;
	}

	public void concludeGame()
	{
		if(game.getSensibleMovesForPlayer(1).isEmpty() && game.getSensibleMovesForPlayer(-1).isEmpty()) {
			gamePlayedToConclusion = true;
			finalScore = game.getSimpleTerminalScore();
			if(finalScore < 0)
				gameOutcome = -1;
			else
				gameOutcome = 1;
		}
	}

	public void concludeInResignationBy(int resigningPlayer)
	{
		gameEndedInResignation = true;
		gameOutcome = -resigningPlayer;
	}

	public int getOutcome()
	{
		return gameOutcome;
	}

	public boolean gameIsFreeOfErrorsAndPlayedToConclusion()
	{
		return (simulationErrors == 0) && gamePlayedToConclusion;
	}

	public String movesToSGF()
	{
		return SGFParser.movesToSGF(moveset.stream().map(m -> m.move).collect(Collectors.toCollection(ArrayList::new)), game.getSide());
	}
}
