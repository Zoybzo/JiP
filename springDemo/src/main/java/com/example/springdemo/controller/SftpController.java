package com.example.springdemo.controller;

import com.example.springdemo.common.CommonResult;
import com.example.springdemo.logic.Impl.AsyncLogicImpl;
import com.example.springdemo.schedule.ScheduleApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/sftp")
public class SftpController {
    private static final Logger LOGGER = LoggerFactory.getLogger(SftpController.class);

    @Autowired
    private ScheduleApplication scheduleApplication;

    @RequestMapping(value = "/test", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult testRequest(@RequestBody String placeholder, BindingResult result) {
        LOGGER.info("testRequest: " + placeholder);
        return CommonResult.success(null, "test success");
    }

    /**
     * 手动调用sftp
     */
    @RequestMapping(value = "/sftp", method = RequestMethod.POST)
    @ResponseBody
    public CommonResult testSftp(@RequestBody String placeholder, BindingResult result) {
        LOGGER.info("testSftp");
        scheduleApplication.triggerUploadFile();
        return CommonResult.success(null, "sftp begin");
    }
}
