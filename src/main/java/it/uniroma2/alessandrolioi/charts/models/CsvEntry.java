package it.uniroma2.alessandrolioi.charts.models;

import it.uniroma2.alessandrolioi.analysis.models.AnalysisType;

public record CsvEntry(int version, AnalysisType.Classifiers classifier, AnalysisType.FeatureSelection featureSelection,
                       AnalysisType.Sampling sampling, AnalysisType.CostSensitive costSensitive,
                       double precision, double recall, double kappa, double auc) {
}
