package com.sky.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
public class EmployeePageQueryDTO implements Serializable {

    //员工姓名
    @Schema
    private String name;

    //页码
    @Schema
    private int page;

    //每页显示记录数
    @Schema
    private int pageSize;

}
