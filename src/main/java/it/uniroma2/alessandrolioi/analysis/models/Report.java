package it.uniroma2.alessandrolioi.analysis.models;

public final class Report {
    private AnalysisType.Classifiers classifier;
    private AnalysisType.FeatureSelection featureSelection;
    private AnalysisType.Sampling sampling;
    private AnalysisType.CostSensitive costSensitive;
    private final int releases;
    private final double precision;
    private final double recall;
    private final double auc;
    private final double kappa;

    public Report(int releases, double precision, double recall, double auc, double kappa) {
        this.releases = releases;
        this.precision = precision;
        this.recall = recall;
        if (Double.isNaN(auc)) this.auc = 0f;
        else this.auc = auc;
        this.kappa = kappa;
    }

    public void setProperties(AnalysisType.Classifiers classifier, AnalysisType.FeatureSelection featureSelection,
                              AnalysisType.Sampling sampling, AnalysisType.CostSensitive costSensitive) {
        this.classifier = classifier;
        this.featureSelection = featureSelection;
        this.sampling = sampling;
        this.costSensitive = costSensitive;
    }

    public String toCsvString(String project) {
        return "%s,%d,%s,%s,%s,%s,%f,%f,%f,%f"
                .formatted(
                        project, releases(),
                        classifier(), featureSelection(), sampling(), costSensitive(),
                        precision(), recall(), kappa(), auc());
    }

    public AnalysisType.Classifiers classifier() {
        return classifier;
    }

    public AnalysisType.FeatureSelection featureSelection() {
        return featureSelection;
    }

    public AnalysisType.Sampling sampling() {
        return sampling;
    }

    public AnalysisType.CostSensitive costSensitive() {
        return costSensitive;
    }

    public double precision() {
        return precision;
    }

    public double recall() {
        return recall;
    }

    public double auc() {
        return auc;
    }

    public double kappa() {
        return kappa;
    }

    public int releases() {
        return releases;
    }
}
