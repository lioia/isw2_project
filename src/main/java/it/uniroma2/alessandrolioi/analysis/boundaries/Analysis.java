package it.uniroma2.alessandrolioi.analysis.boundaries;

import it.uniroma2.alessandrolioi.analysis.controllers.ConverterController;
import it.uniroma2.alessandrolioi.analysis.exceptions.*;
import it.uniroma2.alessandrolioi.analysis.models.AnalysisType;
import it.uniroma2.alessandrolioi.analysis.models.CsvEntry;
import it.uniroma2.alessandrolioi.analysis.models.Report;
import weka.attributeSelection.BestFirst;
import weka.attributeSelection.CfsSubsetEval;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.RandomForest;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SMOTE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Analysis {
    private final String project;
    private final int lastRelease;
    private final int totalReleases;
    private Instances testing;
    private Instances training;
    private Classifier classifier;

    public Analysis(String project, int lastRelease, int totalReleases) throws CsvException, ArffException {
        this.project = project;
        this.lastRelease = lastRelease;
        this.totalReleases = totalReleases;

        loadCsv();
        loadInstances();
    }

    public List<Report> performAnalysis() throws ClassifierException, EvaluationException, FeatureSelectionException, SamplingException {
        List<Report> reports = new ArrayList<>();
        for (AnalysisType.FeatureSelection featureSelection : AnalysisType.FeatureSelection.values()) {
            applyFeatureSelection(featureSelection);
            for (AnalysisType.Sampling sampling : AnalysisType.Sampling.values()) {
                applySampling(sampling);
                for (AnalysisType.CostSensitive costSensitive : AnalysisType.CostSensitive.values()) {
                    applyCostSensitive(costSensitive);
                    for (AnalysisType.Classifiers classifierType : AnalysisType.Classifiers.values()) {
                        selectClassifier(classifierType);
                        Evaluation evaluation = analyze();
                        Report report = generateReport(evaluation, classifierType, featureSelection, sampling, costSensitive);
                        reports.add(report);
                    }
                }
            }
        }
        return reports;
    }

    private void loadCsv() throws CsvException, ArffException {
        ConverterController controller = new ConverterController();
        Map<Integer, List<CsvEntry>> entries = controller.readCsv(project, totalReleases);
        controller.writeToArff(project, entries, lastRelease);
    }

    private void loadInstances() throws ArffException {
        ConverterController controller = new ConverterController();
        this.training = controller.loadInstance(project, lastRelease, "training");
        this.testing = controller.loadInstance(project, lastRelease, "testing");
    }

    private void selectClassifier(AnalysisType.Classifiers classifierType) {
        switch (classifierType) {
            case RANDOM_FOREST -> classifier = new RandomForest();
            case NAIVE_BAYES -> classifier = new NaiveBayes();
            case IBK -> classifier = new IBk();
        }
    }

    private void applyFeatureSelection(AnalysisType.FeatureSelection featureSelection) throws FeatureSelectionException {
        switch (featureSelection) {
            case NONE -> {
            }
            case BEST_FIRST -> {
                AttributeSelection filter = new AttributeSelection();
                CfsSubsetEval evaluator = new CfsSubsetEval();
                BestFirst search = new BestFirst();
                filter.setEvaluator(evaluator);
                filter.setSearch(search);
                try {
                    filter.setInputFormat(training);
                    training = Filter.useFilter(training, filter);
                    testing = Filter.useFilter(testing, filter);
                } catch (Exception e) {
                    throw new FeatureSelectionException("Could not apply Best First feature selection", e);
                }
            }
        }
    }

    private void applySampling(AnalysisType.Sampling sampling) throws SamplingException {
        int yesInstances = calculateYes();
        int majority = Math.max(yesInstances, training.size() - yesInstances);
        double percent = 100 * 2 * ((double) majority) / training.size();
        if (percent < 50) percent = 100 - percent;
        switch (sampling) {
            case NONE -> {
            }
            case OVER_SAMPLING -> {
                try {
                    Resample overSample = new Resample();
                    overSample.setInputFormat(training);
                    overSample.setNoReplacement(false);
                    overSample.setBiasToUniformClass(1.0);
                    overSample.setSampleSizePercent(percent);
                    training = Filter.useFilter(training, overSample);
                } catch (Exception e) {
                    throw new SamplingException("Could not apply Over Sampling", e);
                }
            }
            case SMOTE -> {
                try {
                    SMOTE smote = new SMOTE();
                    smote.setInputFormat(training);
                    smote.setPercentage(percent);
                    training = Filter.useFilter(training, smote);
                } catch (Exception e) {
                    throw new SamplingException("Could not apply SMOTE", e);
                }
            }
        }
    }

    private void applyCostSensitive(AnalysisType.CostSensitive costSensitive) {

    }

    private int calculateYes() {
        int buggy = 0;
        for (Instance instance : training) {
            if (instance.stringValue(instance.numAttributes() - 1).equals("true"))
                buggy += 1;
        }
        return buggy;
    }

    private Evaluation analyze() throws EvaluationException, ClassifierException {
        try {
            classifier.buildClassifier(training);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ClassifierException("Could not build classifier", e);
        }
        try {
            Evaluation evaluation = new Evaluation(training);
            evaluation.evaluateModel(classifier, testing);
            return evaluation;
        } catch (Exception e) {
            throw new EvaluationException("Could not evaluate classifier", e);
        }
    }

    private Report generateReport(Evaluation evaluation, AnalysisType.Classifiers classifierType,
                                  AnalysisType.FeatureSelection featureSelection, AnalysisType.Sampling sampling,
                                  AnalysisType.CostSensitive costSensitive) {
        double auc = evaluation.areaUnderROC(1);
        if (Double.isNaN(auc)) auc = 0;
        return new Report(
                lastRelease,
                classifierType, featureSelection, sampling, costSensitive,
                evaluation.precision(1),
                evaluation.recall(1),
                auc,
                evaluation.kappa());
    }
}
