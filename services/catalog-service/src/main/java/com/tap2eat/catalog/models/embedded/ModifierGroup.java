package com.tap2eat.catalog.models.embedded;

import com.tap2eat.catalog.models.enums.SelectionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModifierGroup {

    private String id;
    private String name;
    private SelectionType selectionType;
    private Boolean required = false;
    private Integer minSelections = 0;
    private Integer maxSelections = 1;
    private Integer displayOrder;
    private Boolean isActive = true;

    @Builder.Default
    private List<ModifierOption> options = new ArrayList<>();
}