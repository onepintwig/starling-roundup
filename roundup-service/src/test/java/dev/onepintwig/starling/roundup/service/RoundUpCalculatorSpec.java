package dev.onepintwig.starling.roundup.service;

import dev.onepintwig.starling.roundup.model.CurrencyAmount;
import dev.onepintwig.starling.roundup.model.FeedItem;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class RoundUpCalculatorSpec {


    @Test
    public void singleCurrencyRoundUp() {
        //Only roundup OUT
        List<FeedItem> roundUps = List.of(
                new FeedItem(new CurrencyAmount("GBP", 435), FeedItem.TransactionDirection.OUT, FeedItem.Status.SETTLED),
                new FeedItem(new CurrencyAmount("GBP", 520), FeedItem.TransactionDirection.OUT, FeedItem.Status.SETTLED),
                new FeedItem(new CurrencyAmount("GBP", 87), FeedItem.TransactionDirection.OUT, FeedItem.Status.SETTLED),
                new FeedItem(new CurrencyAmount("GBP", 1), FeedItem.TransactionDirection.IN, FeedItem.Status.SETTLED)
        );
        List<CurrencyAmount> result = RoundUpCalculator.roundUpsForFeedItems(roundUps);
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(158, result.getFirst().minorUnits());
        Assertions.assertEquals("GBP", result.getFirst().currency());
    }

    @Test
    public void multiCurrencyRoundUp() {
        //Only roundup OUT
        List<FeedItem> roundUps = List.of(
                new FeedItem(new CurrencyAmount("GBP", 435), FeedItem.TransactionDirection.OUT, FeedItem.Status.SETTLED),
                new FeedItem(new CurrencyAmount("GBP", 520), FeedItem.TransactionDirection.OUT, FeedItem.Status.SETTLED),
                new FeedItem(new CurrencyAmount("GBP", 87), FeedItem.TransactionDirection.OUT, FeedItem.Status.SETTLED),
                new FeedItem(new CurrencyAmount("GBP", 1), FeedItem.TransactionDirection.IN, FeedItem.Status.SETTLED),
                new FeedItem(new CurrencyAmount("USD", 435), FeedItem.TransactionDirection.OUT, FeedItem.Status.SETTLED),
                new FeedItem(new CurrencyAmount("USD", 520), FeedItem.TransactionDirection.OUT, FeedItem.Status.SETTLED),
                new FeedItem(new CurrencyAmount("USD", 87), FeedItem.TransactionDirection.OUT, FeedItem.Status.SETTLED),
                new FeedItem(new CurrencyAmount("USD", 1), FeedItem.TransactionDirection.IN, FeedItem.Status.SETTLED)
        );
        List<CurrencyAmount> result = RoundUpCalculator.roundUpsForFeedItems(roundUps);
        Assertions.assertEquals(2, result.size());
        CurrencyAmount gbp = result.stream().filter(ru -> ru.currency().equals("GBP")).findFirst().get();
        CurrencyAmount usd = result.stream().filter(ru -> ru.currency().equals("USD")).findFirst().get();
        Assertions.assertEquals(158, gbp.minorUnits());
        Assertions.assertEquals("GBP", gbp.currency());
        Assertions.assertEquals(158, usd.minorUnits());
        Assertions.assertEquals("USD", usd.currency());
    }

    @Test
    public void emptyCurrencyRoundUp() {
        //Only roundup OUT
        List<FeedItem> roundUps = List.of();
        List<CurrencyAmount> result = RoundUpCalculator.roundUpsForFeedItems(roundUps);
        Assertions.assertEquals(0, result.size());
    }




}
