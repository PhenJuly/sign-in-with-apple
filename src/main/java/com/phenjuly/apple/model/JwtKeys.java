package com.phenjuly.apple.model;

import lombok.Data;
import lombok.Getter;

import java.util.List;

/**
 * @author PhenJuly
 * @create 2020/2/20
 * @since
 */
@Data
@Getter
public class JwtKeys {
    private List<Key> keys;

    @Data
    @Getter
    public static class Key {
        private String kty;
        private String kid;
        private String use;
        private String alg;
        private String n;
        private String e;
    }
}
