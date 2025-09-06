package org.jetlinks.community.auth.common;

import org.hswebframework.web.authorization.token.ParsedToken;
import reactor.core.publisher.Mono;

/**
 * 第三方应用凭证校验器接口
 *
 * @author Gemini
 * @since 1.0
 */
public interface AppCredentialsValidator {

    /**
     * 根据传入的凭证信息进行校验,并返回解析后的token信息。
     *
     * @param credentials 凭证信息
     * @param isWebSocket 是否是websocket
     * @return 异步的token信息,如果校验失败,则返回一个包含{@link org.hswebframework.web.authorization.exception.UnAuthorizedException}的错误Mono
     */
    Mono<ParsedToken> validate(AppCredentials credentials, boolean isWebSocket);


    /**
     * 根据传入的凭证信息进行校验,并返回解析后的token信息。
     *
     * @param credentials 凭证信息
     * @return 异步的token信息,如果校验失败,则返回一个包含{@link org.hswebframework.web.authorization.exception.UnAuthorizedException}的错误Mono
     */
    default Mono<ParsedToken> validate(AppCredentials credentials){
        return this.validate(credentials,false);
    }

}