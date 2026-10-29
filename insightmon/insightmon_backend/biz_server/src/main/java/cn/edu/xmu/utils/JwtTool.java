package cn.edu.xmu.utils;

import cn.edu.xmu.exception.UnauthorizedException;
import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTValidator;
import cn.hutool.jwt.signers.JWTSigner;
import cn.hutool.jwt.signers.JWTSignerUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Date;

@Component
@Slf4j
public class JwtTool {
    private final JWTSigner jwtSigner;

    private static final KeyPair keyPair = generateKeyPair();


    public JwtTool() {
        this.jwtSigner = JWTSignerUtil.createSigner("rs256", keyPair);
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048); // 设置密钥长度
            return keyPairGenerator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("无法生成 RSA 密钥对", e);
        }
    }


    public String createToken(Long userId, Duration ttl) {
        // 1.生成jws
        return JWT.create()
                .setPayload("user", userId)
                .setExpiresAt(new Date(System.currentTimeMillis() + ttl.toMillis()))
                .setSigner(jwtSigner)
                .sign();
    }

    /**
     * 解析token
     *
     * @param token token
     * @return 解析刷新token得到的用户信息
     */
    public Long parseToken(String token) {
        if (token == null) {
            throw new UnauthorizedException("未登录");
        }

        try {
            JWT jwt = JWT.of(token).setSigner(jwtSigner);

            if (!jwt.verify()) {
                throw new UnauthorizedException("无效的token2 - 签名验证失败");
            }

            JWTValidator.of(jwt).validateDate();

            Object userPayload = jwt.getPayload("user");
            if (userPayload == null) {
                throw new UnauthorizedException("无效的token3 - 缺少用户信息");
            }

            return Long.valueOf(userPayload.toString());
        } catch (ValidateException e) {
            throw new UnauthorizedException("token已经过期");
        } catch (Exception e) {
            throw new UnauthorizedException("解析 token 失败", e);
        }
    }

}
