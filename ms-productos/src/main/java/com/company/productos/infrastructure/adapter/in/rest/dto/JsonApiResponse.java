package com.company.productos.infrastructure.adapter.in.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record JsonApiResponse<T>(
    T data,
    List<T> dataList,
    Meta meta,
    List<ApiError> errors
) {
    public record Meta(long total, int page, int size, String correlationId) {}
    public record ApiError(String status, String code, String title, String detail) {}

    public static <T> JsonApiResponse<T> of(T data)                    { return new JsonApiResponse<>(data, null, null, null); }
    public static <T> JsonApiResponse<T> ofList(List<T> items, Meta m) { return new JsonApiResponse<>(null, items, m, null); }
    public static <T> JsonApiResponse<T> error(List<ApiError> errors)  { return new JsonApiResponse<>(null, null, null, errors); }
}
