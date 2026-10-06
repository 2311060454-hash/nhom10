package vn.shop.common;

import java.util.List;

/** Hợp đồng phân trang ổn định, không phụ thuộc cách Jackson serialize PageImpl. */
public record PageResult<T>(List<T> content, long totalElements, int totalPages, int number, int size, boolean last) {}
