import java.util.ArrayList;
import java.util.List;

public class Main
{
/*
model architecture:
	support a range of board sizes like in the paper, but sample sizes following a distribution like below

	inputs:
		definitely use input features like those described in the KataGo paper - the ablation study insists!

	body: (typical resnet using pre-activation and two convolutions per block)
		32x4 -> 64x6 -> 96x8 -> 128x10

	outputs:
		weights		loss		quantity
		0.40		mse			MCTS policy for the current player
		0.20		bin xent	game-end ownership (if we played until someone runs out of legal moves)
		0.40		mse			utility of the current board state

regime:
	play 10,000 games at inverse proportion to their area (like this)
		7	49	13.4693877551020	0.492462086710767	0.492462086710767
		9	81	8.14814814814815	0.297909163565772	0.790371250276539
		13	169	3.90532544378698	0.142784865377678	0.933156115654217
		19	361	1.82825484764543	0.066843884345783	1
		to play these games, use something like MC-RAVE

	exploit symmetries to augment data

	train the model above on the generated dataset

	incorporate the model into the MCTS and allow it to start generating its own training data

	devise a method for check-pointing and monitoring changes in model performance

by break's end:
	1) have a Go engine
	2) have a mcts tree search with pUCT and RAVE
	3) have a mechanism for recovering training data from MCTS (state/policy/value)

afterward:
	4) build a version of MCTS that uses a model to estimate policy/value
	5) build a simple model 64x5 with policy (described above) and value (average of entire subtree)
	6) build a training loop and let it run on the big computer to train up a model that's strong enough to challenge me (aka not very strong)
		I'd like to build a training regime strong enough to challenge me, but weak enough that I actually learn something and win sometimes ;)
 */

	public static void main(String[] args)
	{
		/*
		var test = new iGo(5);
		test.placeStone(new Move(1,1,5).index, 1);
		test.placeStone(new Move(1,0,5).index, 1);
		test.placeStone(new Move(0,1,5).index, 1);
		test.placeStone(new Move(2,0,5).index, -1);
		test.placeStone(new Move(2,1,5).index, -1);
		test.placeStone(new Move(0,2,5).index, -1);
		test.placeStone(new Move(1,2,5).index, -1);
		test.placeStone(new Move(2,2,5).index, -1);
		test.placeStone(new Move(0,0,5).index, -1);
		test.displayBoard();
		*/

		simulationSpeedTest("res\\compliant kgs games",19);
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
		int legalMoveErrors = 0;
		long startTime = System.nanoTime();
		for(var moveset : movesets)
		{
			int player = 1;
			var game = new iGo(edgeLength);
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

				//game.displayBoard(move.index);
			}

			legalMoveErrors += game.auditLegalMoves("Game Number:"+gamesPlayed+" Move Number:"+movesPlayed);

			if(gameContainsErrors)
			{
				gamesWithErrors ++;
			}

			gamesPlayed ++;
			if(gamesPlayed % 100 == 0)
			{
				var elapsedTime = System.nanoTime() - startTime;
				var timePerGame = elapsedTime / gamesPlayed;
				var timePerMove = elapsedTime / movesPlayed;
				System.out.println("Moves: "+movesPlayed+" Games: "+gamesPlayed+" Time/(m,g): ("+timePerMove+","+timePerGame+") Errors/(m,g): ("+errors+","+gamesWithErrors+")"+" Legal Move Errors: "+legalMoveErrors);
			}
		}
	}
}
