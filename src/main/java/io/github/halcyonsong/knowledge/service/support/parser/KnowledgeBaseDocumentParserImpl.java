package io.github.halcyonsong.knowledge.service.support.parser;

import io.github.halcyonsong.common.enums.ResultCodeEnum;
import io.github.halcyonsong.common.exception.BusinessException;
import io.github.halcyonsong.knowledge.common.constants.KnowledgeMetadataConstants;
import io.github.halcyonsong.knowledge.common.util.TextChunkUtil;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.sl.usermodel.Shape;
import org.apache.poi.sl.usermodel.Slide;
import org.apache.poi.sl.usermodel.SlideShow;
import org.apache.poi.sl.usermodel.SlideShowFactory;
import org.apache.poi.sl.usermodel.TextShape;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class KnowledgeBaseDocumentParserImpl implements KnowledgeBaseDocumentParser {


    @Override
    public List<Document> parse(String taskId,
                                String knowledgeBaseId,
                                byte[] fileBytes,
                                String fileName,
                                String fileType) throws Exception {
        return switch (fileType) {
            case "txt" -> buildTxtDocuments(taskId, knowledgeBaseId, fileBytes, fileName, fileType);
            case "pdf" -> buildPdfDocuments(taskId, knowledgeBaseId, fileBytes, fileName, fileType);
            case "ppt", "pptx" -> buildPowerPointDocuments(taskId, knowledgeBaseId, fileBytes, fileName, fileType);
            default -> throw new IllegalArgumentException("当前不支持的文件类型: " + fileType);
        };
    }

    // 解析txt文档
    private List<Document> buildTxtDocuments(String taskId,
                                             String knowledgeBaseId,
                                             byte[] fileBytes,
                                             String fileName,
                                             String fileType) {
        String content = new String(fileBytes, StandardCharsets.UTF_8);
        List<String> chunks = TextChunkUtil.splitText(content, 800, 100);

        List<Document> documentList = new ArrayList<>();
        for (int index = 0; index < chunks.size(); index++) {
            String chunkText = chunks.get(index);
            if (!StringUtils.hasText(chunkText)) {
                continue;
            }

            Document document = Document.builder()
                    .text(chunkText)
                    .metadata(mergeMetadata(null, taskId, knowledgeBaseId, fileName, fileType, index))
                    .build();

            documentList.add(document);
        }

        return documentList;
    }

    // 解析pdf文档
    private List<Document> buildPdfDocuments(String taskId,
                                             String knowledgeBaseId,
                                             byte[] fileBytes,
                                             String fileName,
                                             String fileType) {
        try {
            try (PDDocument pdfDocument = Loader.loadPDF(fileBytes)) {
                PDFTextStripper textStripper = new PDFTextStripper();
                int totalPages = pdfDocument.getNumberOfPages();

                List<Document> documentList = new ArrayList<>();
                // 当前仅按页解析，后续可考虑按段落语义解析等
                for (int pageIndex = 1; pageIndex <= totalPages; pageIndex++) {
                    textStripper.setStartPage(pageIndex);
                    textStripper.setEndPage(pageIndex);

                    String pageText = textStripper.getText(pdfDocument);
                    if (!StringUtils.hasText(pageText)) {
                        continue;
                    }

                    Map<String, Object> metadata = mergeMetadata(
                            null,
                            taskId,
                            knowledgeBaseId,
                            fileName,
                            fileType,
                            pageIndex - 1
                    );
                    metadata.put(KnowledgeMetadataConstants.PAGE_NUMBER, pageIndex);

                    Document document = Document.builder()
                            .text(pageText.trim())
                            .metadata(metadata)
                            .build();

                    documentList.add(document);
                }

                if (documentList.isEmpty()) {
                    throw new BusinessException(
                            ResultCodeEnum.DOCUMENT_PARSE_ERROR.getCode(),
                            "PDF 文档未解析到可用文本内容，请检查文件是否为图片型 PDF 或内容不可提取"
                    );
                }

                return documentList;
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BusinessException(
                    ResultCodeEnum.DOCUMENT_PARSE_ERROR.getCode(),
                    "PDF 文档读取失败，请检查文件是否损坏或格式异常"
            );
        } catch (Exception exception) {
            throw new BusinessException(
                    ResultCodeEnum.DOCUMENT_PARSE_ERROR.getCode(),
                    "PDF 文档解析失败，当前文件可能包含复杂内容或不可提取文本，请尝试转换为普通 PDF 或 TXT 后重试"
            );
        }
    }


    private List<Document> buildPowerPointDocuments(String taskId,
                                                    String knowledgeBaseId,
                                                    byte[] fileBytes,
                                                    String fileName,
                                                    String fileType) throws Exception {
        List<Document> documentList = new ArrayList<>();

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(fileBytes);
             SlideShow<?, ?> slideShow = SlideShowFactory.create(inputStream)) {

            List<? extends Slide<?, ?>> slides = slideShow.getSlides();
            for (int index = 0; index < slides.size(); index++) {
                Slide<?, ?> slide = slides.get(index);
                String slideText = extractSlideText(slide);

                if (!StringUtils.hasText(slideText)) {
                    continue;
                }

                Map<String, Object> metadata = mergeMetadata(
                        null,
                        taskId,
                        knowledgeBaseId,
                        fileName,
                        fileType,
                        index
                );
                metadata.put(KnowledgeMetadataConstants.SLIDE_NUMBER, index + 1);

                Document document = Document.builder()
                        .text(slideText)
                        .metadata(metadata)
                        .build();

                documentList.add(document);
            }
        }

        return documentList;
    }

    private String extractSlideText(Slide<?, ?> slide) {
        StringBuilder textBuilder = new StringBuilder();

        for (Shape<?, ?> shape : slide.getShapes()) {
            if (shape instanceof TextShape<?, ?> textShape) {
                String text = textShape.getText();
                if (StringUtils.hasText(text)) {
                    if (!textBuilder.isEmpty()) {
                        textBuilder.append('\n');
                    }
                    textBuilder.append(text.trim());
                }
            }
        }

        return textBuilder.toString().trim();
    }

    private Map<String, Object> mergeMetadata(Map<String, Object> originalMetadata,
                                              String taskId,
                                              String knowledgeBaseId,
                                              String fileName,
                                              String fileType,
                                              int chunkIndex) {
        Map<String, Object> metadata = new HashMap<>();
        if (originalMetadata != null) {
            metadata.putAll(originalMetadata);
        }

        metadata.put(KnowledgeMetadataConstants.TASK_ID, taskId);
        metadata.put(KnowledgeMetadataConstants.KNOWLEDGE_BASE_ID, knowledgeBaseId);
        metadata.put(KnowledgeMetadataConstants.FILE_NAME, fileName);
        metadata.put(KnowledgeMetadataConstants.FILE_TYPE, fileType);
        metadata.put(KnowledgeMetadataConstants.CHUNK_INDEX, chunkIndex);
        metadata.put(KnowledgeMetadataConstants.UPLOAD_TIME, LocalDateTime.now().toString());

        return metadata;
    }
}