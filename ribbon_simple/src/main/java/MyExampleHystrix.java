/**
 * Created by mcorrea on 2/27/15.
 */

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;

public class MyExampleHystrix {
    
    public static void main(String args[]){
        String s = new CommandHelloWorld("FOOBAR").execute();
        System.out.println(s);
        System.exit(0);
    }

    public static class CommandHelloWorld extends HystrixCommand<String> {

        private final String name;

        public CommandHelloWorld(String name) {
            super(HystrixCommandGroupKey.Factory.asKey("ExampleGroup"));
            this.name = name;
        }

        @Override
        protected String run() {

            return name+" successful!";
        }

        @Override
        protected String getFallback() {

            return name + " failed";
            
        }
    }
}
