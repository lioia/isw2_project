package it.uniroma2.alessandrolioi.analysis.boundaries;

import it.uniroma2.alessandrolioi.analysis.controllers.ReportController;
import it.uniroma2.alessandrolioi.analysis.exceptions.ReportWriterException;
import it.uniroma2.alessandrolioi.analysis.models.Report;

import java.util.List;

public class ReportHandler {
    private final String project;
    private final List<Report> reports;

    public ReportHandler(String project, List<Report> reports) {
        this.project = project;
        this.reports = reports;
    }

    public void writeToFile() throws ReportWriterException {
        ReportController controller = new ReportController();
        controller.writeReports(project, reports);
    }
}
