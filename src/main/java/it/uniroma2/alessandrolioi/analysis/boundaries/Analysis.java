package it.uniroma2.alessandrolioi.analysis.boundaries;

import it.uniroma2.alessandrolioi.analysis.controllers.ConverterController;
import it.uniroma2.alessandrolioi.analysis.exceptions.*;
import it.uniroma2.alessandrolioi.analysis.models.AnalysisType;
import it.uniroma2.alessandrolioi.analysis.models.CsvEntry;
import it.uniroma2.alessandrolioi.analysis.models.Report;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Analysis {
    private final String project;
    private final int lastRelease;
    private Instances testing, training;
    private Classifier classifier;

    public Analysis(String project, int lastRelease) throws CsvException, ArffException {
        this.project = project;
        this.lastRelease = lastRelease;

        loadCsv();
        loadInstances();
    }

    public void loadCsv() throws CsvException, ArffException {
        ConverterController controller = new ConverterController();
        Map<Integer, List<CsvEntry>> entries = controller.readCsv(project, lastRelease);
        controller.writeToArff(project, entries, lastRelease);
    }

    public void loadInstances() throws ArffException {
        ConverterController controller = new ConverterController();
        this.training = controller.loadInstance(project, lastRelease, "training");
        this.testing = controller.loadInstance(project, lastRelease, "testing");
    }

    public void buildClassifier(AnalysisType.Classifiers classifierType) throws ClassifierException {
        switch (classifierType) {
            case RANDOM_FOREST -> classifier = new RandomForest();
            case NAIVE_BAYES -> classifier = new NaiveBayes();
            case IBK -> classifier = new IBk();
        }
        try {
            classifier.buildClassifier(training);
        } catch (Exception e) {
            throw new ClassifierException("Could not build classifier", e);
        }
    }

    public Report generateReport() throws EvaluationException {
        try {
            Evaluation evaluation = new Evaluation(training);
            evaluation.evaluateModel(classifier, testing);
            return new Report(
                    lastRelease,
                    evaluation.precision(1),
                    evaluation.recall(1),
                    evaluation.areaUnderROC(1),
                    evaluation.kappa());
        } catch (Exception e) {
            throw new EvaluationException("Could not evaluate classifier", e);
        }
    }
}
