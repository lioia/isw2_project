package it.uniroma2.alessandrolioi.apps;

import java.io.IOException;

public class CompleteApp {
    public static void main(String[] args) throws IOException {
        DatasetGeneratorApp.main(args);
        AnalysisApp.main(args);
        ChartsApp.main(args);
    }
}
