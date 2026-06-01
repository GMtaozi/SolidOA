package com.solidoa.system.service;

import com.solidoa.common.vo.PageVO;
import com.solidoa.system.form.ContactForm;
import com.solidoa.system.vo.ContactVO;
import java.util.List;

public interface ContactService {
    Long create(ContactForm form);
    void update(Long id, ContactForm form);
    void delete(Long id);
    ContactVO getById(Long id);
    List<ContactVO> listAll();
    List<ContactVO> listByDeptId(Long deptId);
    PageVO<ContactVO> search(String keyword, Integer pageNum, Integer pageSize);
}