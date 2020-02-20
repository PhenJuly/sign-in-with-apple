package com.phenjuly.apple.service;

import com.alibaba.fastjson.JSONObject;
import com.phenjuly.apple.model.JwtKeys;
import com.phenjuly.apple.model.TokenResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.stream.Collectors;

/**
 * @author PhenJuly
 * @create 2020/2/19
 * @since
 */
@Slf4j
@Component
public class AppleIdTokenVerify {
    /**
     * 后台验证jwt Id_token 获取公钥地址
     */
    @Value("${apple.login.auth-keys-url}")
    public String authKeyUrl;


    @Autowired
    private RestTemplate restTemplate;

    /**
     * 验证JWT的有效性
     *
     * @param jwt      授权回调返回的JWT格式数据，即id_token
     * @param audience APPID 或者client_id
     * @param subject  userId
     * @return
     */
    public Claims verify(String jwt, String audience, String subject) throws InvalidKeySpecException, NoSuchAlgorithmException {
        JwtKeys keys = restTemplate.getForObject(authKeyUrl, JwtKeys.class);

        // apple的keys接口会返回三条记录，要取kid为id_token一致那条，否则会验证不通过
        String jsonKey = new String(Base64.decodeBase64(jwt.split("\\.")[0]));
        JwtKeys.Key publicKeyType = JSONObject.parseObject(jsonKey, JwtKeys.Key.class);
        JwtKeys.Key key = keys.getKeys().stream()
                .collect(Collectors.toMap(JwtKeys.Key::getKid, k -> k))
                .get(publicKeyType.getKid());

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        BigInteger modulus = new BigInteger(1, Base64.decodeBase64(key.getN()));
        BigInteger publicExponent = new BigInteger(1, Base64.decodeBase64(key.getE()));
        Jws<Claims> claim = getClaims(keyFactory.generatePublic(new RSAPublicKeySpec(modulus, publicExponent)), jwt, audience, subject);
        if (claim != null && claim.getBody().containsKey("auth_time")) {
            return claim.getBody();
        }
        return null;
    }

    /**
     * @param key      通过apple 公钥接口回去的n e 够着的公钥对象
     * @param jwt      授权回调返回的JWT格式数据，即id_token
     * @param issuer   默认为 https://appleid.apple.com
     * @param audience APPID 或者client_id
     * @param subject  userId
     * @return
     */
    public Jws<Claims> getClaims(PublicKey key, String jwt, String audience, String subject) {
        JwtParser jwtParser = Jwts.parser().setSigningKey(key);
        jwtParser.requireIssuer("https://appleid.apple.com");
        jwtParser.requireAudience(audience);
        if (subject != null) {
            jwtParser.requireSubject(subject);
        }
        return jwtParser.parseClaimsJws(jwt);

    }

}
