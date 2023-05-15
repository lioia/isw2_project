package it.uniroma2.alessandrolioi.analysis;

import it.uniroma2.alessandrolioi.analysis.controllers.CsvReaderController;
import it.uniroma2.alessandrolioi.analysis.exceptions.CsvException;
import it.uniroma2.alessandrolioi.analysis.models.CsvEntry;

import java.util.List;
import java.util.Map;

public class Analysis {
    private final Map<Integer, List<CsvEntry>> entries;

    public Analysis(String file) throws CsvException {
        CsvReaderController controller = new CsvReaderController();
        this.entries = controller.readCsv(file);
    }

    public Map<Integer, List<CsvEntry>> entries() {
        return entries;
    }
}
