package com.example.An_Yang.service;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.io.ClassPathResource;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class CsvUtils {

    /** UTF-8로 먼저 읽고, 헤더에 깨짐문자(�)가 있으면 CP949로 재시도 */
    static Iterable<CSVRecord> read(String path) {
        try {
            var parsed = parse(path, StandardCharsets.UTF_8);
            if (hasMojibake(parsed.getHeaderMap().keySet())) {
                closeQuiet(parsed);
                parsed = parse(path, Charset.forName("MS949")); // CP949
            }
            // Records를 미리 리스트로 복제해서 리더/파서 닫아도 사용 가능
            List<CSVRecord> all = parsed.getRecords();
            closeQuiet(parsed);
            return all;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static CSVParser parse(String path, Charset cs) throws IOException {
        Reader r = openReader(path, cs);
        return CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withIgnoreHeaderCase()
                .withTrim()
                .parse(r);
    }

    private static void closeQuiet(CSVParser p){
        try { if (p != null) p.close(); } catch (Exception ignored) {}
    }

    private static boolean hasMojibake(Set<String> keys){
        for (String k : keys) {
            if (k != null && (k.indexOf('\uFFFD') >= 0 || k.contains("��"))) return true;
        }
        return false;
    }

    static Map<String,String> row(CSVRecord rec){
        Map<String,String> m = new LinkedHashMap<>();
        rec.toMap().forEach((k,v) -> {
            String kk = sanitize(k);
            String vv = sanitize(v);
            m.put(kk, vv);
        });
        return m;
    }

    /** 후보 키들 중 실제 존재하는 첫 키의 값을 반환(대소문자 무시) */
    static String get(Map<String,String> m, String... keys) {
        for (String k : keys) {
            for (String key : m.keySet()) {
                if (key.equalsIgnoreCase(k)) return m.get(key);
            }
        }
        return "";
    }

    private static Reader openReader(String path, Charset cs) throws IOException {
        if (path.startsWith("classpath:")) {
            String p = path.substring("classpath:".length());
            return new InputStreamReader(new ClassPathResource(p).getInputStream(), cs);
        }
        return Files.newBufferedReader(Paths.get(path), cs);
    }

    /** 값 정리: trim + 선행 BOM 제거 + 양끝 큰따옴표 제거 */
    private static String sanitize(String s){
        if (s == null) return "";
        s = s.trim();
        if (!s.isEmpty() && s.charAt(0) == '\uFEFF') s = s.substring(1); // BOM
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length()-1);
        }
        return s;
    }

    static double toD(String s){
        try { return Double.parseDouble(s.replace(",", "").replace("\"","")); } catch (Exception e) { return 0d; }
    }
    static int toI(String s){
        try { return (int)Math.round(Double.parseDouble(s.replace(",", "").replace("\"",""))); } catch (Exception e) { return 0; }
    }

    /** 숫자만 남기기/왼쪽 n자리 유틸 (다른 서비스에서도 쓰려고 public) */
    public static String digits(String s){ return s == null ? "" : s.replaceAll("\\D+",""); }
    public static String left(String s,int n){ return s == null ? "" : (s.length()<=n ? s : s.substring(0,n)); }
}
