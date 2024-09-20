package dev.onepintwig.starling.roundup.service;

import dev.onepintwig.starling.roundup.model.CurrencyAmount;
import dev.onepintwig.starling.roundup.model.FeedItem;

import java.util.List;
import java.util.stream.Collectors;

public class RoundUpCalculator {

    /**
     * Gets a list of all the round up amounts for the currencies in a given [[FeedItem]] set
     * A valid transaction for rounding-up is an outbound transaction
     * <p>
     * Could be simpler if we only rounded up a single currency... but holiday spending's should save for the next holiday too!
     *
     * @param feedItems The feed-item list to determine round-ups from
     * @return The round-up total for each currency in the feed-items
     */
    public static List<CurrencyAmount> roundUpsForFeedItems(List<FeedItem> feedItems) {
        //Filter down to only relevant transactions. Settled and outbound. Admitting here that my banking domain knowledge is a bit weak.
        //Already filtered on server side for settled. So just need to check direction now.
        return feedItems.stream()
                .filter(fi -> fi.direction() == FeedItem.TransactionDirection.OUT)
                //Group and reduce amounts by currency
                .collect(
                        Collectors.groupingBy(
                                fi -> fi.amount().currency(),
                                Collectors.reducing(
                                        0,
                                        //In the interests of time, assuming 100 here for the "round-up" target.
                                        //Would likely need an additional lookup table or service call for prod
                                        //as some currencies have different decimalisation
                                        fi -> 100 - (fi.amount().minorUnits() % 100),
                                        Integer::sum
                                )
                        )
                ).entrySet().stream().map(
                        //Return in our model for easy sending to savings target
                        group -> new CurrencyAmount(group.getKey(), group.getValue())
                ).toList();
    }
}
