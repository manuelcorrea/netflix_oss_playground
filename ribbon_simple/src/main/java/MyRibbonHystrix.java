/**
 * Created by mcorrea on 2/27/15.
 */


import com.netflix.client.http.HttpResponse;
import com.netflix.hystrix.Hystrix;
import com.netflix.hystrix.HystrixExecutableInfo;
import com.netflix.ribbon.*;
import com.netflix.ribbon.http.HttpRequestTemplate;
import com.netflix.ribbon.http.HttpResourceGroup;
import com.netflix.ribbon.http.HttpResponseValidator;
import com.netflix.ribbon.hystrix.FallbackHandler;
import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import rx.Observable;
import rx.Observer;
import rx.functions.Action1;
import rx.functions.Func1;

import java.nio.charset.Charset;
import java.util.Map;


public class MyRibbonHystrix {


    static HttpResourceGroup httpResourceGroup;
    
    public static void main(String args[]){
        
        
        usingTemplate();
    }


    public static void usingTemplate(){

        httpResourceGroup = Ribbon.createHttpResourceGroup("testingendpoint",
                ClientOptions.create()
                        .withMaxAutoRetriesNextServer(3)
                        .withConfigurationBasedServerList("localhost:9292"));

        HttpRequestTemplate<ByteBuf> endpointTemplate = httpResourceGroup.newTemplateBuilder("testingenpoint", ByteBuf.class)
                .withMethod("GET")
                .withUriTemplate("/?status=200&size=1")
                .withFallbackProvider(new ValidateAuthTokenFallbackHandler())
                .withResponseValidator(new ServiceResponseValidator())

                .build();

        String result = makeCall(endpointTemplate);

        System.out.println(result);
        Hystrix.reset();
        
    }

    
    
    private static String makeCall(HttpRequestTemplate<ByteBuf> endpointTemplate){

        final Observable<ByteBuf> serviceCall = endpointTemplate.requestBuilder().build().toObservable();

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
                            .withConfigurationBasedServerList("testingendpoint.cbplatform.link:80"));

            HttpRequestTemplate<ByteBuf> endpointTemplate = httpResourceGroup.newTemplateBuilder("testingenpoint", ByteBuf.class)
                    .withMethod("GET")
                    .withUriTemplate("/?size=1").build();
            

            System.out.println(hystrixInfo.isCircuitBreakerOpen());
            
            return  endpointTemplate.requestBuilder().build().toObservable();

        }
    }

    public static class ServiceResponseValidator implements HttpResponseValidator {
        @Override
        public void validate(HttpClientResponse<ByteBuf> response) throws UnsuccessfulResponseException, ServerError {
            System.out.println(response.getStatus());
            
            if (response.getStatus().code() / 100 != 2) {
                throw new UnsuccessfulResponseException("Unexpected HTTP status code " + response.getStatus());
            }
        }
    }

    
}
