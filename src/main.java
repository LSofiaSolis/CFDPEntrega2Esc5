import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import model.*;

public class main {
    private static final Path INPUT = Paths.get("input");
    private static final Path OUTPUT = Paths.get("output");
    private static final Path PRODUCTS_FILE = INPUT.resolve("products.csv");
    private static final Path SALESMEN_INFO_FILE = INPUT.resolve("salesmen_info.csv");
    private static final Path SALES_DIR = INPUT.resolve("sales");
    private static final Path SELLERS_REPORT = OUTPUT.resolve("sellers_report.csv");
    private static final Path PRODUCTS_QTY_REPORT = OUTPUT.resolve("products_by_quantity.csv");
    private static final Path ERR_LOG = OUTPUT.resolve("processing_errors.log");

    public static void main(String[] args) {
        try {
            Files.createDirectories(OUTPUT);
            Map<String, Product> products = loadProducts(PRODUCTS_FILE);
            Map<String, Salesman> salesmen = loadSalesmen(SALESMEN_INFO_FILE);
            Map<String, Double> moneyBySeller = new HashMap<>();
            Map<String, Long> qtyByProductId = new HashMap<>();
            List<String> errors = new ArrayList<>();
            if (Files.isDirectory(SALES_DIR)) {
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(SALES_DIR, "*")) {
                    for (Path f : ds) {
                        if (Files.isRegularFile(f)) {
                            processSalesFile(f, products, salesmen, moneyBySeller, qtyByProductId, errors);
                        }
                    }
                }
            }
            List<Map.Entry<String, Double>> sellersSorted = moneyBySeller.entrySet().stream()
                .sorted((a,b)->{
                    int cmp = Double.compare(b.getValue(), a.getValue());
                    if (cmp != 0) return cmp;
                    Salesman sa = salesmen.getOrDefault(a.getKey(), parseMinimalSalesman(a.getKey()));
                    Salesman sb = salesmen.getOrDefault(b.getKey(), parseMinimalSalesman(b.getKey()));
                    return sa.fullName().compareTo(sb.fullName());
                })
                .collect(Collectors.toList());
            try (BufferedWriter bw = Files.newBufferedWriter(SELLERS_REPORT)) {
                for (Map.Entry<String, Double> e : sellersSorted) {
                    String key = e.getKey();
                    Salesman s = salesmen.getOrDefault(key, parseMinimalSalesman(key));
                    bw.write(s.fullName() + ";" + String.format(Locale.US, "%.2f", e.getValue()));
                    bw.newLine();
                }
            }
            List<Map.Entry<String, Long>> prodsSorted = qtyByProductId.entrySet().stream()
                .sorted((a,b)->{
                    int cmp = Long.compare(b.getValue(), a.getValue());
                    if (cmp != 0) return cmp;
                    Product pa = products.get(a.getKey());
                    Product pb = products.get(b.getKey());
                    String na = pa != null ? pa.name : a.getKey();
                    String nb = pb != null ? pb.name : b.getKey();
                    return na.compareTo(nb);
                })
                .collect(Collectors.toList());
            try (BufferedWriter bw = Files.newBufferedWriter(PRODUCTS_QTY_REPORT)) {
                for (Map.Entry<String, Long> e : prodsSorted) {
                    Product p = products.get(e.getKey());
                    if (p != null) {
                        bw.write(p.name + ";" + String.format(Locale.US,"%.2f", p.price) + ";" + e.getValue());
                        bw.newLine();
                    }
                }
            }
            if (!errors.isEmpty()) {
                try (BufferedWriter bw = Files.newBufferedWriter(ERR_LOG)) {
                    for (String line : errors) { bw.write(line); bw.newLine(); }
                }
            }
            System.out.println("OK");
        } catch (Exception e) {
            System.err.println("ERROR");
        }
    }

    private static Map<String, Product> loadProducts(Path f) throws IOException {
        Map<String, Product> map = new HashMap<>();
        try (BufferedReader br = Files.newBufferedReader(f)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] t = line.split(";");
                if (t.length >= 3) {
                    String id = t[0].trim();
                    String name = t[1].trim();
                    double price = Double.parseDouble(t[2].trim().replace(",", "."));
                    if (price >= 0) {
                        map.put(id, new Product(id, name, price));
                    }
                }
            }
        }
        return map;
    }

    private static Map<String, Salesman> loadSalesmen(Path f) throws IOException {
        Map<String, Salesman> map = new HashMap<>();
        try (BufferedReader br = Files.newBufferedReader(f)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] t = line.split(";");
                if (t.length >= 4) {
                    Salesman s = new Salesman(t[0].trim(), t[1].trim(), t[2].trim(), t[3].trim());
                    map.put(s.key(), s);
                }
            }
        }
        return map;
    }

    private static void processSalesFile(Path f,
                                         Map<String, Product> products,
                                         Map<String, Salesman> salesmen,
                                         Map<String, Double> moneyBySeller,
                                         Map<String, Long> qtyByProductId,
                                         List<String> errors) {
        try (BufferedReader br = Files.newBufferedReader(f)) {
            String header = br.readLine();
            if (header == null) { errors.add(f + ": vacio"); return; }
            String[] h = header.split(";");
            if (h.length < 2) { errors.add(f + ": cabecera invalida"); return; }
            String sellerKey = h[0].trim() + ";" + h[1].trim();
            moneyBySeller.putIfAbsent(sellerKey, 0.0);
            String line;
            int ln = 1;
            while ((line = br.readLine()) != null) {
                ln++;
                String[] t = line.split(";");
                if (t.length < 2) { errors.add(f + ": linea " + ln + " invalida"); continue; }
                String prodId = t[0].trim();
                long qty;
                try { qty = Long.parseLong(t[1].trim()); }
                catch (Exception ex) { errors.add(f + ": cantidad invalida en linea " + ln); continue; }
                if (qty <= 0) { errors.add(f + ": cantidad <=0 en linea " + ln); continue; }
                Product p = products.get(prodId);
                if (p == null) { errors.add(f + ": producto no existe " + prodId + " linea " + ln); continue; }
                qtyByProductId.merge(prodId, qty, Long::sum);
                moneyBySeller.compute(sellerKey, (k,v)-> v + qty * p.price);
            }
        } catch (Exception e) {
            errors.add("Error leyendo " + f);
        }
    }

    private static Salesman parseMinimalSalesman(String key) {
        String[] t = key.split(";");
        String n = (t.length==2 ? t[0]+" "+t[1] : key);
        return new Salesman(t.length>0?t[0]:"?", t.length>1?t[1]:"?", n, "");
    }
}
