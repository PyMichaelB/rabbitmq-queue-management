package de.gessnerfl.rabbitmq.queue.management.controller.rest;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import de.gessnerfl.rabbitmq.queue.management.model.remoteapi.Exchange;
import de.gessnerfl.rabbitmq.queue.management.service.rabbitmq.RabbitMqFacade;

@RestController
public class ExchangeController {

    private final RabbitMqFacade facade;

    @Autowired
    public ExchangeController(RabbitMqFacade facade) {
        this.facade = facade;
    }

    @GetMapping("/api/{vhost}/exchanges")
    public List<Exchange> getExchanges(@PathVariable String vhost) {
        return facade.getExchanges(vhost);
    }

    @GetMapping("/api/exchanges/{vhost}/{exchange}/routingKeys")
    public List<String> getSourceBindings(@PathVariable String vhost, @PathVariable String exchange) {
        return facade.getExchangeSourceBindings(vhost, exchange)
                .stream()
                .map(b -> b.getRoutingKey())
                .distinct()
                .collect(Collectors.toList());
    }

}
