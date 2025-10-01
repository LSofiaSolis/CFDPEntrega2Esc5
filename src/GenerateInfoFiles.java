import java.io.*;
import java.nio.file.*;
import java.util.*;
import model.*;

public class GenerateInfoFiles {
    private static final Path INPUT = Paths.get("input");
    private static final Path SALES_DIR = INPUT.resolve("sales");

    public static void main(String[] args) {
        try {
            Files.createDirectories(INPUT);
            Files.createDirectories(SALES_DIR);
            int productsCount = 20;
            int salesmanCount = 10;
            int randomSalesCountPerFile = 25;
            List<Product> products = createProductsFile(productsCount);
            List<Salesman> salesmen = createSalesManInfoFile(salesmanCount);
            Random rnd = new Random();
            for (int i = 0; i < salesmen.size(); i++) {
                Salesman s = salesmen.get(i);
                writeSalesFile(s.docType, s.docNumber, products, randomSalesCountPerFile, rnd);
                if (i % 2 == 0) {
                    writeSalesFile(s.docType, s.docNumber, products, randomSalesCountPerFile, rnd);
                }
            }
            System.out.println("OK");
        } catch (Exception e) {
            System.err.println("ERROR");
        }
    }

    public static List<Product> createProductsFile(int productsCount) throws IOException {
        Path f = Paths.get("input", "products.csv");
        Files.createDirectories(f.getParent());
        Random rnd = new Random();
        List<Product> list = new ArrayList<>();
        try (BufferedWriter bw = Files.newBufferedWriter(f)) {
            for (int i = 1; i <= productsCount; i++) {
                String id = String.format("P%03d", i);
                String name = "Producto_" + i;
                double price = 5.0 + rnd.nextInt(195) + rnd.nextDouble();
                list.add(new Product(id, name, price));
                bw.write(id + ";" + name + ";" + String.format(java.util.Locale.US, "%.2f", price));
                bw.newLine();
            }
        }
        return list;
    }

    public static List<Salesman> createSalesManInfoFile(int salesmanCount) throws IOException {
        Path f = Paths.get("input", "salesmen_info.csv");
        Files.createDirectories(f.getParent());
        List<String> names = Arrays.asList("Camila","Laura","Sofia","Valentina","Maria","Juan","Pedro","Luis","Andres","Tatiana","Jorge","Leidy","Daniel","Joshua","Johana");
        List<String> lasts = Arrays.asList("Gomez","Solis","Ramirez","Pinto","Arenas","Saavedra","Diaz","Roa","Lopez","Martinez","Cortes","Vargas");
        Random rnd = new Random();
        List<Salesman> list = new ArrayList<>();
        try (BufferedWriter bw = Files.newBufferedWriter(f)) {
            for (int i = 0; i < salesmanCount; i++) {
                String docType = (rnd.nextBoolean() ? "CC" : "TI");
                String docNum = String.valueOf(10000000 + rnd.nextInt(90000000));
                String firsts = names.get(rnd.nextInt(names.size()));
                String lastsName = lasts.get(rnd.nextInt(lasts.size()));
                Salesman s = new Salesman(docType, docNum, firsts, lastsName);
                list.add(s);
                bw.write(docType + ";" + docNum + ";" + s.firstNames + ";" + s.lastNames);
                bw.newLine();
            }
        }
        return list;
    }

    private static Path writeSalesFile(String docType, String docNumber, List<Product> products, int randomSalesCount, Random rnd) throws IOException {
        String fileName = "ventas_" + docType + "_" + docNumber + "_" + Math.abs(rnd.nextInt()) + ".txt";
        Path f = Paths.get("input", "sales", fileName);
        try (BufferedWriter bw = Files.newBufferedWriter(f)) {
            bw.write(docType + ";" + docNumber); bw.newLine();
            for (int i = 0; i < randomSalesCount; i++) {
                Product p = products.get(rnd.nextInt(products.size()));
                int qty = 1 + rnd.nextInt(7);
                bw.write(p.id + ";" + qty); bw.newLine();
            }
        }
        return f;
    }
}
