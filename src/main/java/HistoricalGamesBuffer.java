
import java.io.*;
import java.util.ArrayList;

public class HistoricalGamesBuffer {

	private final TrainingSchedule schedule;
	private BufferedWriter bufferedWriter;
	private ArrayList<HistoricalGame> historicalGames;

	public HistoricalGamesBuffer(TrainingSchedule schedule, String pathToHistoricalGames) {
		this.schedule = schedule;

		bufferedWriter = null;

		loadSavedGamesFromFile(pathToHistoricalGames);

		prepareToAppendNewGamesToFile(pathToHistoricalGames);

		System.out.println(historicalGames.size()+" games loaded into replay buffer");

		trimHistoricalGamesBuffer();
	}

	public void registerNewGame(MatchRecord matchRecord) {
		try {
			var gameString = matchRecord.movesToSGF();

			bufferedWriter.write(gameString + "\n");
			bufferedWriter.flush();

			attemptToBufferGame(gameString);
		}
		catch(IOException e) {
			// I'm fine with nuking everything here because I really don't want to continue if I can't save games
			throw new RuntimeException("bufferedWriter == null in HistoricalGamesBuffer::registerNewGame()");
		}
	}

	private void attemptToBufferGame(String gameString) {
		var optOfGame = HistoricalGame.parseFromString(gameString);

		if(optOfGame.isPresent())
			historicalGames.add(optOfGame.get());
		else
			System.out.println("Parsing error in HistoricalGamesBuffer::HistoricalGamesBuffer()");
	}

	private void trimHistoricalGamesBuffer() {
		int originalLength = historicalGames.size();
		while(historicalGames.size() > schedule.replayBufferGameCount)
			historicalGames.remove(0);
		if(originalLength != historicalGames.size())
			System.out.println(originalLength - historicalGames.size()+" games trimmed from replay buffer");
	}

	private void loadSavedGamesFromFile(String allegedPath) {

		historicalGames = new ArrayList<>();

		try {
			var bufferedReader = new BufferedReader(new FileReader(allegedPath));

			String line = bufferedReader.readLine();
			while(line != null) {
				attemptToBufferGame(line);
				line = bufferedReader.readLine();
			}
		}
		catch(IOException e) {
			System.out.println("Error opening file: "+allegedPath);
		}
	}

	private void prepareToAppendNewGamesToFile(String desiredPath) {
		try {
			bufferedWriter = new BufferedWriter(new FileWriter(desiredPath, true));
		}
		catch(IOException e) {
			System.out.println("Error opening file for appending: "+desiredPath);
			System.out.println("Creating file: "+desiredPath);

			try {
				bufferedWriter = new BufferedWriter(new FileWriter(desiredPath));
			}
			catch(IOException f) {
				throw new RuntimeException("File could neither be opened nor created: "+desiredPath);
			}
		}
	}
}
