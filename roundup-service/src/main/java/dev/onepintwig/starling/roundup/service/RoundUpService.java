package dev.onepintwig.starling.roundup.service;

import dev.onepintwig.starling.roundup.client.ReactiveStarlingClient;
import dev.onepintwig.starling.roundup.model.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public final class RoundUpService {

    /**
     * Gets all the feed items for a given account, and calculates the round-up amounts for each currency
     * These are then added to the given savings goal
     * <p>
     * Each currency is added in a separate request to the savings goal.
     * NOTE: This is not atomic, so if one fails, the others will still be added.
     * Would need to be wrapped in a transaction for the real world (or use a some sort of queue to guarantee delivery)
     * Regretting my choice of rounding all currencies now...
     *
     * @param token          The bearer token for the request
     * @param accountUid     The account to get the feed items for
     * @param savingsGoalUid The savings goal to add the round-up amounts to
     * @return A list of all the completed savings goals
     */
    public static Mono<RoundUpResponse> doRoundUp(String token, UUID accountUid, UUID savingsGoalUid, Date roundUpWeekStartTimestamp) {
        //Get feed items
        return Flux.from(getFeedItems(token, accountUid, roundUpWeekStartTimestamp)
                        //Get the roundup for the feed items
                        .map(RoundUpService::roundUpsForFeedItems)).flatMap(
                        roundUps ->
                                //For each calculated round up currency, execute the savings goal transfer
                                Flux.fromIterable(roundUps).flatMap(
                                        roundUp -> {
                                            UUID transferUid = UUID.randomUUID();
                                            return ReactiveStarlingClient.put(
                                                    token,
                                                    "/account/" + accountUid + "/savings-goals/" + savingsGoalUid + "/add-money/" + transferUid,
                                                    new TopUpRequest(roundUp),
                                                    SavingsGoalTransferResponse.class
                                            );
                                        }
                                )
                ).collectList()
                //Map to our api
                .map(RoundUpResponse::new);
    }


    /**
     * Gets all the feed items for a given account, using the accounts default categoryId
     * <p>
     * No API for getting an account by id - so get the list and filter from there. Returns an error if the account is not found
     *
     * @param token      The bearer token for the request
     * @param accountUid The account to get the feed items for
     * @return The FeedItems for the account
     */
    private static Mono<List<FeedItem>> getFeedItems(String token, UUID accountUid, Date roundUpWeekStartTimestamp) {
        //Get all the feed items for all accounts
        return ReactiveStarlingClient.get(token, "/accounts", AccountList.class)
                .map(accountList -> accountList.accounts().stream().filter(account -> account.accountUid().equals(accountUid)).findFirst())
                .flatMap(maybeAccount ->
                {
                    //No way to filter on the server side, so filter in here and handle missing account logic
                    if (maybeAccount.isEmpty()) {
                        return Mono.error(new IllegalArgumentException("Account: " + accountUid + " not found"));
                    } else {
                        Account account = maybeAccount.get();
                        //Get transactions for the week after the specified week start
                        Instant start = roundUpWeekStartTimestamp.toInstant();
                        Instant end = start.plus(7, ChronoUnit.DAYS);
                        String query = "/feed/account/" + account.accountUid().toString() + "/settled-transactions-between" + "?minTransactionTimestamp=" + start + "&maxTransactionTimestamp=" + end;
                        return ReactiveStarlingClient.get(token, query, FeedItemList.class).map(FeedItemList::feedItems);
                    }
                });
    }

    /**
     * Gets a list of all the round up amounts for the currencies in a given [[FeedItem]] set
     * A valid transaction for rounding-up is a settled outbound transaction
     * <p>
     * Could be simpler if we only rounded up a single currency... but holiday spending's should save for the next holiday too!
     *
     * @param feedItems The feed-item list to determine round-ups from
     * @return The round-up total for each currency in the feed-items
     */
    private static List<CurrencyAmount> roundUpsForFeedItems(List<FeedItem> feedItems) {
        //Filter down to only relevant transactions. Settled and outbound. Admitting here that my banking domain knowledge is a bit weak.
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
