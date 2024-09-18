package dev.onepintwig.starling.roundup.rest;

import dev.onepintwig.starling.roundup.model.RoundUpWeekRequest;
import dev.onepintwig.starling.roundup.model.RoundUpResponse;
import dev.onepintwig.starling.roundup.service.RoundUpService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Path("/round-up")
public class RoundUpEndpoint {

    @PUT
    @Path("/account/{accountUid}/savings-goal/{savingsGoalUid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public CompletableFuture<RoundUpResponse> accounts(
            @HeaderParam("Authorization") String token,
            @PathParam("accountUid") final UUID accountUid,
            @PathParam("savingsGoalUid") final UUID savingsGoalUid,
            final RoundUpWeekRequest roundUpWeekRequest
    ) {
        //TODO: Input validation
        //TODO: Check authorization token
        //TODO: Error model and response codes for Mono on error. Currently just 500's with the error message as a string.

        //Ok, now something that I actually have done! Actually execute the service logic
        return RoundUpService.doRoundUp(token, accountUid, savingsGoalUid, roundUpWeekRequest.roundUpWeekStartTimestamp()).toFuture();
    }
}
