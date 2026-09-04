package com.minh.fakebook.gateway.web.rest;

import com.minh.fakebook.gateway.security.AuthoritiesConstants;
import com.minh.fakebook.gateway.web.rest.vm.RouteVM;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.http.*;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST controller for managing Gateway configuration.
 */
@RestController
@RequestMapping("/api/gateway")
public class GatewayResource {

    private final RouteLocator routeLocator;

    private final DiscoveryClient discoveryClient;

    @Value("${spring.application.name}")
    private String appName;

    public GatewayResource(RouteLocator routeLocator, DiscoveryClient discoveryClient) {
        this.routeLocator = routeLocator;
        this.discoveryClient = discoveryClient;
    }

    /**
     * {@code GET  /routes} : get the active routes.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the list of routes.
     */
    @GetMapping("/routes")
    @Secured(AuthoritiesConstants.ADMIN)
    public Mono<ResponseEntity<List<RouteVM>>> activeRoutes() {
        return routeLocator
            .getRoutes()
            .collectList()
            .map(routes -> {
                List<RouteVM> routeVMs = new ArrayList<>();
                for (Route route : routes) {
                    RouteVM routeVM = new RouteVM();
                    
                    String predicate = route.getPredicate() != null ? route.getPredicate().toString() : "";
                    String path = predicate;
                    int startIdx = predicate.indexOf("[");
                    int endIdx = predicate.indexOf("]");
                    if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                        path = predicate.substring(startIdx + 1, endIdx);
                    }
                    routeVM.setPath(path);

                    String routeId = route.getId() != null ? route.getId() : "";
                    String serviceId = routeId;
                    int underscoreIdx = routeId.indexOf("_");
                    if (underscoreIdx != -1 && underscoreIdx + 1 < routeId.length()) {
                        serviceId = routeId.substring(underscoreIdx + 1).toLowerCase();
                    } else {
                        serviceId = routeId.toLowerCase();
                    }
                    routeVM.setServiceId(serviceId);

                    if (!serviceId.equalsIgnoreCase(appName)) {
                        try {
                            routeVM.setServiceInstances(discoveryClient.getInstances(serviceId));
                        } catch (Exception e) {
                            routeVM.setServiceInstances(new ArrayList<>());
                        }
                        routeVMs.add(routeVM);
                    }
                }
                return ResponseEntity.ok(routeVMs);
            });
    }
}
