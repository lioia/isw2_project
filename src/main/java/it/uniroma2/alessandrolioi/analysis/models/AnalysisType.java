package it.uniroma2.alessandrolioi.analysis.models;

public class AnalysisType {
    public enum Classifiers {RANDOM_FOREST, NAIVE_BAYES, IBK,}

    public enum FeatureSelection {NONE, BEST_FIRST}

    public enum Sampling {NONE, UNDER_SAMPLING, OVER_SAMPLING, SMOTE}
}
