import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main
{

	public static void main(String[] args)
	{
		//simulationSpeedTest("res\\compliant kgs games",19);

		//randomSamplerTest(100000, 9);

		// some have included dirichlet noise to the PUCT function, too, so maybe add a term for that
		// https://stats.stackexchange.com/questions/322831/purpose-of-dirichlet-noise-in-the-alphazero-paper
		mctsSelfPlayTest(7, 10000, 1);

		//datasetGeneratorTest(5, 100);
	}

	public static void datasetGeneratorTest(int boardSize, int rollouts)
	{
		int moves = 0;
		for(int game = 0; game < 100; game++){
			moves += mctsSelfPlayTest(boardSize, rollouts, 1);
			System.out.println(game + " games and " + moves + " moves");
		}
	}

	public static int mctsSelfPlayTest(int boardSide, int rollouts, double expansionProbability)
	{
		var policy = new MonteCarloTreeSearch(boardSide, 6.5, 0.25);
		policy.displayBoard();

		int consecutivePasses = 0;
		int moveNumber = 0;
		while(consecutivePasses < 2 && moveNumber < policy.area*3) {

			moveNumber ++;

			policy.simulate(rollouts, expansionProbability);

			policy.displayPositionStrength();
			var strongestMove = policy.getWeightedRandomStrongestMoveFromTopK(3);
			if(strongestMove == policy.area) {
				consecutivePasses++;
			}
			else {
				consecutivePasses = 0;
			}
			policy.doMove(strongestMove);
			policy.displayBoard();
		}
		return moveNumber;
	}

	private static void randomSamplerTest(int gamesToPlay, int edgeLength)
	{
		int errors = 0;
		int gamesPlayed = 0;
		int movesPlayed = 0;
		int gamesWithErrors = 0;
		long startTime = System.nanoTime();
		while(gamesPlayed < gamesToPlay)
		{
			int player = 1;
			var game = new iGo(edgeLength, 6.5);
			boolean gameContainsErrors = false;

			var random = new Random();
			int consecutivePasses = 0;
			while(consecutivePasses < 2)
			{
				//var legalMoves = game.getMovesLegalForBothPlayers();
				var sensibleMoves = game.getSensibleMovesForPlayer(player);
				if(sensibleMoves.isEmpty())
					consecutivePasses ++;
				else
				{
					movesPlayed ++;
					consecutivePasses = 0;

					var mv = sensibleMoves.get(random.nextInt(sensibleMoves.size()));
					var success = game.placeStone(mv, player);
					if(!success)
					{
						errors ++;
						gameContainsErrors = true;
					}
					player = -player;
				}
			}

			//System.out.println(game.getSimpleTerminalScore());
			//game.display(new HashSet<>());
			//game.displayLiberties(new HashSet<>());

			if(gameContainsErrors)
			{
				gamesWithErrors ++;
			}

			gamesPlayed ++;
			if(gamesPlayed % 1000 == 0)
			{
				var elapsedTime = System.nanoTime() - startTime;
				var timePerGame = elapsedTime / gamesPlayed;
				var timePerMove = elapsedTime / movesPlayed;
				System.out.println("Moves: "+movesPlayed+" Games: "+gamesPlayed+" ("+movesPlayed/gamesPlayed+"mpg) Time/(m,g): ("+timePerMove+","+timePerGame+") Errors/(m,g): ("+errors+","+gamesWithErrors+")");
			}
		}
	}

	private static void simulationSpeedTest(String sgfFileLocation, int edgeLength)
	{
		var parser = new SGFParser(sgfFileLocation, edgeLength);
		var movesets = new ArrayList<List<Move>>();
		while(!parser.endOfFile())
		{
			var moveset = parser.getNextMoveSet();
			if(!moveset.isEmpty())
				movesets.add(moveset);
		}

		int errors = 0;
		int gamesPlayed = 0;
		int movesPlayed = 0;
		int gamesWithErrors = 0;
		long startTime = System.nanoTime();
		for(var moveset : movesets)
		{
			int player = 1;
			var game = new iGo(edgeLength, 6.5);
			boolean gameContainsErrors = false;

			for(var move : moveset)
			{
				movesPlayed ++;
				if(!game.placeStone(move.index, player))
				{
					errors ++;
					gameContainsErrors = true;
				}
				player = -player;
			}

			if(gameContainsErrors)
			{
				gamesWithErrors ++;
			}

			gamesPlayed ++;
			if(gamesPlayed % 1000 == 0)
			{
				var elapsedTime = System.nanoTime() - startTime;
				var timePerGame = elapsedTime / gamesPlayed;
				var timePerMove = elapsedTime / movesPlayed;
				System.out.println("Moves: "+movesPlayed+" Games: "+gamesPlayed+" Time/(m,g): ("+timePerMove+","+timePerGame+") Errors/(m,g): ("+errors+","+gamesWithErrors+")");
			}
		}
	}
}
