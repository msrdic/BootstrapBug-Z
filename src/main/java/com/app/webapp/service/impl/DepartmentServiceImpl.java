package com.app.webapp.service.impl;

import com.app.webapp.model.Department;
import com.app.webapp.repository.DepartmentRepository;
import com.app.webapp.service.DepartmentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class DepartmentServiceImpl implements DepartmentService {
    private final DepartmentRepository departmentRepository;
    private static final int pageSize = 11;

    public DepartmentServiceImpl(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Override
    public Page<Department> findAllDepartments(Integer page) {
        return departmentRepository.findAll(PageRequest.of(page - 1, DepartmentServiceImpl.pageSize));
    }

    @Override
    public Department findDepartmentById(Long id) {
        return departmentRepository.findById(id).orElse(null);
    }

    @Override
    public void createDepartment(Department department) {
        departmentRepository.save(department);
    }

    @Override
    public void editDepartment(Department department) {
        departmentRepository.save(department);
    }




    @Override
    public List<Department> findAll() {
        return departmentRepository.findAll();
    }

    @Override
    public Optional<Department> findById(Long id) {
        return departmentRepository.findById(id);
    }

    @Override
    public Department save(Department department) {
        return departmentRepository.save(department);
    }

    @Override
    public void deleteById(Long id) {
        departmentRepository.deleteById(id);
    }

    @Override
    public void deleteAll() {
        departmentRepository.deleteAll();
    }

    @Override
    public boolean existsById(Long id) {
        return departmentRepository.existsById(id);
    }
}