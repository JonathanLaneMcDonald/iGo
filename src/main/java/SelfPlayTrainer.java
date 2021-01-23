public class SelfPlayTrainer {

	private TrainingSchedule schedule;
	private HistoricalGamesBuffer hgb;
	private TrainablePolicy policy;
	private MultiMatchOrchestrator orchestrator;

	public SelfPlayTrainer(TrainingSchedule schedule, HistoricalGamesBuffer hgb, TrainablePolicy policy, MultiMatchOrchestrator orchestrator) {
		this.schedule = schedule;
		this.hgb = hgb;
		this.policy = policy;
		this.orchestrator = orchestrator;
	}

	public void commenceTraining(int parallelGames, int asyncWorkers) {
		/* Where I currently am
			I can automate the playing of multiple matches with player-specific suppliers for strategies
			I can buffer old game records and store new ones
		 */

		/* TODO list to implement this function
			* i need to actually get training and inference working for the model i've built
			* i need to be able to represent a game state as input to a model
			* MCTS needs to be model-enabled. it looks like i can tack that on
			* need player and strategy presets for model-enabled MCTS.
			*
			* i've got some notes in the TODO file about this...
			*

		 */

		/* TODO list to *properly* implement this function
			* multithreading and async support in MCTS and MMO
			* i'll need a way of aggregating inference requests in a thread-safe way and i'll need callbacks to update the MCTS instances
		 */
	}
}
