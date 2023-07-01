package it.uniroma2.alessandrolioi.analysis.boundaries;

import it.uniroma2.alessandrolioi.analysis.controllers.ConverterController;
import it.uniroma2.alessandrolioi.analysis.controllers.ReaderController;
import it.uniroma2.alessandrolioi.analysis.exceptions.ArffException;
import it.uniroma2.alessandrolioi.analysis.exceptions.CsvException;
import it.uniroma2.alessandrolioi.analysis.models.CsvEntry;

import java.util.*;

public class CsvToArff {
    private final String project;
    private final Map<Integer, List<CsvEntry>> oracleEntries;

    public CsvToArff(String project) throws CsvException {
        this.project = project;
        ReaderController controller = new ReaderController();
        this.oracleEntries = controller.loadCsv(project, "oracle.csv");
    }

    public int getTotalReleases() throws CsvException {
        Optional<Integer> total = oracleEntries.keySet().stream().max(Comparator.naturalOrder());
        if (total.isEmpty()) throw new CsvException("Could not load total releases");
        return total.get();
    }

    public void convertToArff() throws CsvException, ArffException {
        ReaderController readerController = new ReaderController();
        ConverterController converterController = new ConverterController();
        for (int i = 2; i < getTotalReleases(); i++) {
            Map<Integer, List<CsvEntry>> entries = readerController.loadCsv(project, "%d.csv".formatted(i));
            converterController.writeToArff(project, oracleEntries, entries, i);
        }
    }
}
