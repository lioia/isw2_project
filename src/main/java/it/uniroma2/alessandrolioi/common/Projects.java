package it.uniroma2.alessandrolioi.common;

public class Projects {
    private Projects() {
        throw new IllegalStateException("Utility class");
    }

    private static final String[] names = new String[]{"bookkeeper", "avro"};

    public static String[] names() {
        return names;
    }
}
