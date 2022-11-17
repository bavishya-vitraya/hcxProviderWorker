package hcxprovider.hcxproviderconsumer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProcedureMethod {
    private int id;
    private int procedureId;
    private String procedureMethodName;
    private String procedureMethodDisplayName;
    private String procedureCode;
    private String procedureNameInsurer;
    private boolean enabled;
    private Date createTime;
    private Date updateTime;
    private String procedureMethodCode;
}
