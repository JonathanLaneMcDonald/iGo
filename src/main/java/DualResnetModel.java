import org.deeplearning4j.nn.graph.ComputationGraph;

public class DualResnetModel {

	public static ComputationGraph getModel(int blocks, int numPlanes) {

		DL4JAlphaGoZeroBuilder builder = new DL4JAlphaGoZeroBuilder();
		String input = "in";

		builder.addInputs(input);
		String initBlock = "init";
		String convOut = builder.addConvBatchNormBlock(initBlock, input, numPlanes, true);
		String towerOut = builder.addResidualTower(blocks, convOut);
		String policyOut = builder.addPolicyHead(towerOut, true);
		String valueOut = builder.addValueHead(towerOut, true);
		builder.addOutputs(policyOut, valueOut);

		ComputationGraph model = new ComputationGraph(builder.buildAndReturn());
		model.init();

		return model;
	}
}
