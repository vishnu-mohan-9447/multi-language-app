package main

import (
    "encoding/json"
    "fmt"
    "log"
    "net/http"
    "os"
)

type Product struct {
    ID    int    `json:"id"`
    Name  string `json:"name"`
    Price float64 `json:"price"`
}

func health(w http.ResponseWriter, r *http.Request) {
    fmt.Fprintf(w, `{"status": "healthy", "service": "go-products"}`)
}

func products(w http.ResponseWriter, r *http.Request) {
    products := []Product{
        {ID: 101, Name: "Laptop", Price: 999.99},
        {ID: 102, Name: "Phone", Price: 699.99},
    }
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(products)
}

func main() {
    http.HandleFunc("/health", health)
    http.HandleFunc("/products", products)

    port := os.Getenv("PORT")
    if port == "" {
        port = "8081"
    }
    log.Printf("Go service starting on port %s", port)
    log.Fatal(http.ListenAndServe(":"+port, nil))
}
