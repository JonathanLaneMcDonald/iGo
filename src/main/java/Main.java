import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main
{

	public static void main(String[] args)
	{
		//loadModelTest();

		datasetGeneratorTest(16384, 100, 100, new BoardSizeDistribution(new int[]{7,8,9}, true));

		var gameConfig = new MatchConfiguration(5, 2.5);
		var blackSupplier = new StrategySupplier(StrategySupplier.StrategyType.VanillaMCTS, gameConfig, 50);
		var whiteSupplier = new StrategySupplier(StrategySupplier.StrategyType.VanillaMCTS, gameConfig, 50);
		var orchestrator = new MultiMatchOrchestrator(blackSupplier, whiteSupplier, gameConfig);

		orchestrator.playNGames(10);
		for(var string : orchestrator.getSGFRecords())
			System.out.println(string);
	}

	public static void loadModelTest()
	{
		var model = InferenceModel.getModel(32, 4, new InputShape(9, 9, 4));
		var auto = model.summary();
	}

	public static void datasetGeneratorTest(int gamesToPlay, int blackRollouts, int whiteRollouts, BoardSizeDistribution bsd)
	{
		try {
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
			LocalDateTime now = LocalDateTime.now();

			BufferedWriter writer = new BufferedWriter(new FileWriter("self-play data "+dtf.format(now)));

			int moves = 0;
			long startTime = System.nanoTime();
			for(int game = 1; game < gamesToPlay; game++){
				var boardSize = bsd.getBoardSize();
				var blackStrategy = new VanillaTreeSearchStrategy(boardSize, 6.5, blackRollouts);
				var whiteStrategy = new VanillaTreeSearchStrategy(boardSize, 6.5, whiteRollouts);
				var matchFacilitator = new MatchFacilitator(blackStrategy, whiteStrategy);
				var result = matchFacilitator.facilitateGame(new MatchConfiguration(boardSize, 6.5));

				writer.write(result.movesToSGF() + "\n");
				writer.flush();
				moves += result.getCountOfMovesPlayed();

				var elapsedTime = System.nanoTime() - startTime;
				var timePerGame = elapsedTime / game;
				var timePerMove = elapsedTime / moves;

				System.out.println("board size " + boardSize + " " + game + " games played and " + moves + " moves written -- "+" Time/(m,g): ("+timePerMove+","+timePerGame+")");
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
		boolean playerResignation = false;
		while(consecutivePasses < 2 && moveNumber < policy.area*3 && !playerResignation) {

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

			if(!policy.nextPlayerChanceToWinExceeds(20, 0.05))
				playerResignation = true;
			else
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
