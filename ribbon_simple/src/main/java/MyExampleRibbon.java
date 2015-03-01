import com.google.common.collect.Lists;
import com.netflix.client.ClientFactory;
import com.netflix.config.ConfigurationManager;
import com.netflix.loadbalancer.BaseLoadBalancer;
import com.netflix.loadbalancer.LoadBalancerBuilder;
import com.netflix.loadbalancer.Server;
import com.netflix.ribbon.transport.netty.RibbonTransport;
import com.netflix.ribbon.transport.netty.http.LoadBalancingHttpClient;
import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import rx.Observer;
import rx.functions.Action1;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;


/**
 * Created by mcorrea on 2/27/15.
 */
public class MyExampleRibbon {
    
    
    public static void main(String args[])throws Exception{
       // simpleGet("http://testingendpoint.cbplatform.link/?status=500&size=1");
        
        lbGet( Lists.newArrayList(
                "localhost:9292",
                "testingendpoint.cbplatform.link"
        ) );
    }
    
    
    
    public static void simpleGet(String url){
        LoadBalancingHttpClient<ByteBuf, ByteBuf> client = RibbonTransport.newHttpClient();
        HttpClientRequest<ByteBuf> request = HttpClientRequest.createGet(url);
        client.submit(request)
                .toBlocking()
                .forEach(new Action1<HttpClientResponse<ByteBuf>>() {
                    @Override
                    public void call(HttpClientResponse<ByteBuf> response) {
                        System.out.println(response.getStatus());
                        response.getContent().subscribe(new Action1<ByteBuf>() {
                            @Override
                            public void call(ByteBuf content) {
                                System.out.println("Response content: " + content.toString(Charset.defaultCharset()));
                            }
                        });
                    }
                    
                    
                });
    }
    
    
    public static void lbGet(List<String> urls)throws  Exception{
        List<Server> servers = new ArrayList<Server>();
        for(String url: urls){
            servers.add(new Server(url));
        }
        BaseLoadBalancer lb = LoadBalancerBuilder.newBuilder()
                .buildFixedServerListLoadBalancer(servers);

        LoadBalancingHttpClient<ByteBuf, ByteBuf> client = RibbonTransport.newHttpClient(lb);
        final CountDownLatch latch = new CountDownLatch(50);
        Observer<HttpClientResponse<ByteBuf>> observer = new Observer<HttpClientResponse<ByteBuf>>() {
            @Override
            public void onCompleted() {
            }

            @Override
            public void onError(Throwable e) {
                e.printStackTrace();
            }

            @Override
            public void onNext(HttpClientResponse<ByteBuf> args) {
                latch.countDown();
                System.out.println("Got response: " + args.getStatus());
            }
        };

        for (int i = 0; i < 50; i++) {
            HttpClientRequest<ByteBuf> request = HttpClientRequest.createGet("/?size=1");
            client.submit(request).subscribe(observer);
        }
        
        
        latch.await();
        System.out.println(lb.getLoadBalancerStats());
               
        
    }
    
}
