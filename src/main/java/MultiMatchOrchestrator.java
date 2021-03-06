import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MultiMatchOrchestrator {

	/*
	this will ideally orchestrate gameplay across many threads to complete the specified requests

	this may involve a shared queue for input frames so models can do inferences in batches... i feel like that belongs somewhere else

	i also feel like i should be generating Strategies for black and white on a per-game basis, but i need to figure out how to best do that

	 */

	int errors;

	StrategySupplier blackSupplier;
	StrategySupplier whiteSupplier;

	MultiMatchConfiguration mmConfig;

	private ArrayList<MatchRecord> matchRecords;

	public MultiMatchOrchestrator(StrategySupplier forBlack, StrategySupplier forWhite, MultiMatchConfiguration mmConfig) {
		errors = 0;

		blackSupplier = forBlack;
		whiteSupplier = forWhite;

		this.mmConfig = mmConfig;

		matchRecords = new ArrayList<>();
	}

	public void playGame() {
		var matchConfig = new MatchConfiguration(mmConfig.getBoardSize(), mmConfig.getKomi());
		var optBlackStrategy = blackSupplier.getStrategy(matchConfig);
		var optWhiteStrategy = whiteSupplier.getStrategy(matchConfig);
		if(optBlackStrategy.isPresent() && optWhiteStrategy.isPresent()) {
			var result = new MatchFacilitator(optBlackStrategy.get(), optBlackStrategy.get()).facilitateMatch(matchConfig);
			if(result.gameIsFreeOfErrorsAndPlayedToConclusion())
				matchRecords.add(result);
			else {
				errors++;
				System.out.println(errors+" errors in MultiMatchOrchestrator::playGame()");
			}
		}
	}

	public void playNGames(int games) {
		for(int i = 0; i < games; i++) {
			playGame();
			System.out.println("Game Finished:"+matchRecords.size());
		}
	}

	public void playNGamesInKThreads(int games, int threads) {

	}

	public List<MatchRecord> getMatchRecords() {
		return matchRecords;
	}

	public List<String> getSGFRecords() {
		return matchRecords.stream().map(MatchRecord::movesToSGF).collect(Collectors.toList());
	}
}
