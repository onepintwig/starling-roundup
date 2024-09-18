package dev.onepintwig.starling.roundup.model;

public record FeedItem(
        CurrencyAmount amount,
        TransactionDirection direction,
        Status status
) {

    public enum TransactionDirection {
        IN, OUT
    }

    public enum Status {
        UPCOMING, UPCOMING_CANCELLED, PENDING, REVERSED, SETTLED, DECLINED, REFUNDED, RETRYING, ACCOUNT_CHECK
    }

}
