package cn.medbanks.dataplatform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "cn.medbanks.dataplatform")
public class DataplatformControllerApplication {


    public static void main(String[] args) {
        SpringApplication.run(DataplatformControllerApplication.class, args);
    }

}
