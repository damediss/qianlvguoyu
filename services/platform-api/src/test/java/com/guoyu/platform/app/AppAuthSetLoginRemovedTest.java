package com.guoyu.platform.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.guoyu.platform.app.auth.AppAuthApplicationService;
import com.guoyu.platform.app.controller.AppAuthController;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * /appauth/setlogin 永久下线契约测试（鉴权方案 Q3）：
 * 仅凭身份证+手机号、不验密码即发 token 的端点不得重新上线。
 */
class AppAuthSetLoginRemovedTest {

  @Test
  void controllerHasNoSetLoginEndpoint() {
    assertThat(Arrays.stream(AppAuthController.class.getDeclaredMethods())
        .filter(method -> method.getName().equalsIgnoreCase("setLogin"))
        .findAny())
        .as("AppAuthController 不得再存在 setLogin 方法（含无注解的死代码）")
        .isEmpty();

    assertThat(Arrays.stream(AppAuthController.class.getDeclaredMethods())
        .flatMap(method -> Arrays.stream(method.getAnnotationsByType(PostMapping.class)))
        .flatMap(mapping -> Arrays.stream(mapping.value()))
        .filter(path -> path.toLowerCase().contains("setlogin"))
        .findAny())
        .as("不得存在映射到 setlogin 的 @PostMapping")
        .isEmpty();
  }

  @Test
  void applicationServiceHasNoSetLoginMethod() {
    assertThat(Arrays.stream(AppAuthApplicationService.class.getDeclaredMethods())
        .map(Method::getName)
        .filter(name -> name.equalsIgnoreCase("setLogin"))
        .findAny())
        .as("AppAuthApplicationService 不得再存在 setLogin 方法")
        .isEmpty();
  }

  @Test
  void appauthBaseMappingIsUnchanged() {
    RequestMapping mapping = AppAuthController.class.getAnnotation(RequestMapping.class);
    assertThat(mapping).isNotNull();
    assertThat(mapping.value()).containsExactly("/appauth");
  }
}
