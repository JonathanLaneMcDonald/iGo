import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
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
		// mctsSelfPlayTest(7, 1000, 0, 1);

		int[] boardSizes = {7,7,7,7,8,8,8,9,9};
		datasetGeneratorTest(10000, boardSizes);
	}

	public static void datasetGeneratorTest(int gamesToPlay, int[] boardSizes)
	{
		var random = new Random();
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter("training data export test"));

			int moves = 0;
			long startTime = System.nanoTime();
			for(int game = 1; game < gamesToPlay; game++){
				var boardSize = boardSizes[random.nextInt(boardSizes.length)];
				var gameRecords = mctsSelfPlayTest(boardSize, boardSize*boardSize, boardSize*boardSize, 1);
				for(var record : gameRecords)
					writer.write(record + "\n");
				writer.flush();
				moves += gameRecords.size();

				var elapsedTime = System.nanoTime() - startTime;
				var timePerGame = elapsedTime / game;
				var timePerMove = elapsedTime / moves;

				System.out.println(game + " games played and " + moves + " moves written -- "+" Time/(m,g): ("+timePerMove+","+timePerGame+")");
			}
			writer.close();
		}
		catch(IOException e) {
			System.out.println("Conflatulations, you found a runtime error!");
		}
	}

	public static ArrayList<String> mctsSelfPlayTest(int boardSide, int rolloutsBlack, int rolloutsWhite, double expansionProbability)
	{
		var policy = new MonteCarloTreeSearch(boardSide, 6.5, 0.25);
		//policy.displayBoard();

		int consecutivePasses = 0;
		int moveNumber = 0;
		while(consecutivePasses < 2 && moveNumber < policy.area*3) {

			moveNumber ++;

			if(policy.getNextPlayerToMove() == 1)
				policy.simulate(rolloutsBlack, expansionProbability);
			else
				policy.simulate(rolloutsWhite, expansionProbability);

			//policy.displayPositionStrength();
			var strongestMove = policy.getWeightedRandomStrongestMoveFromTopK(3);
			if(strongestMove == policy.area) {
				consecutivePasses++;
			}
			else {
				consecutivePasses = 0;
			}
			policy.doMove(strongestMove);
			//policy.displayBoard();
		}

		return policy.exportTrainingDataForGame();
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
