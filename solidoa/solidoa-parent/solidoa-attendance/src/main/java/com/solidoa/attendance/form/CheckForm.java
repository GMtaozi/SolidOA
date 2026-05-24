package com.solidoa.attendance.form;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CheckForm {
    private String location;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String deviceType;
}