package com.solidoa.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.solidoa.common.exception.BusinessException;
import com.solidoa.common.vo.PageVO;
import com.solidoa.system.entity.Contact;
import com.solidoa.system.form.ContactForm;
import com.solidoa.system.mapper.ContactMapper;
import com.solidoa.system.service.ContactService;
import com.solidoa.system.vo.ContactVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ContactServiceImpl implements ContactService {

    @Autowired
    private ContactMapper contactMapper;

    @Override
    public Long create(ContactForm form) {
        // 检查userId是否已存在（仅在userId不为null时检查）
        if (form.getUserId() != null) {
            LambdaQueryWrapper<Contact> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Contact::getUserId, form.getUserId());
            if (contactMapper.selectCount(wrapper) > 0) {
                throw new BusinessException("该用户联系人已存在");
            }
        }

        Contact contact = new Contact();
        BeanUtils.copyProperties(form, contact);
        contactMapper.insert(contact);
        log.info("创建通讯录: id={}, userId={}", contact.getId(), form.getUserId());
        return contact.getId();
    }

    @Override
    public void update(Long id, ContactForm form) {
        Contact contact = contactMapper.selectById(id);
        if (contact == null) {
            throw new BusinessException("通讯录记录不存在");
        }
        BeanUtils.copyProperties(form, contact);
        contactMapper.updateById(contact);
        log.info("更新通讯录: id={}", id);
    }

    @Override
    public void delete(Long id) {
        contactMapper.deleteById(id);
        log.info("删除通讯录: id={}", id);
    }

    @Override
    public ContactVO getById(Long id) {
        Contact contact = contactMapper.selectById(id);
        if (contact == null) {
            return null;
        }
        ContactVO vo = new ContactVO();
        BeanUtils.copyProperties(contact, vo);
        return vo;
    }

    @Override
    public List<ContactVO> listAll() {
        LambdaQueryWrapper<Contact> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Contact::getStatus, 1).orderByAsc(Contact::getRealName);
        List<Contact> list = contactMapper.selectList(wrapper);
        return list.stream().map(c -> {
            ContactVO vo = new ContactVO();
            BeanUtils.copyProperties(c, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<ContactVO> listByDeptId(Long deptId) {
        List<Contact> list = contactMapper.selectByDeptId(deptId);
        return list.stream().map(c -> {
            ContactVO vo = new ContactVO();
            BeanUtils.copyProperties(c, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public PageVO<ContactVO> search(String keyword, Integer pageNum, Integer pageSize) {
        pageNum = pageNum == null ? 1 : pageNum;
        pageSize = pageSize == null ? 20 : pageSize;

        LambdaQueryWrapper<Contact> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Contact::getStatus, 1);
        wrapper.and(w -> w.like(Contact::getRealName, keyword)
                .or().like(Contact::getMobile, keyword)
                .or().like(Contact::getPosition, keyword));
        wrapper.orderByAsc(Contact::getRealName);

        List<Contact> list = contactMapper.selectList(wrapper);
        int total = list.size();
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, total);

        // 边界检查：防止start超过列表长度导致IndexOutOfBoundsException
        if (start >= total) {
            start = 0;
            end = 0;
        }

        List<ContactVO> records = list.subList(start, end).stream().map(c -> {
            ContactVO vo = new ContactVO();
            BeanUtils.copyProperties(c, vo);
            return vo;
        }).collect(Collectors.toList());

        PageVO<ContactVO> page = new PageVO<>();
        page.setRecords(records);
        page.setTotal(total);
        page.setPageNum(pageNum);
        page.setPageSize(pageSize);
        return page;
    }
}