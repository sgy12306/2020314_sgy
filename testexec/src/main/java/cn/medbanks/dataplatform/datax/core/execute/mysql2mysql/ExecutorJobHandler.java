package cn.medbanks.dataplatform.datax.core.execute.mysql2mysql;

import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.annotation.JobHandler;
import com.xxl.job.core.log.XxlJobLogger;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * TODO DataX任务运行
 *
 * @author 孙广园
 * @version V1.0
 * @since 2020-02-18 14:51
 */
@JobHandler(value = "mysql2mysql")
@Component
public class ExecutorJobHandler extends IJobHandler {
    @Value("${datax.executor.jsonpath}")
    private String jsonpath;

    @Value("${datax.pypath}")
    private String dataXPyPath;

    public static final String SPLIT_SPACE = " ";

    @Override
    public ReturnT<String> execute(String tgParam) throws RuntimeException, IOException {
        String cmd = buildCmd(tgParam, jsonpath);
        try {
            Runtime.getRuntime().exec(cmd);
            XxlJobLogger.log("------------------DataX运行进程");
        } catch (RuntimeException r) {
            return new ReturnT<>(IJobHandler.FAIL.getCode(), "任务执行失败，请重试");
        }
        return IJobHandler.SUCCESS;

    }

    private String buildCmd(String tgParam, String tmpFilePath) {
        StringBuilder builder = new StringBuilder();
        builder.append("python");
        builder.append(SPLIT_SPACE);
        builder.append(dataXPyPath);
        builder.append(SPLIT_SPACE);
        builder.append(tgParam);
        builder.append(SPLIT_SPACE);
        builder.append(tmpFilePath);
        System.out.println(builder.toString());
        return builder.toString();
    }


}
