package it.uniroma2.alessandrolioi.apps;

import it.uniroma2.alessandrolioi.analysis.AnalysisBuilder;
import it.uniroma2.alessandrolioi.analysis.exceptions.ArffException;
import it.uniroma2.alessandrolioi.analysis.exceptions.ClassifierException;
import it.uniroma2.alessandrolioi.analysis.exceptions.CsvException;
import it.uniroma2.alessandrolioi.analysis.exceptions.EvaluationException;
import it.uniroma2.alessandrolioi.analysis.models.AnalysisType;
import it.uniroma2.alessandrolioi.analysis.models.Report;

import java.util.logging.Logger;

public class CsvAnalysis {
    private static final Logger logger = Logger.getLogger("CsvAnalysis");

    public static void main(String[] args) {
        try {
            int numberOfReleases = 6;
            for (int i = 2; i <= numberOfReleases; i++) {
                for (AnalysisType.Classifiers classifiers : AnalysisType.Classifiers.values()) {
                    Report report = new AnalysisBuilder("bookkeeper", i)
                            .loadCsv()
                            .loadInstances()
                            .buildClassifier(classifiers)
                            // ... apply filters ... (feature selection, sampling, cost sensitive)
                            .generateReport();
                    logger.info(report.toString());
                }
            }
        } catch (CsvException | ArffException | ClassifierException | EvaluationException e) {
            e.printStackTrace();
            logger.severe(e.getMessage());
        }
    }
}
