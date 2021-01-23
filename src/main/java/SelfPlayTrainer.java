public class SelfPlayTrainer {

	private TrainingSchedule schedule;
	private HistoricalGamesBuffer hgb;
	private TrainablePolicy policy;

	public SelfPlayTrainer(TrainingSchedule schedule, HistoricalGamesBuffer hgb, TrainablePolicy policy) {
		this.schedule = schedule;
		this.hgb = hgb;
		this.policy = policy;
	}
}
