import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

public class TeachableMoments {

	/*public static INDArray fromBufferReturnKModelInputs(HistoricalGamesBuffer hgb, int numSamples) {
		// need a way to pass in the input dimensions, but i'll hard-code them for now

		int rows = 9;
		int columns = 9;
		int channels = 4;

		INDArray inputs = Nd4j.zeros(numSamples, rows, columns, channels);
		INDArray policy = Nd4j.zeros(numSamples, rows * columns + 1);
		INDArray utility = Nd4j.zeros(numSamples, 1);

		for(int sample = 0; sample < numSamples; sample++) {
			// pick a game to sample
			var historicalGame = hgb.getUniformRandomGame();



		}
	}*/

	/*
	public static INDArray fromBufferReturnKModelInputs(HistoricalGamesBuffer, numSamples) {
		// this is where we'd generate a symmetry if that was called for in the training schedule
		// we'd treat the sampled game as a canonical game and generate a random symmetry out of the 8 possible ones, including "identity"
		game_replay = get_replay_data(historical_game) // move-by-move board states, next moves, and final outcome
		random_move = uniform_random_over_range(len(game_replay))

		// a few thoughts on the above... i don't love the idea of simulating an entire game each time i do this, but i also know that i won't know the outcome unless i do because i'm not storing that...
		// so the get_replay_data() function is going to need to basically create a MatchRecord as it's going ...
		// i may introduce the concept of a MatchAuditor who's job it is to replay a HistoricalGame and record board state -> next action pairs as well as recording the eventual outcome
		// a MatchAuditor has no business knowing about symmetries, so symmetries are going to need to be generated probably as a wrapper to the function above as follows:

		// since it's a Go Symmetry, we can assume the class knows there are 8 symmetrical modes and we don't have to specify that.
		// with knowledge of the moves made and the board size, GoSymmetries will do a simple transformation on the moves to generate a new moveset and return a new HistoricalGame
		game_replay = get_replay_data(GoSymmetries.randomSymmetry(historical_game))

		// this class is going to need to know something about the model input shape to be able to sample things correctly
		// this would also be the time to generate a random symmetry if we were going to do that... (actually a few lines up)
		teachable_moments.append(state_action_pair_at_move(game_replay, random_move))

		// now we have as many state-action pairs as we'd like for this training batch, including symmetries if we choose
		// it's time to convert our state-action pairs into INDArrays of inputs and outputs

		// there's some tedium involved, but i pretty much know how to read and write like this, so i'll gloss over it for now

		return some data structure that is the right shape for the model

	}*/

}
