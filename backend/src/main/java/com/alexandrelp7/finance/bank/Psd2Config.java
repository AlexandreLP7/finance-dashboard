package com.alexandrelp7.finance.bank;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class Psd2Config {

  @Bean
  public RestClient.Builder psd2RestClientBuilder() {
    return RestClient.builder();
  }
}
