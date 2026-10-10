package com.guoyu.platform.chazheng.exception;

/**
 * 查证第三方接口级故障（DNS 解析失败/网络不通/连接超时/对方 5xx）。
 *
 * <p>与「用户级查证失败」（接口正常应答但查无此人、业务报错）区分：
 * 接口级故障时逐用户重试毫无意义，批处理层据此对整个批次熔断中止，
 * 待接口恢复后用「重试失败用户」一键重跑。
 */
public class CertQueryInterfaceDownException extends RuntimeException {

  public CertQueryInterfaceDownException(String message) {
    super(message);
  }

  public CertQueryInterfaceDownException(String message, Throwable cause) {
    super(message, cause);
  }
}
