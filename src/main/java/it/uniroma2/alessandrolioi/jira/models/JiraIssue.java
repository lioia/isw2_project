package it.uniroma2.alessandrolioi.jira.models;

import java.time.LocalDate;
import java.util.List;

public final class JiraIssue {
    // JSON named fields
    public static final String KEY_FIELD = "key";
    public static final String VERSIONS_FIELD = "versions";
    public static final String FIX_VERSIONS_FIELD = "fixVersions";
    public static final String RESOLUTION_DATE_FIELD = "resolutiondate";
    public static final String CREATED_FIELD = "created";
    private static final String[] FIELDS = {KEY_FIELD, VERSIONS_FIELD, FIX_VERSIONS_FIELD, RESOLUTION_DATE_FIELD, CREATED_FIELD};

    private final String key;
    private final LocalDate resolution;
    private final LocalDate created;

    // Corresponds to the `version` field; sorted list, but it can be empty
    private final List<LocalDate> affectedVersionsDates;
    private int ivIndex;
    private int ovIndex;
    private int fvIndex;

    public JiraIssue(String key, LocalDate resolution, LocalDate created, List<LocalDate> affectedVersionsDates,
                     int ivIndex, int ovIndex, int fvIndex) {
        this.key = key;
        this.resolution = resolution;
        this.created = created;
        this.affectedVersionsDates = affectedVersionsDates;
        this.ivIndex = ivIndex;
        this.ovIndex = ovIndex;
        this.fvIndex = fvIndex;
    }

    public JiraIssue(String key, LocalDate resolution, LocalDate created, List<LocalDate> affectedVersionsDates) {
        this(key, resolution, created, affectedVersionsDates, -1, -1, -1);
    }

    public double calculateProportion() {
        return ((double) getFvMinusIv()) / getFvMinusOv();
    }

    public int getFvMinusOv() {
        // FV-OV is set to 1 if the opening and fix are the same version (Proportion Paper page 13.(d).(iii))
        return Math.max(fvIndex - ovIndex, 1);
    }

    public int getFvMinusIv() {
        return fvIndex - ivIndex;
    }

    public String getKey() {
        return key;
    }

    public LocalDate getResolution() {
        return resolution;
    }

    public LocalDate getCreated() {
        return created;
    }

    public List<LocalDate> getAffectedVersionsDates() {
        return affectedVersionsDates;
    }

    public int getIvIndex() {
        return ivIndex;
    }

    public void setIvIndex(int ivIndex) {
        this.ivIndex = ivIndex;
    }

    public int getOvIndex() {
        return ovIndex;
    }

    public void setOvIndex(int ovIndex) {
        this.ovIndex = ovIndex;
    }

    public int getFvIndex() {
        return fvIndex;
    }

    public void setFvIndex(int fvIndex) {
        this.fvIndex = fvIndex;
    }

    public static String getFields() {
        return String.join(",", FIELDS);
    }
}
