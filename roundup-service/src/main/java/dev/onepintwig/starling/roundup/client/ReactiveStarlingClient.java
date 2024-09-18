package dev.onepintwig.starling.roundup.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.*;

import reactor.core.publisher.Mono;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.netty.ByteBufFlux;
import reactor.netty.NettyPipeline;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;

//Still a slightly indoctrinated Scala developer... So everything has to be lazily evaluated and reactive :D
public class ReactiveStarlingClient {


    private static final HttpClient underlying = HttpClient.create()
            .baseUrl("https://api-sandbox.starlingbank.com/api/v2");

    //I've dumbed down the api a bit to only have the stuff I care about. So let jackson know to drop everything else
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    //Make sure all the requests have the bearer token passed through
    private static HttpClient withAuth(String authToken) {
        return underlying
                .headers(h -> h.add(HttpHeaderNames.AUTHORIZATION, authToken)
                        .add(HttpHeaderNames.ACCEPT, "application/json")
                        .add(HttpHeaderNames.CONTENT_TYPE, "application/json"));
    }

    /**
     *
     * @param authToken The token to use for request authorization
     * @param endpoint The starling endpoint to hit
     * @param payload Payload for the put
     * @param response Class instance used for deserialization
     * @return A [[Mono]] of the expected response type
     * @param <T> THe type of the payload
     * @param <T2> The type of the expected result
     */
    public static <T, T2> Mono<T2> put(String authToken, String endpoint, T payload, Class<T2> response) {
        return handeResponse(withAuth(authToken)
                .put()
                .uri(endpoint)
                .send(ByteBufFlux.fromString(serialize(payload))), response);
    }

    /**
     *
     * @param authToken The token to use for request authorization
     * @param query The starling endpoint to hit
     * @param target Class instance used for deserialization
     * @return A [[Mono]] of the expected response type
     * @param <T> The type of the expected result
     */
    public static <T> Mono<T> get(String authToken, String query, Class<T> target) {
        return handeResponse(withAuth(authToken)
                //So - I was up until like 2am the evening I wrote this because this was returning 400 for everything...
                //Not gonna lie... I was pondering a career change at about 1:45am.
                //Turns out that the starling aws load-balancer rejects any request with Content-Length: 0 as sus
                //
                //Classification reasons in: https://docs.aws.amazon.com/elasticloadbalancing/latest/application/load-balancer-access-logs.html
                //GetHeadZeroContentLength is the one that killed me - and is tripped when desync mitigation is set to "strictest"
                //
                //This would be fine, but reactor-netty decide to automatically inject this header at the very end of every Get without documenting that they do!
                //So this code snippet is a shameless copy+paste that disables that lovely feature for Get Requests only
                //https://github.com/reactor/reactor-netty/issues/2900
                .doOnChannelInit((_, ch, _) ->
                        ch.pipeline().addAfter(NettyPipeline.HttpCodec, "test",
                                new ChannelOutboundHandlerAdapter() {
                                    @Override
                                    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
                                        if (msg instanceof HttpRequest) {
                                            // manipulate headers
                                            ((HttpRequest) msg).headers().remove(HttpHeaderNames.CONTENT_LENGTH);
                                        }
                                        ctx.write(msg, promise);
                                    }
                                }))
                //Ok, back to normal code
                .get()
                .uri(query), target);
    }

    //Pretty noddy response handler. Deserialize if 200, else catch the underlying error and propagate back to caller
    private static <T> Mono<T> handeResponse(HttpClient.ResponseReceiver<?> receiver, Class<T> target){
        return receiver
                .responseSingle((headers, response) ->
                response.asString().flatMap(
                        json -> {
                            //TODO: Extended handling of response codes
                            if (headers.status().code() != 200) {
                                return Mono.error(new Throwable(json));
                            } else {
                                return deserialize(json, target);
                            }
                        }
                )
        );
    }

    private static <T> Mono<String> serialize(T object) {
        try {
            String json = objectMapper.writeValueAsString(object);
            return Mono.just(json);
        } catch (IOException e) {
            return Mono.error(e);
        }
    }

    private static <T> Mono<T> deserialize(String json, Class<T> target) {
        try {
            T object = objectMapper.readValue(json, target);
            return Mono.just(object);
        } catch (IOException e) {
            return Mono.error(e);
        }
    }
}
