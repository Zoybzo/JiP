package com.example.springdemo.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/sftp")
public class SftpController {
    private static final Logger LOGGER = LoggerFactory.getLogger(SftpController.class);

//    @RequestMapping(value = "/test", method = RequestMethod.POST)
//    @ResponseBody
//    public CommonResult customerLogin(@Valid @RequestBody ReqUmUserLogin umsAdminLoginParam, BindingResult result) {
//        return null;
//    }
}