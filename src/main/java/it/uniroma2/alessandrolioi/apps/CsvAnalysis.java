package it.uniroma2.alessandrolioi.apps;

import it.uniroma2.alessandrolioi.analysis.boundaries.Analysis;
import it.uniroma2.alessandrolioi.analysis.boundaries.ReportHandler;
import it.uniroma2.alessandrolioi.analysis.models.AnalysisType;
import it.uniroma2.alessandrolioi.analysis.models.Report;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class CsvAnalysis {
    private static final Logger logger = Logger.getLogger("CsvAnalysis");

    public static void main(String[] args) {
        try {
            String project = "bookkeeper";
            List<Report> reports = new ArrayList<>();
            int numberOfReleases = 6;
            for (int i = 2; i <= numberOfReleases; i++) {
                Analysis analysis = new Analysis(project, i);
                for (AnalysisType.Classifiers classifier : AnalysisType.Classifiers.values()) {
                    analysis.buildClassifier(classifier);
                    for (AnalysisType.FeatureSelection featureSelection : AnalysisType.FeatureSelection.values()) {
//                        analysis.applyFeatureSelection(featureSelection);
                        for (AnalysisType.Sampling sampling : AnalysisType.Sampling.values()) {
//                            analysis.applySampling(sampling);
                            for (AnalysisType.CostSensitive costSensitive : AnalysisType.CostSensitive.values()) {
//                                        analysis.applyCostSensitive(costSensitive)
                                Report report = analysis.generateReport();
                                report.setProperties(classifier, featureSelection, sampling, costSensitive);
                                reports.add(report);
                            }
                        }
                    }
                }
            }
            ReportHandler report = new ReportHandler(project, reports);
            report.writeToFile();
        } catch (Exception e) {
            logger.severe(e.getMessage());
        }
    }
}
