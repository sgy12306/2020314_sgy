package com.xxl.job.executor.service.jobhandler;


import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.biz.model.TriggerParam;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.annotation.JobHandler;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.log.XxlJobLogger;
import com.xxl.job.core.util.DateUtil;
import com.xxl.job.executor.log.JobLogger;
import com.xxl.job.executor.util.SystemUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;

/**
 * DataX任务运行
 *
 * @author jingwk 2019-11-16
 */

@Component
public class ExecutorJobHandler  {

    @Value("${datax.executor.jsonpath}")
    private String jsonpath;

    @Value("${datax.pypath}")
    private String dataXPyPath;

    @XxlJob("mysql2mysql")
    public ReturnT<String> execute(String tgParam) throws Exception {
        int exitValue = -1;
        Thread inputThread = null;
        Thread errThread = null;
        String tmpFilePath;
        //生成Json临时文件
        tmpFilePath = "D:\\datax-mini\\datax\\job\\job.json";
        try {
            String[] cmdarrayFinal = buildCmd(tgParam, tmpFilePath);
            final Process process = Runtime.getRuntime().exec(cmdarrayFinal);
//            String processId = ProcessUtil.getProcessId(process);
            XxlJobLogger.log("------------------DataX运行进程");

            //更新任务进程Id
//            ProcessCallbackThread.pushCallBack(new HandleProcessCallbackParam(tgParam.getLogId(), tgParam.getLogDateTime(), processId));
            // log-thread
            inputThread = new Thread(() -> {
                try {
                    reader(new BufferedInputStream(process.getInputStream()));

                } catch (IOException e) {
                    JobLogger.log(e);
                }
            });
            errThread = new Thread(() -> {
                try {
                    reader(new BufferedInputStream(process.getErrorStream()));
                } catch (IOException e) {
                    JobLogger.log(e);
                }
            });
            inputThread.start();
            errThread.start();
            // process-wait
            exitValue = process.waitFor();      // exit code: 0=success, 1=error
            // log-thread join
            inputThread.join();
            errThread.join();
        } catch (Exception e) {
            JobLogger.log(e);
        } finally {
            if (inputThread != null && inputThread.isAlive()) {
                inputThread.interrupt();
            }
            if (errThread != null && errThread.isAlive()) {
                errThread.interrupt();
            }
//            //  删除临时文件
            if (FileUtil.exist(tmpFilePath)) {
                FileUtil.del(new File(tmpFilePath));
                System.out.println("fjja");
                System.out.println("fjja");
                System.out.println("fjja");
                System.out.println("fjja");
            }
        }
        if (exitValue == 0) {
            return IJobHandler.SUCCESS;
        } else {
            return new ReturnT<>(IJobHandler.FAIL.getCode(), "command exit value(" + exitValue + ") is failed");
        }
    }

    private String[] buildCmd(String tgParam, String tmpFilePath) {
        // command process
        //"--loglevel=debug"
        List<String> cmdarray = new ArrayList<>();
        cmdarray.add("python");
        String dataXHomePath = SystemUtils.getDataXHomePath();
        if (StringUtils.isNotEmpty(dataXHomePath)) dataXPyPath = dataXHomePath + DataxOption.DEFAULT_DATAX_PY;
        cmdarray.add(dataXPyPath);
//        String doc = buildDataXParam(tgParam);
//        if (StringUtils.isNotBlank(doc)) {
//            cmdarray.add(doc.replaceAll(DataxOption.SPLIT_SPACE, DataxOption.TRANSFORM_SPLIT_SPACE));
//        }
        cmdarray.add(tmpFilePath);
        return cmdarray.toArray(new String[cmdarray.size()]);
    }

    /**
     * 数据流reader（Input自动关闭，Output不处理）
     *
     * @param inputStream
     * @throws java.io.IOException
     */
    private static void reader(InputStream inputStream) throws IOException {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                XxlJobLogger.log(line);
            }
            reader.close();
            inputStream = null;
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    private String buildDataXParam(TriggerParam tgParam) {
        StringBuilder doc = new StringBuilder();
        String jvmParam = tgParam.getJvmParam();
        String partitionStr = tgParam.getPartitionInfo();
        if (StringUtils.isNotBlank(jvmParam)) {
            doc.append(DataxOption.JVM_CM).append(DataxOption.TRANSFORM_QUOTES).append(jvmParam).append(DataxOption.TRANSFORM_QUOTES);
        }
        long tgSecondTime = tgParam.getTriggerTime().getTime() / 1000;
        if (StringUtils.isNotBlank(tgParam.getReplaceParam())) {
            long lastTime = tgParam.getStartTime().getTime() / 1000;
            if (doc.length() > 0) doc.append(DataxOption.SPLIT_SPACE);
            doc.append(DataxOption.PARAMS_CM).append(DataxOption.TRANSFORM_QUOTES).append(String.format(tgParam.getReplaceParam(), lastTime, tgSecondTime));
            if (StringUtils.isNotBlank(partitionStr)) {
                doc.append(DataxOption.SPLIT_SPACE);
                List<String> partitionInfo = Arrays.asList(partitionStr.split(","));
                doc.append(String.format(DataxOption.PARAMS_CM_V_PT, buildPartition(partitionInfo)));
            }
            doc.append(DataxOption.TRANSFORM_QUOTES);
        }else{
            if (StringUtils.isNotBlank(partitionStr)) {
                List<String> partitionInfo = Arrays.asList(partitionStr.split(","));
                if (doc.length() > 0) doc.append(DataxOption.SPLIT_SPACE);
                doc.append(DataxOption.PARAMS_CM).append(DataxOption.TRANSFORM_QUOTES).append(String.format(DataxOption.PARAMS_CM_V_PT, buildPartition(partitionInfo))).append(DataxOption.TRANSFORM_QUOTES);
            }
        }
        JobLogger.log("------------------命令参数: " + doc);
        return doc.toString();
    }

    private String buildPartition(List<String> partitionInfo) {
        String field = partitionInfo.get(0);
        int timeOffset = Integer.parseInt(partitionInfo.get(1));
        String timeFormat = partitionInfo.get(2);
        String partitionTime = DateUtil.format(DateUtil.addDays(new Date(), timeOffset), timeFormat);
        return field + "=" + partitionTime;
    }

    private String generateTemJsonFile(String jobJson) {
        String tmpFilePath;
        String dataXHomePath = SystemUtils.getDataXHomePath();
        if (StringUtils.isNotEmpty(dataXHomePath)) jsonpath = dataXHomePath + DataxOption.DEFAULT_JSON;
        if (!FileUtil.exist(jsonpath)) FileUtil.mkdir(jsonpath);
        tmpFilePath = jsonpath + "jobTmp-" + IdUtil.simpleUUID() + ".conf";
        // 根据json写入到临时本地文件
        try (PrintWriter writer = new PrintWriter(tmpFilePath, "UTF-8")) {
            writer.println(jobJson);
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            JobLogger.log("JSON 临时文件写入异常：" + e.getMessage());
        }
        return tmpFilePath;
    }


}
