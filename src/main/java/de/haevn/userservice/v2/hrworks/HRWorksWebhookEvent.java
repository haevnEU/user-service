package de.haevn.userservice.v2.hrworks;

public record HRWorksWebhookEvent(String eventId, HRworksEventType eventType, String timestamp, String personId) {
}