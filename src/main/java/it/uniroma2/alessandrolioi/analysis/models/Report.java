package it.uniroma2.alessandrolioi.analysis.models;

public record Report(AnalysisType.Classifiers classifier, double precision, double recall, double auc, double kappa) {
    @Override
    public String toString() {
        return "Report: Classifier=" + classifier.name() +
                ", Precision=" + precision +
                ", Recall=" + recall +
                ", AUC=" + auc +
                ", Kappa=" + kappa;
    }
}
