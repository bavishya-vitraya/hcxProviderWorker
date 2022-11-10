package hcxprovider.hcxproviderconsumer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProcedureMethod {
    private BigDecimal id;
    private Integer procedureId;
    private String procedureMethodName;
    private String procedureMethodDisplayName;
    private String procedureCode;
    private String procedureNameInsurer;
    private boolean enabled;
    private String createTime;
    private String updateTime;
    private String procedureMethodCode;
}
