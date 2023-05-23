package it.uniroma2.alessandrolioi.analysis.controllers;

import it.uniroma2.alessandrolioi.analysis.exceptions.ArffException;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

public class AnalysisController {
    public Instances loadInstance(String project, int testingRelease, String instanceType) throws ArffException {
        try {
            String filename = "dataset/%s/%s-%d.arff".formatted(project, instanceType, testingRelease);
            Instances instance = ConverterUtils.DataSource.read(filename);
            if (instance.classIndex() == -1)
                instance.setClassIndex(instance.numAttributes() - 1);
            return instance;
        } catch (Exception e) {
            throw new ArffException("Could not load arff file", e);
        }
    }
}
