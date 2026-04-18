package com.flowboard.list.dto;

import lombok.Data;
import java.util.List;

@Data
public class ReorderRequest {
    private List<Long> listIds; // IDs in new order
}
