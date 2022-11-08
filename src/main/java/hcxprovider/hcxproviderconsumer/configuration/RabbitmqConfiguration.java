package hcxprovider.hcxproviderconsumer.configuration;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitmqConfiguration {
    @Value("${queue.req.name}")
    String reqQueue;

    @Value("${queue.res.name}")
    String resQueue;

    @Value("${queue.exchange}")
    String exchange;

    @Value("${queue.req.routingKey}")
    private String reqRoutingkey;

    @Value("${queue.res.routingKey}")
    private String resRoutingkey;


    @Bean
    public Queue ReqQueue() {
        return new Queue(reqQueue);
    }

    @Bean
    public Queue ResQueue() {
        return new Queue(resQueue);
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchange);
    }

    @Bean
    public Binding Reqbind() {
        return BindingBuilder.bind(ReqQueue()).to(exchange()).with(reqRoutingkey);
    }

    public Binding Resbind() {
        return BindingBuilder.bind(ResQueue()).to(exchange()).with(resRoutingkey);
    }

    @Bean
    public MessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate template(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter());
        return rabbitTemplate;
    }
}
