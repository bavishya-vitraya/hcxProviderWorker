package hcxprovider.hcxproviderconsumer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Procedure {
    private long id;
    private Date createTime;
    private boolean enabled;
    private String description;
    private int departmentId;
    private Date updateTime;
    private String name;
    private String procedureCode;
}
