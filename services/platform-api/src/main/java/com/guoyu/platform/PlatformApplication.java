package com.guoyu.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan({
    "com.guoyu.platform.system.mapper",
    "com.guoyu.platform.file.mapper",
    "com.guoyu.platform.ai.mapper",
    "com.guoyu.platform.exam.mapper",
    "com.guoyu.platform.exam.knowledgefix.mapper",
    "com.guoyu.platform.trainingdepartment.mapper",
    "com.guoyu.platform.app.mapper",
    "com.guoyu.platform.app.exam.mapper",
    "com.guoyu.platform.app.home.mapper",
    "com.guoyu.platform.knowledge.mapper",
    "com.guoyu.platform.certification.mapper",
    "com.guoyu.platform.certification.info.mapper",
    "com.guoyu.platform.contract.mapper",
    "com.guoyu.platform.chazheng.mapper",
    "com.guoyu.platform.competition.mapper",
    "com.guoyu.platform.competition.sorting.mapper",
    "com.guoyu.platform.competition.classify.mapper",
    "com.guoyu.platform.competition.draw.mapper",
    "com.guoyu.platform.competition.messaging.mapper",
    "com.guoyu.platform.exam.reviewsync.mapper",
    "com.guoyu.platform.exam.standard.mapper",
    "com.guoyu.platform.exam.theorygen.mapper",
    "com.guoyu.platform.exam.skillgen.mapper",
    "com.guoyu.platform.courseware.mapper"
})
@SpringBootApplication
public class PlatformApplication {

  public static void main(String[] args) {
    SpringApplication.run(PlatformApplication.class, args);
  }
}
