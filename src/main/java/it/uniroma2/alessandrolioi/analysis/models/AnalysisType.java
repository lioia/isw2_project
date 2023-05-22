package it.uniroma2.alessandrolioi.analysis.models;

import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.RandomForest;

public class AnalysisType {
    public enum Classifiers {RANDOM_FOREST, NAIVE_BAYES, IBK,}

    public enum FeatureSelection {No}

    public enum Sampling {No}

    public enum CostSensitive {No}
}
