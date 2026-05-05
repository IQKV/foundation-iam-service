/*
 * Copyright 2026 IQKV Foundation Team.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iqkv.foundation.iamservice;

import java.util.TimeZone;

import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "PT55M")
@ConfigurationPropertiesScan
@MapperScan(
    basePackages = {
        "com.iqkv.foundation.iamservice.denylist",
        "com.iqkv.foundation.iamservice.email",
        "com.iqkv.foundation.iamservice.invitation",
        "com.iqkv.foundation.iamservice.lockout",
        "com.iqkv.foundation.iamservice.membership",
        "com.iqkv.foundation.iamservice.passwordreset",
        "com.iqkv.foundation.iamservice.tenant",
        "com.iqkv.foundation.iamservice.user"
    },
    annotationClass = Mapper.class
)
public class IamServiceApplication {

  public static void main(String[] args) {
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    SpringApplication.run(IamServiceApplication.class, args);
  }
}
