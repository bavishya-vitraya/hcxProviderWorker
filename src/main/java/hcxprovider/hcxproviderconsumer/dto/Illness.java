package hcxprovider.hcxproviderconsumer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Illness {
    private int id;
    private int illnessCategoryId;
    private String illnessName;
    private String defaultICDCode;
    private String illnessDescription;
    private String relatedDisease;
    private String procedures;
    private String illnessCode;
    private boolean active;
    private String createTime;
    private String updateTime;
}
