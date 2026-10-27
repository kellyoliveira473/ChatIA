package org.example.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

public class DocumentExtractor {

    public static String extrairTexto(File arquivo) {
        String nomeArquivo = arquivo.getName().toLowerCase();
        try {
            if (nomeArquivo.endsWith(".txt")) {
                return Files.readString(arquivo.toPath(), StandardCharsets.UTF_8);
            }
            else if (nomeArquivo.endsWith(".pdf")) {
                try (PDDocument document = Loader.loadPDF(arquivo)) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    return stripper.getText(document);
                }
            }
            else if (nomeArquivo.endsWith(".docx")) {
                try (FileInputStream fis = new FileInputStream(arquivo);
                     XWPFDocument docx = new XWPFDocument(fis)) {
                    StringBuilder sb = new StringBuilder();
                    for (XWPFParagraph p : docx.getParagraphs()) {
                        sb.append(p.getText()).append("\n");
                    }
                    return sb.toString();
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao extrair texto do arquivo: " + e.getMessage());
        }
        return "";
    }
}