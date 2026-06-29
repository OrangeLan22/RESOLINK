package com.orangelan.resolinkagent.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import javax.net.ssl.SSLException;

@Configuration
public class WebClientConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(WebClientConfig.class);

    @Bean
    public WebClient webClient() throws SSLException {
        logger.info("初始化WebClient，检查JVM代理设置");
        
        // 获取JVM代理设置
        String httpProxyHost = System.getProperty("http.proxyHost");
        String httpProxyPort = System.getProperty("http.proxyPort");
        String httpsProxyHost = System.getProperty("https.proxyHost");
        String httpsProxyPort = System.getProperty("https.proxyPort");
        
        logger.info("JVM代理设置 - HTTP: {}:{}, HTTPS: {}:{}", 
                httpProxyHost, httpProxyPort, httpsProxyHost, httpsProxyPort);
        
        // 创建一个信任所有证书的SSL上下文
        SslContext sslContext = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

        HttpClient httpClient = HttpClient.create()
                .secure(t -> t.sslContext(sslContext));
        
        // 如果有代理设置，则配置代理
        if ((httpProxyHost != null && !httpProxyHost.isEmpty()) || 
            (httpsProxyHost != null && !httpsProxyHost.isEmpty())) {
            
            logger.info("检测到代理设置，配置WebClient使用代理");
            
            // 确定使用哪个代理设置（HTTPS优先，然后是HTTP）
            String proxyHost = httpsProxyHost != null ? httpsProxyHost : httpProxyHost;
            String proxyPortStr = httpsProxyPort != null ? httpsProxyPort : httpProxyPort;
            
            if (proxyHost != null && proxyPortStr != null) {
                int proxyPort = Integer.parseInt(proxyPortStr);
                
                httpClient = httpClient.proxy(proxy -> proxy.type(ProxyProvider.Proxy.HTTP)
                        .host(proxyHost)
                        .port(proxyPort));
                
                logger.info("WebClient已配置使用代理: {}:{}", proxyHost, proxyPort);
            } else {
                logger.info("代理设置不完整，不使用代理");
            }
        } else {
            logger.info("未检测到代理设置，不使用代理");
        }

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}