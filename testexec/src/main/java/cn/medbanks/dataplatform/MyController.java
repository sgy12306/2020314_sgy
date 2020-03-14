package cn.medbanks.dataplatform;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TODO
 *
 * @author 孙广园
 * @version V1.0
 * @since 2020-02-19 12:08
 */
@RestController
public class MyController {
    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;
    @Value("${datax.pypath}")
    private String dataXPyPath;
    @GetMapping("/err")
    public String getIndex() {
        return adminAddresses;
    }
}
