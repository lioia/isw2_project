package it.uniroma2.alessandrolioi.analysis.models;

import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.classifiers.trees.RandomForest;

public class AnalysisType {
    public enum Classifiers {
        RANDOM_FOREST, NAIVE_BAYES, IBK;

        public static Classifiers fromClassifier(Classifier classifier) {
            if (classifier instanceof RandomForest) return Classifiers.RANDOM_FOREST;
            if (classifier instanceof NaiveBayes) return Classifiers.NAIVE_BAYES;
            if (classifier instanceof IBk) return Classifiers.IBK;
            return null;
        }
    }

    public enum FeatureSelection {}

    public enum Sampling {}

    public enum CostSensitive {}
}
