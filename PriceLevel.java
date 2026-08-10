package com.example.InternProject.Model;

public class PriceLevel {

    private double price;
    private int quantity;

    public PriceLevel(double price, int quantity) {
        this.price = price;
        this.quantity = quantity;
    }

    public double getPrice() {
        return price;
    }

    public int getQuantity() {
        return quantity;
    }
}
