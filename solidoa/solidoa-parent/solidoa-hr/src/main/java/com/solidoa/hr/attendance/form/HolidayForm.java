package com.solidoa.hr.attendance.form;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

/**
 * 节假日表单
 */
@Data
public class HolidayForm {

    private Long id;

    /**
     * 节假日日期
     */
    @NotNull(message = "节假日日期不能为空")
    private LocalDate holidayDate;

    /**
     * 节假日名称
     */
    @NotBlank(message = "节假日名称不能为空")
    private String holidayName;

    /**
     * 类型:HOLIDAY节假日,WORKDAY调休上班
     */
    @NotBlank(message = "节假日类型不能为空")
    @Pattern(regexp = "^(HOLIDAY|WORKDAY)$", message = "节假日类型只能是HOLIDAY或WORKDAY")
    private String holidayType;
}