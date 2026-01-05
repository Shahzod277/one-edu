package uz.raqamli_talim.oneedu.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    WebClient webClient() throws SSLException {

        SslContext context = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

        HttpClient httpClient = HttpClient.create().secure(t -> t.sslContext(context));
        final int size = 16 * 1024 * 1024;
        final ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(codec -> codec.defaultCodecs().maxInMemorySize(size))
                .build();
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient
                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                        .responseTimeout(Duration.ofSeconds(5))))
                .exchangeStrategies(exchangeStrategies)
                .build();
    }
}
