package com.solidoa.hr.finance.form;

import lombok.Data;
import java.util.List;

@Data
public class BatchSalaryForm {
    private String yearMonth;
    private List<Long> userIds;
}