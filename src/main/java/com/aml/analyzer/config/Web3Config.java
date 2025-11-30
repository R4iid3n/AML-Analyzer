package com.aml.analyzer.config;

import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

import java.util.concurrent.TimeUnit;

/**
 * Web3 configuration for blockchain connectivity.
 */
@Configuration
public class Web3Config {

    @Value("${blockchain.ethereum.rpc-url}")
    private String ethRpcUrl;

    @Bean
    public Web3j web3j() {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        return Web3j.build(new HttpService(ethRpcUrl, httpClient));
    }
}
