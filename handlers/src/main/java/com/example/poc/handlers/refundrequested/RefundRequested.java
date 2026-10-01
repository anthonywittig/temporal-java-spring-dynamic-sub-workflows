package com.example.poc.handlers.refundrequested;

public record RefundRequested(String refundId, String orderId, long amountCents, String reason) {}
