package com.flowboard.list.dto;

import lombok.Data;

@Data
public class ListRequest {
    private Long boardId;
    private String name;
    private String color;
}
