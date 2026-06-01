package com.solidoa.hr.finance.form;

import lombok.Data;
import java.util.List;

@Data
public class BatchPayForm {
    private List<Long> salaryIds;
}