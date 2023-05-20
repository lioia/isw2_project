package it.uniroma2.alessandrolioi.apps;

import it.uniroma2.alessandrolioi.analysis.Analysis;
import it.uniroma2.alessandrolioi.analysis.exceptions.ArffException;
import it.uniroma2.alessandrolioi.analysis.exceptions.CsvException;

import java.util.logging.Logger;

public class CsvAnalysis {
    private static final Logger logger = Logger.getLogger("CsvAnalysis");

    public static void main(String[] args) {
        try {
            Analysis analysis = new Analysis("bookkeeper");
            analysis.csvToArff(2);
        } catch (CsvException | ArffException e) {
            logger.severe(e.getMessage());
        }
    }
}
