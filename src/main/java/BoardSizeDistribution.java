import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class BoardSizeDistribution {

	private final Random random;
	private final ArrayList<Integer> boardSizeDistribution;

	public BoardSizeDistribution(int[] boardSizes, boolean inverseProportionalSampling) {

		random = new Random();

		if(inverseProportionalSampling) {
			int aggregateBoardArea = Arrays.stream(boardSizes).map(p->p*p).reduce(0, Integer::sum);

			boardSizeDistribution = new ArrayList<>();
			for(int size : boardSizes)
				for(int i = 0; i < aggregateBoardArea/(size*size); i++)
					boardSizeDistribution.add(size);
		}
		else {
			boardSizeDistribution = new ArrayList<>();
			for(int size : boardSizes)
				boardSizeDistribution.add(size);
		}
	}

	public int getBoardSize() {
		return boardSizeDistribution.get(random.nextInt(boardSizeDistribution.size()));
	}
}
