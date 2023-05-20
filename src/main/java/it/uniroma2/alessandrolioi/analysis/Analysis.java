package it.uniroma2.alessandrolioi.analysis;

import it.uniroma2.alessandrolioi.analysis.controllers.ConverterController;
import it.uniroma2.alessandrolioi.analysis.exceptions.ArffException;
import it.uniroma2.alessandrolioi.analysis.exceptions.CsvException;
import it.uniroma2.alessandrolioi.analysis.models.CsvEntry;
import it.uniroma2.alessandrolioi.common.Metric;
import weka.core.Instances;

import java.util.List;
import java.util.Map;

public class Analysis {
    private final String project;

    public Analysis(String project) throws CsvException {
        this.project = project;
    }

    public void csvToArff(int lastRelease) throws CsvException, ArffException {
        ConverterController controller = new ConverterController();
        String file = "dataset/%s/%d.csv".formatted(project, lastRelease);
        Map<Integer, List<CsvEntry>> entries = controller.readCsv(file);
        controller.writeToArff(project, entries, lastRelease);
    }

//    public void applyWalkForward(int testingRelease) throws ArffException {
//        ConverterController controller = new ConverterController();
//        controller.writeToArff(project, entries, fields, testingRelease);
//        Instances training = controller.loadInstance(project, "training", testingRelease);
//        Instances testing = controller.loadInstance(project, "testing", testingRelease);
//    }
}
