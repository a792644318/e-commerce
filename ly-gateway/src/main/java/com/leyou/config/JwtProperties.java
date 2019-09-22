package com.leyou.config;

import com.leyou.auth.utils.RsaUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.security.PublicKey;

@Slf4j
@Data
@ConfigurationProperties(prefix = "ly.jwt")
public class JwtProperties {
    private String pubKeyPath;
    private PublicKey publicKey;
    private String cookieName;

    @PostConstruct
    public void init(){
        try{
            //获取公钥
            this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        }catch (Exception e){
            log.error("初始化公钥失败", e);
            throw new RuntimeException();
        }
    }
}
