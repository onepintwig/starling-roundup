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
                        .map(RoundUpCalculator::roundUpsForFeedItems)).flatMap(
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
        //Get all customers accounts
        return ReactiveStarlingClient.get(token, "/accounts", AccountList.class)
                .map(accountList -> accountList.accounts().stream().filter(account -> account.accountUid().equals(accountUid)).findFirst())
                .flatMap(maybeAccount ->
                {
                    //No way to filter on the server side, so filter in here and handle missing account logic
                    if (maybeAccount.isEmpty()) {
                        return Mono.error(new IllegalArgumentException("Account: " + accountUid + " not found"));
                    } else {
                        Account account = maybeAccount.get();
                        //For the spdcified account, get transactions for the week after the specified week start
                        Instant start = roundUpWeekStartTimestamp.toInstant();
                        Instant end = start.plus(7, ChronoUnit.DAYS);
                        String query = "/feed/account/" + account.accountUid().toString() + "/settled-transactions-between" + "?minTransactionTimestamp=" + start + "&maxTransactionTimestamp=" + end;
                        return ReactiveStarlingClient.get(token, query, FeedItemList.class).map(FeedItemList::feedItems);
                    }
                });
    }


}
