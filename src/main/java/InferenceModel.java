import org.deeplearning4j.nn.conf.ConvolutionMode;
import org.deeplearning4j.nn.conf.InputPreProcessor;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.graph.ElementWiseVertex;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.*;
import org.deeplearning4j.nn.conf.preprocessor.CnnToFeedForwardPreProcessor;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.Sgd;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.util.HashMap;
import java.util.Map;

public class InferenceModel {

	public static ComputationGraph getModel(int filters, int blocks, InputShape inputShape) {
		var config = new NeuralNetConfiguration.Builder()
				.updater(new Sgd()).weightInit(WeightInit.NORMAL).graphBuilder()
				.setInputTypes(InputType.convolutional(inputShape.rows, inputShape.cols, inputShape.depth));

		config.addInputs("InputFeatures");

		// projection layers
		config.addLayer("Projector", new ConvolutionLayer.Builder()
				.kernelSize(new int[]{3,3}).stride(new int[]{1,1}).convolutionMode(ConvolutionMode.Same)
				.nIn(inputShape.depth).nOut(filters).build(), "InputFeatures");

		config.addLayer("ProjectorBN", new BatchNormalization.Builder()
				.nOut(filters).build(), "Projector");

		config.addLayer("ProjectorRELU", new ActivationLayer.Builder()
				.activation(Activation.RELU).build(), "ProjectorBN");

		String nextInput = "ProjectorRELU";
		String skipConnection = "ProjectorRELU";

		for(int block = 1; block < blocks+1; block ++) {

			// residual block 1
			config.addLayer("ResConv1Block"+block, new ConvolutionLayer.Builder()
					.kernelSize(new int[]{3,3}).stride(new int[]{1,1}).convolutionMode(ConvolutionMode.Same)
					.nIn(filters).nOut(filters).build(), nextInput);

			config.addLayer("ResBN1Block"+block, new BatchNormalization.Builder()
					.nOut(filters).build(), "ResConv1Block"+block);

			config.addLayer("ResRELU1Block"+block, new ActivationLayer.Builder()
					.activation(Activation.RELU).build(), "ResBN1Block"+block);


			// residual block 2
			config.addLayer("ResConv2Block"+block, new ConvolutionLayer.Builder()
					.kernelSize(new int[]{3,3}).stride(new int[]{1,1}).convolutionMode(ConvolutionMode.Same)
					.nIn(filters).nOut(filters).build(), "ResRELU1Block"+block);

			config.addLayer("ResBN2Block"+block, new BatchNormalization.Builder()
					.nOut(filters).build(), "ResConv2Block"+block);

			config.addVertex("AddResultBlock"+block, new ElementWiseVertex(ElementWiseVertex.Op.Add), skipConnection, "ResBN2Block"+block);

			config.addLayer("ResRELU2Block"+block, new ActivationLayer.Builder()
					.activation(Activation.RELU).build(), "AddResultBlock"+block);

			nextInput = "ResRELU2Block"+block;
			skipConnection = "ResRELU2Block"+block;
		}

		// policy head
		config.addLayer("PolicyProjector", new ConvolutionLayer.Builder()
				.kernelSize(new int[]{1,1}).stride(new int[]{1,1}).convolutionMode(ConvolutionMode.Same)
				.nIn(filters).nOut(2).build(), nextInput);

		config.addLayer("PolicyProjectorBN", new BatchNormalization.Builder()
				.nOut(2).build(), "PolicyProjector");

		config.addLayer("PolicyProjectorRELU", new ActivationLayer.Builder()
				.activation(Activation.RELU).build(), "PolicyProjectorBN");

		config.addLayer("Policy", new OutputLayer.Builder(LossFunctions.LossFunction.XENT)
				.activation(Activation.SOFTMAX)
				.nIn(2 * inputShape.rows * inputShape.cols)
				.nOut(inputShape.rows * inputShape.cols + 1)
				.build(), "PolicyProjectorRELU");

		Map<String, InputPreProcessor> policyProcessorMap = new HashMap<String, InputPreProcessor>();
		policyProcessorMap.put("Policy", new CnnToFeedForwardPreProcessor(inputShape.rows, inputShape.cols, 2));
		config.setInputPreProcessors(policyProcessorMap);

		// value head
		config.addLayer("ValueProjector", new ConvolutionLayer.Builder()
				.kernelSize(new int[]{1,1}).stride(new int[]{1,1}).convolutionMode(ConvolutionMode.Same)
				.nIn(filters).nOut(1).build(), nextInput);

		config.addLayer("ValueProjectorBN", new BatchNormalization.Builder()
				.nOut(1).build(), "ValueProjector");

		config.addLayer("ValueProjectorRELU", new ActivationLayer.Builder()
				.activation(Activation.RELU).build(), "ValueProjectorBN");

		config.addLayer("ValueDense", new DenseLayer.Builder()
				.nIn(inputShape.rows * inputShape.cols)
				.nOut(256)
				.build(), "ValueProjectorRELU");

		Map<String, InputPreProcessor> valueProcessorMap = new HashMap<String, InputPreProcessor>();
		valueProcessorMap.put("ValueDense", new CnnToFeedForwardPreProcessor(inputShape.rows, inputShape.cols, 1));
		config.setInputPreProcessors(valueProcessorMap);

		config.addLayer("Value", new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
				.activation(Activation.TANH).nIn(256).nOut(1).build(), "ValueDense");

		config.setOutputs("Policy", "Value");

		var model = new ComputationGraph(config.build());
		model.init();

		return model;
	}
}
