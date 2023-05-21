package it.uniroma2.alessandrolioi.apps;

import it.uniroma2.alessandrolioi.analysis.AnalysisBuilder;
import it.uniroma2.alessandrolioi.analysis.exceptions.ArffException;
import it.uniroma2.alessandrolioi.analysis.exceptions.ClassifierException;
import it.uniroma2.alessandrolioi.analysis.exceptions.CsvException;
import it.uniroma2.alessandrolioi.analysis.exceptions.EvaluationException;
import it.uniroma2.alessandrolioi.analysis.models.AnalysisType;

import java.util.logging.Logger;

public class CsvAnalysis {
    private static final Logger logger = Logger.getLogger("CsvAnalysis");

    public static void main(String[] args) {
        try {
            new AnalysisBuilder("bookkeeper", 2)
                    .loadCsv()
                    .loadInstances()
                    .buildClasisifier(AnalysisType.Classifiers.RANDOM_FOREST)
                    // ... apply filters ... (feature selection, sampling, cost sensitive)
                    .evaluate();
        } catch (CsvException | ArffException | ClassifierException | EvaluationException e) {
            e.printStackTrace();
            logger.severe(e.getMessage());
        }
    }
}
