package com.absensi.absensi_app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpdResponse {
    private Long id;
    private String kode;
    private String nama;
    private String alamat;
    private Double latitudeKantor;
    private Double longitudeKantor;
    private Integer radiusAbsen;
}