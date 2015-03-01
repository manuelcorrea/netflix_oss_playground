
import com.netflix.client.ClientFactory;
import com.netflix.client.http.HttpRequest;
import com.netflix.client.http.HttpResponse;
import com.netflix.hystrix.Hystrix;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.niws.client.http.RestClient;
import com.netflix.ribbon.Ribbon;
import com.netflix.ribbon.transport.netty.RibbonTransport;
import com.netflix.ribbon.transport.netty.http.LoadBalancingHttpClient;
import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import rx.functions.Action1;

import javax.ws.rs.core.MultivaluedMap;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;

import static com.netflix.client.http.HttpRequest.Verb;

/**
 * Hystrix wrapper around Eureka Ribbon command
 * @author mcorrea modified generic
 */
public class RibbonCommand extends HystrixCommand<HttpClientResponse<ByteBuf>> {

    String url;
    Verb verb;
    URI uri;
    MultivaluedMap<String, String> headers;
    MultivaluedMap<String, String> params;

    public RibbonCommand(
                         String url,
                         Verb verb,
                         String uri,
                         MultivaluedMap<String, String> headers,
                         MultivaluedMap<String, String> params) throws URISyntaxException {

        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("test")).andCommandPropertiesDefaults(
                HystrixCommandProperties.Setter().withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE)
                        ));
        
        this.url = url;
        this.verb = verb;
        this.uri = new URI(uri);
        this.headers = headers;
        this.params = params;
    }

    @Override
    protected  HttpClientResponse<ByteBuf> run() throws Exception {
        try {
            return forward();
        } catch (Exception e) {
            throw e;
        }
    }

    HttpClientResponse<ByteBuf> forward() throws Exception {

        LoadBalancingHttpClient<ByteBuf, ByteBuf> client = RibbonTransport.newHttpClient();
        HttpClientRequest<ByteBuf> request = HttpClientRequest.createGet(url);

        return client.submit(request).toBlocking().first();
    }

    @Override
    protected  HttpClientResponse<ByteBuf> getFallback() {
        
        System.out.println("Fallback!");
        LoadBalancingHttpClient<ByteBuf, ByteBuf> client = RibbonTransport.newHttpClient();
        HttpClientRequest<ByteBuf> request = HttpClientRequest.createGet("http://localhost:9292/headers");

        return client.submit(request).toBlocking().first();
    }
    
    
    public static void main(String args[])throws Exception{
        System.out.println("ribbon command");
        RibbonCommand ribbonCommand = new RibbonCommand(
                "http://testingendpoint.cbplatform.link123/?status=200&size=1",
                Verb.GET, 
                "/headers",
                null, 
                null
        );
        
        HttpClientResponse<ByteBuf> res = ribbonCommand.execute();
        

        System.out.println(res.getStatus().code());
        res.getContent().subscribe(new Action1<ByteBuf>() {

            @Override
            public void call(ByteBuf content) {
                System.out.println("Response content: " + content.toString(Charset.defaultCharset()));
            }

        });

        Hystrix.reset();
        
    }
}


