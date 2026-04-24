package com.tap2eat.catalog.constants;

public final class CatalogConstraints {

    private CatalogConstraints() {
    }

    public static final int MAX_NAME_LENGTH = 120;
    public static final int MAX_DESCRIPTION_LENGTH = 1000;
    public static final int MAX_TAGS_PER_PRODUCT = 20;
    public static final int MAX_MODIFIER_GROUPS_PER_PRODUCT = 10;
    public static final int MAX_OPTIONS_PER_MODIFIER_GROUP = 20;
}