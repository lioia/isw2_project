package it.uniroma2.alessandrolioi.analysis;

import it.uniroma2.alessandrolioi.analysis.controllers.ConverterController;
import it.uniroma2.alessandrolioi.analysis.exceptions.ArffException;
import it.uniroma2.alessandrolioi.analysis.exceptions.ClassifierException;
import it.uniroma2.alessandrolioi.analysis.exceptions.CsvException;
import it.uniroma2.alessandrolioi.analysis.exceptions.EvaluationException;
import it.uniroma2.alessandrolioi.analysis.models.AnalysisType;
import it.uniroma2.alessandrolioi.analysis.models.CsvEntry;
import it.uniroma2.alessandrolioi.analysis.models.Report;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.RandomForest;
import weka.core.Instances;

import java.util.List;
import java.util.Map;

public class AnalysisBuilder {
    private final String project;
    private final int lastRelease;
    private Instances testing, training;
    private Classifier classifier;

    public AnalysisBuilder(String project, int lastRelease) throws CsvException {
        this.project = project;
        this.lastRelease = lastRelease;
    }

    public AnalysisBuilder loadCsv() throws CsvException, ArffException {
        ConverterController controller = new ConverterController();
        Map<Integer, List<CsvEntry>> entries = controller.readCsv(project, lastRelease);
        controller.writeToArff(project, entries, lastRelease);
        return this;
    }

    public AnalysisBuilder loadInstances() throws ArffException {
        ConverterController controller = new ConverterController();
        this.testing = controller.loadInstance(project, lastRelease, "testing");
        this.training = controller.loadInstance(project, lastRelease, "training");
        return this;
    }

    public AnalysisBuilder buildClassifier(AnalysisType.Classifiers classifierType) throws ClassifierException {
        switch (classifierType) {
            case RANDOM_FOREST -> classifier = new RandomForest();
            case NAIVE_BAYES -> classifier = new NaiveBayes();
            case IBK -> classifier = new IBk();
        }
        try {
            classifier.buildClassifier(training);
            return this;
        } catch (Exception e) {
            throw new ClassifierException("Could not build classifier", e);
        }
    }

    public Report generateReport() throws EvaluationException {
        try {
            Evaluation evaluation = new Evaluation(training);
            evaluation.evaluateModel(classifier, testing);
            return new Report(
                    AnalysisType.Classifiers.fromClassifier(classifier),
                    evaluation.weightedPrecision(),
                    evaluation.weightedRecall(),
                    evaluation.weightedAreaUnderROC(),
                    evaluation.kappa());
        } catch (Exception e) {
            throw new EvaluationException("Could not evaluate classifier", e);
        }
    }
}
