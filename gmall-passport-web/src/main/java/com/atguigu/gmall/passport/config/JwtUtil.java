package com.atguigu.gmall.passport.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.impl.Base64UrlCodec;

import java.util.Map;

public class JwtUtil {

    /**
     * 生成token
     * @param key
     * @param param
     * @param salt
     * @return
     */
    public static String encode(String key,Map<String,Object> param,String salt){
        if(salt!=null){
            key+=salt;
        }
        //将key再次加密
        Base64UrlCodec base64UrlCodec = new Base64UrlCodec();
        key = base64UrlCodec.encode(key.getBytes());

        JwtBuilder jwtBuilder = Jwts.builder().signWith(SignatureAlgorithm.HS256,key);

        jwtBuilder = jwtBuilder.setClaims(param);

        String token = jwtBuilder.compact();
        return token;

    }

    /**
     * 将token解密
     * @param token
     * @param key
     * @param salt
     * @return
     */
    public  static Map<String,Object> decode(String token , String key, String salt){
        Claims claims=null;
        if (salt!=null){
            key+=salt;
        }
        //将key再次加密
        Base64UrlCodec base64UrlCodec = new Base64UrlCodec();
        key = base64UrlCodec.encode(key.getBytes());

        try {
            claims= Jwts.parser().setSigningKey(key).parseClaimsJws(token).getBody();
        } catch ( JwtException e) {
            return null;
        }
        return  claims;
    }

}
