package it.uniroma2.alessandrolioi.analysis.models;

public record Report(int releases, AnalysisType.Classifiers classifier, AnalysisType.FeatureSelection featureSelection,
                     AnalysisType.Sampling sampling, AnalysisType.CostSensitive costSensitive,
                     double precision, double recall, double auc, double kappa) {
    public String toCsvString(String project) {
        return "%s,%d,%s,%s,%s,%s,%f,%f,%f,%f"
                .formatted(
                        project, releases(),
                        classifier(), featureSelection(), sampling(), costSensitive(),
                        precision(), recall(), kappa(), auc());
    }
}
