package com.solidoa.attendance.form;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class RepairForm {
    private LocalDate repairDate;
    private String repairType;
    private LocalDateTime repairTime;
    private String reason;
    private String attachments;
}