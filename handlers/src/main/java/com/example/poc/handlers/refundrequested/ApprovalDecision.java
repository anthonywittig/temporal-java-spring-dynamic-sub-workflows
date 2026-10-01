package com.example.poc.handlers.refundrequested;

public record ApprovalDecision(boolean approved, String approver, String comment) {}
