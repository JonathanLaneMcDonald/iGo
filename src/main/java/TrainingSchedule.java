public class TrainingSchedule {

	public int preTrainGameCount;
	public int replayBufferGameCount;
	public int trainOnMod;
	public int saveCheckpointOnMod;
	public int trainingBatchSize;
	public boolean trainOnSymmetries;

	public TrainingSchedule() {
	}

	public static TrainingSchedule defaultSchedule() {
		var schedule = new TrainingSchedule();

		// these are timing-related and are largely aesthetic and unspecific
		schedule.preTrainGameCount = 16384;
		schedule.replayBufferGameCount = 131072;
		schedule.saveCheckpointOnMod = schedule.replayBufferGameCount;

		// these three relate specifically to the fact that Go has 8 symmetrical modes (4 rotational * 2 reflectional)
		schedule.trainOnMod = 128;
		schedule.trainingBatchSize = 1024;
		schedule.trainOnSymmetries = true;

		return schedule;
	}
}
