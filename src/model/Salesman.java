package model;
public class Salesman {
    public final String docType;
    public final String docNumber;
    public final String firstNames;
    public final String lastNames;
    public Salesman(String docType, String docNumber, String firstNames, String lastNames) {
        this.docType = docType; this.docNumber = docNumber;
        this.firstNames = firstNames; this.lastNames = lastNames;
    }
    public String key() { return docType + ";" + docNumber; }
    public String fullName() { return firstNames + " " + lastNames; }
}
