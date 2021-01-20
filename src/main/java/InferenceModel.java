import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.modelimport.keras.KerasModelImport;
import org.deeplearning4j.nn.modelimport.keras.exceptions.InvalidKerasConfigurationException;
import org.deeplearning4j.nn.modelimport.keras.exceptions.UnsupportedKerasConfigurationException;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.io.ClassPathResource;// load the model

import java.io.IOException;

public class InferenceModel {

	public InferenceModel() throws IOException, InvalidKerasConfigurationException, UnsupportedKerasConfigurationException {

		String simpleMlp = new ClassPathResource("az model exported with tf 2.2.0.h5").getFile().getPath();
		ComputationGraph model = KerasModelImport.importKerasModelAndWeights(simpleMlp);

		model.summary();


		int inputs = 10;
		INDArray features = Nd4j.zeros(inputs);
		for (int i = 0; i < inputs; i++)
			features.putScalar(new int[]{i}, Math.random() < 0.5 ? 0 : 1);// get the prediction
		// double prediction = model.output(features).getDouble(0);
	}
}
