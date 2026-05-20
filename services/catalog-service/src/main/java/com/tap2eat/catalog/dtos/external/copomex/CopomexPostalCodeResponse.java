package com.tap2eat.catalog.dtos.external.copomex;

import java.util.List;

public record CopomexPostalCodeResponse(
        Boolean error,
        Integer code_error,
        String error_message,
        CopomexPostalCodeData response
) {

    public boolean hasError() {
        return Boolean.TRUE.equals(error);
    }

    public record CopomexPostalCodeData(
            String cp,
            List<String> asentamiento,
            String tipo_asentamiento,
            String municipio,
            String estado,
            String ciudad,
            String pais
    ) {
    }
}