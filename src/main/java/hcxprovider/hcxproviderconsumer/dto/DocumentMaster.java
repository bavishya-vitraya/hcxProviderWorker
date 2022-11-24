package hcxprovider.hcxproviderconsumer.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentMaster {
    private long parentTableId;
    private String documentType;
    private String fileName;
    private String storageFileName;
    private String fileType;
    private boolean fileSupported;
    private String note;
    private int documentStatus;
}
