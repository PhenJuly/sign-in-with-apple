package com.phenjuly.apple.controller;

import com.phenjuly.apple.model.TokenResponse;
import com.phenjuly.apple.service.AppleIdTokenVerify;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestTemplate;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

/**
 * @author PhenJuly
 */
@Controller
public class AppleController {
    /**
     * 后台验证jwt Id_token 获取公钥地址
     */
    @Value("${apple.login.auth-keys-url}")
    public String authKeyUrl;

    /**
     * apple提供的获取token的地址
     */
    @Value("${apple.login.token-url}")
    public String tokenUrl;

    /**
     * apple后台配置的授权回调地址
     */
    @Value("${apple.login.redirect-uri}")
    public String redirectUri;

    /**
     * Apple services标识，即APPId
     */
    @Value("${apple.login.client-id}")
    private String clientId;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private AppleIdTokenVerify appleIdTokenVerify;

    @GetMapping(path = "/login")
    public String login() {
        return "/login.html";
    }

    @GetMapping(path = "/")
    public String index() {
        return "/login.html";
    }

    /**
     * app授权后台验证接口 个人写死测试接口
     *
     * @return
     */
    @ResponseBody
    @GetMapping(path = "/app/auth")
    public String appAuth() throws InvalidKeySpecException, NoSuchAlgorithmException {
        String idToken = "xxxxxx";
        String appleId = "xxxxxx";
        String userId = "001567.xxxxxx.0223";
        Claims verify = appleIdTokenVerify.verify(idToken, appleId, userId);
        return verify != null ? verify.toString() : "";
    }

    /**
     * web 授权回调接口
     *
     * @param code
     * @param id_token
     * @param state
     * @param user
     * @param model
     * @return
     * @throws InvalidKeySpecException
     * @throws NoSuchAlgorithmException
     */
    @PostMapping(value = "/callback", consumes = "application/x-www-form-urlencoded")
    public String callback(String code, String id_token, String state, String user, Model model) throws InvalidKeySpecException, NoSuchAlgorithmException {
        model.addAttribute("code", code);
        model.addAttribute("id_token", id_token);
        model.addAttribute("state", state);
        model.addAttribute("user", user);

        // 如果后台不验证请求有效期或者其他，直接解析id_token获取里面的信息即可
        String decodedHeader = new String(Base64.decodeBase64(id_token.split("\\.")[0]));
        model.addAttribute("decode_id_token_header", decodedHeader);
        String decoded = new String(Base64.decodeBase64(id_token.split("\\.")[1]));
        model.addAttribute("decode_id_token", decoded);

        // 授权码验证方式 注意：web授权不会无法直接获取到用户唯一标识，需要解析id_token获取其中的sub字段，id_token可以是token服务验证返回的，也可以是授权回调返回的
        TokenResponse tokenResponse = verificationForAuthCode(code);
        model.addAttribute("verificationForCode", tokenResponse.toString());

        // 验证JWT方式
        Claims verify = appleIdTokenVerify.verify(id_token, clientId, null);
        model.addAttribute("verificationForJwt", verify != null ? verify.toString() : "");
        return "succeed";
    }


    public TokenResponse verificationForAuthCode(String code) throws InvalidKeySpecException, NoSuchAlgorithmException {
        Map<String, Object> claims = new HashMap<>();
        //team id
        claims.put("iss", "3VSS4HFDD6");

        // 时间，过期时间不能超过6个月
        long now = System.currentTimeMillis() / 1000;
        claims.put("iat", now);
        // 实效时间：最长半年，单位秒
        claims.put("exp", now + 24 * 50 * 50 * 30);
        // 默认写死"https://appleid.apple.com"
        claims.put("aud", "https://appleid.apple.com");
        claims.put("sub", clientId);

        Map<String, Object> header = new HashMap<>();
        header.put("alg", SignatureAlgorithm.ES256.getValue());
        // keys ID
        header.put("kid", "B9FAWG7V97");


        PrivateKey privateKey = KeyFactory.getInstance("EC")
                .generatePrivate(new PKCS8EncodedKeySpec(readKey()));

        String client_secret = Jwts.builder()
                .setHeader(header)
                .setClaims(claims)
                .signWith(SignatureAlgorithm.ES256, privateKey)
                .compact();

        MultiValueMap<String, String> postParameters = new LinkedMultiValueMap<>();
        postParameters.add("client_id", clientId);
        postParameters.add("client_secret", client_secret);
        postParameters.add("code", code);
        postParameters.add("grant_type", "authorization_code");
        postParameters.add("redirect_uri", redirectUri);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/x-www-form-urlencoded;charset=UTF-8"));

        HttpEntity<MultiValueMap<String, String>> formEntity = new HttpEntity<>(postParameters, headers);

        ResponseEntity<TokenResponse> forEntity = restTemplate.postForEntity(tokenUrl, formEntity, TokenResponse.class);
        TokenResponse body = forEntity.getBody();
        Claims verify = appleIdTokenVerify.verify(body.getId_token(), clientId, null);
        body.setVerify(verify != null ? verify.toString() : "");
        return body;
    }

    /**
     * 后台配置下载的密钥文件，文件内容格式为：
     * -----BEGIN PRIVATE KEY-----
     * xxx 密钥
     * -----END PRIVATE KEY-----
     * <p>
     * 可改成配置获取调整为读取文件
     *
     * @return
     */
    public byte[] readKey() {
        String temp = "xxxxxx";
        return Base64.decodeBase64(temp);
    }


}
