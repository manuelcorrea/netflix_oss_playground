/**
 * Created by mcorrea on 2/27/15.
 */


import com.netflix.hystrix.Hystrix;
import com.netflix.hystrix.HystrixExecutableInfo;
import com.netflix.ribbon.ClientOptions;
import com.netflix.ribbon.Ribbon;
import com.netflix.ribbon.http.HttpRequestTemplate;
import com.netflix.ribbon.http.HttpResourceGroup;
import com.netflix.ribbon.hystrix.FallbackHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.reactivex.netty.channel.StringTransformer;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import rx.Observable;
import rx.functions.Func1;

import java.nio.charset.Charset;
import java.util.Map;


public class MyRibbonHystrix {

    public static void main(String args[]){
        usingTemplate();
    }
    
    
    public static void usingTemplate(){

        HttpResourceGroup httpResourceGroup = Ribbon.createHttpResourceGroup("testingendpoint",
                ClientOptions.create()
                        .withMaxAutoRetriesNextServer(3)
                        .withConfigurationBasedServerList("localhost:9292"));

        HttpRequestTemplate<ByteBuf> endpointTemplate = httpResourceGroup.newTemplateBuilder("testingenpoint", ByteBuf.class)
                .withMethod("GET")
                .withUriTemplate("/headers")
                .withFallbackProvider(new ValidateAuthTokenFallbackHandler()).build();

        String result = makeCall(endpointTemplate);

        System.out.println(result);
        Hystrix.reset();
        
    }
    
    private static String makeCall(HttpRequestTemplate<ByteBuf> endpointTemplate){
       
        Observable<ByteBuf> serviceCall = endpointTemplate.requestBuilder().build().toObservable();


        String result1 = serviceCall.map(new Func1<ByteBuf, String>() {
            @Override
            public String call(ByteBuf buf) {
                String string = buf.toString(Charset.defaultCharset());
                return string;
            }
        }).toBlocking().first();

        return result1;
    }

    private static class ValidateAuthTokenFallbackHandler implements FallbackHandler<ByteBuf> {
        @Override
        public Observable<ByteBuf> getFallback(HystrixExecutableInfo<?> hystrixInfo, Map<String, Object> requestProperties) {
            HttpResourceGroup httpResourceGroup = Ribbon.createHttpResourceGroup("testingendpoint",
                    ClientOptions.create()
                            .withMaxAutoRetriesNextServer(3)
                            .withConfigurationBasedServerList("localhost:9393"));

            HttpRequestTemplate<ByteBuf> endpointTemplate = httpResourceGroup.newTemplateBuilder("testingenpoint", ByteBuf.class)
                    .withMethod("GET")
                    .withUriTemplate("/?size=1").build();
            
            System.out.println(hystrixInfo.isCircuitBreakerOpen());
            
            return  endpointTemplate.requestBuilder().build().toObservable();


//            // TODO: Do something more useful
//            byte[] bytes = "{}".getBytes(Charset.defaultCharset());
//            ByteBuf byteBuf = UnpooledByteBufAllocator.DEFAULT.buffer(bytes.length);
//            byteBuf.writeBytes(bytes);
//            return Observable.just(byteBuf);
        }
    }

    
}
