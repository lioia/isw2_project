package it.uniroma2.alessandrolioi.analysis.boundaries;

import it.uniroma2.alessandrolioi.analysis.controllers.AnalysisController;
import it.uniroma2.alessandrolioi.analysis.exceptions.*;
import it.uniroma2.alessandrolioi.analysis.models.AnalysisType;
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
import weka.filters.supervised.instance.SpreadSubsample;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SMOTE;

import java.util.ArrayList;
import java.util.List;

public class Analysis {
    private final String project;
    private final int lastRelease;
    private Instances testing;
    private Instances training;

    public Analysis(String project, int lastRelease) {
        this.project = project;
        this.lastRelease = lastRelease;
    }

    public List<Report> performAnalysis() throws ClassifierException, EvaluationException, FeatureSelectionException, SamplingException, ArffException {
        List<Report> reports = new ArrayList<>();
        // No Feature Selection, No Balancing, No Cost Sensitive
        for (AnalysisType.Classifiers classifierType : AnalysisType.Classifiers.values()) {
            loadInstances();
            applyFeatureSelection(AnalysisType.FeatureSelection.NONE);
            applySampling(AnalysisType.Sampling.NONE);
            Classifier classifier = selectClassifier(classifierType);
            Evaluation evaluation = analyze(classifier);
            Report report = generateReport(evaluation, classifierType, AnalysisType.FeatureSelection.NONE, AnalysisType.Sampling.NONE);
            reports.add(report);
        }
        // Feature Selection, Balancing, No Cost Sensitive
        for (AnalysisType.Sampling sampling : AnalysisType.Sampling.values()) {
            for (AnalysisType.Classifiers classifierType : AnalysisType.Classifiers.values()) {
                loadInstances();
                applyFeatureSelection(AnalysisType.FeatureSelection.BEST_FIRST);
                applySampling(sampling);
                Classifier classifier = selectClassifier(classifierType);
                Evaluation evaluation = analyze(classifier);
                Report report = generateReport(evaluation, classifierType, AnalysisType.FeatureSelection.BEST_FIRST, sampling);
                reports.add(report);
            }
        }
        return reports;
    }

    private void loadInstances() throws ArffException {
        AnalysisController controller = new AnalysisController();
        this.training = controller.loadInstance(project, lastRelease, "training");
        this.testing = controller.loadInstance(project, lastRelease, "testing");
    }

    private Classifier selectClassifier(AnalysisType.Classifiers classifierType) {
        return switch (classifierType) {
            case RANDOM_FOREST -> new RandomForest();
            case NAIVE_BAYES -> new NaiveBayes();
            case IBK -> new IBk();
        };
    }

    private void applyFeatureSelection(AnalysisType.FeatureSelection featureSelection) throws FeatureSelectionException {
        if (featureSelection == AnalysisType.FeatureSelection.BEST_FIRST) {
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

    private void applySampling(AnalysisType.Sampling sampling) throws SamplingException {
        int yesInstances = calculateYes();
        int majority = Math.max(yesInstances, training.size() - yesInstances);
        double percent = 100 * 2 * ((double) majority) / training.size();
        if (percent < 50) percent = 100 - percent;
        switch (sampling) {
            case NONE -> {
                // does not need to apply anything
            }
            case UNDER_SAMPLING -> {
                try {
                    SpreadSubsample underSample = new SpreadSubsample();
                    underSample.setInputFormat(training);
                    underSample.setDistributionSpread(1.0);
                    training = Filter.useFilter(training, underSample);
                } catch (Exception e) {
                    throw new SamplingException("Could not apply Under Sampling", e);
                }
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

    private int calculateYes() {
        int buggy = 0;
        for (Instance instance : training) {
            if (instance.stringValue(instance.numAttributes() - 1).equals("true"))
                buggy += 1;
        }
        return buggy;
    }

    private Evaluation analyze(Classifier classifier) throws EvaluationException, ClassifierException {
        try {
            classifier.buildClassifier(training);
        } catch (Exception e) {
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
                                  AnalysisType.FeatureSelection featureSelection, AnalysisType.Sampling sampling) {
        double auc = evaluation.areaUnderROC(1);
        if (Double.isNaN(auc)) auc = 0;
        return new Report(
                lastRelease,
                classifierType, featureSelection, sampling,
                evaluation.precision(1),
                evaluation.recall(1),
                auc,
                evaluation.kappa());
    }
}
