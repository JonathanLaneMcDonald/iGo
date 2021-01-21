public class DataTransformer {
	/*
	i think this is going to be a collection of static functions

	i'd like to have the following:
		board to features:
			really quick and dirty. i provide something like (iGo.getBoard(), boardSize, playerToMove) and i get a 4 channel feature set

		i'll have to decide eventually if this is the appropriate place to calculate symmetries

		in this trivial case, TeachableMoment can just be (boardState, boardSize, playerToMove, policyEnacted, gameOutcome)
		or maybe more specifically, a TeachableMoment can be (ModelInput, ModelOutput) where
		ModelInput = (boardState, boardSize, playerToMove) and
		ModelOutput = (policyEnacted, gameOutcome)
		because i'm not going to want to construct a TeachableMoment when I'm doing mid-game inference because there's no outcome yet!
	 */
}
