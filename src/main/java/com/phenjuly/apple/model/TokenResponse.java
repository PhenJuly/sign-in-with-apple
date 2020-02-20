package com.phenjuly.apple.model;

import io.jsonwebtoken.Claims;
import lombok.Data;
import org.apache.tomcat.util.codec.binary.Base64;

/**
 * @author PhenJuly
 * @create 2020/2/20
 * @since
 */
@Data
public class TokenResponse {
    private String access_token;
    private String expires_in;
    private String id_token;
    private String refresh_token;
    private String token_type;
    private String error;

    private String verify;

    @Override
    public String toString() {
        return "TokenResponse{" +
                "access_token='" + access_token + '\'' +
                ", expires_in='" + expires_in + '\'' +
                ", id_token='" + id_token + '\'' +
                ", decode_id_token_header='" + new String(Base64.decodeBase64(id_token.split("\\.")[0])) + '\'' +
                ", decode_id_token_body='" + new String(Base64.decodeBase64(id_token.split("\\.")[1])) + '\'' +
                ", refresh_token='" + refresh_token + '\'' +
                ", token_type='" + token_type + '\'' +
                ", error='" + error + '\'' +
                ", verify='" + verify + '\'' +
                '}';
    }
}
