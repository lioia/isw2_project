package it.uniroma2.alessandrolioi.analysis.controllers;

import it.uniroma2.alessandrolioi.analysis.exceptions.ArffException;
import it.uniroma2.alessandrolioi.common.DatasetPaths;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

import java.nio.file.Path;

public class AnalysisController {
    public Instances loadInstance(String project, int testingRelease, String instanceType) throws ArffException {
        try {
            Path path = DatasetPaths.fromProject(project)
                    .resolve("arff")
                    .resolve(String.format("%s-%d.arff", instanceType, testingRelease));
            Instances instance = ConverterUtils.DataSource.read(path.toString());
            if (instance.classIndex() == -1)
                instance.setClassIndex(instance.numAttributes() - 1);
            return instance;
        } catch (Exception e) {
            throw new ArffException("Could not load arff file", e);
        }
    }
}
