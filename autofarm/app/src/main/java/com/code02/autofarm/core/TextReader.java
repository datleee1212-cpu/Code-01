package com.code02.autofarm.core;

/** Đọc chữ trong một vùng ảnh (máy thật: ML Kit; giả lập: OCR giả có nhiễu). */
public interface TextReader {
    /** digits = true: vùng chỉ chứa số, có thể thử thêm cách xử lý ảnh khác. Trả về null nếu không đọc được. */
    String read(Img img, Box region, boolean digits);
}
