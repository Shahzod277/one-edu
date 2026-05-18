package uz.raqamli_talim.oneedu.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.time.Duration;
@Configuration
public class WebClientConfig {

    @Bean("secureWebClientBuilder")
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }

    @Bean("insecureWebClientBuilder")
    public WebClient.Builder insecureWebClientBuilder() throws SSLException {
        SslContext context = SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

        HttpClient httpClient = HttpClient.create()
                .secure(t -> t.sslContext(context));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    @Primary
    @Bean("secureWebClient")
    public WebClient secureWebClient(
            @Qualifier("secureWebClientBuilder") WebClient.Builder builder
    ) {
        return builder.build();
    }

    @Bean("insecureWebClient")
    public WebClient insecureWebClient(
            @Qualifier("insecureWebClientBuilder") WebClient.Builder builder
    ) {
        return builder
                .codecs(c -> c.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }
}
