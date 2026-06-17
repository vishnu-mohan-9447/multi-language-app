package com.example.dashboard.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.function.client.WebClient;

@Controller
public class DashboardController {

    private final WebClient webClient = WebClient.create();

    @GetMapping("/")
    public String dashboard(Model model) {
        // Call other services (use service names in Docker/K8s)
        String users = webClient.get().uri("http://python-module:5000/users").retrieve().bodyToMono(String.class).block();
        String products = webClient.get().uri("http://go-module:8081/products").retrieve().bodyToMono(String.class).block();
        String orders = webClient.get().uri("http://nodejs-module:3000/orders").retrieve().bodyToMono(String.class).block();

        model.addAttribute("users", users);
        model.addAttribute("products", products);
        model.addAttribute("orders", orders);
        return "dashboard";  // Renders templates/dashboard.html via Thymeleaf
    }

    @GetMapping("/health")
    public String health() {
        return "UP";
    }
}
