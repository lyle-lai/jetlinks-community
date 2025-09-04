package org.jetlinks.community.auth.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpMethod;

import java.io.Serializable;
import java.util.Map;

/**
 * 第三方应用凭证信息,用于统一认证。
 *
 * @author Gemini
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppCredentials implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 应用ID
     */
    private String appId;

    /**
     * 应用Key
     */
    private String appKey;

    /**
     * 时间戳(秒)
     */
    private String timestamp;

    /**
     * 随机数
     */
    private String nonce;

    /**
     * 签名
     */
    private String signature;

    /**
     * 请求路径
     */
    private String requestPath;

    /**
     * 请求方法
     */
    private HttpMethod requestMethod;

    /**
     * 请求查询参数
     */
    private Map<String, String> queryParams;

}
