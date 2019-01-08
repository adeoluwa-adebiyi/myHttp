import com.google.common.collect.ArrayListMultimap;
import com.google.common.util.concurrent.*;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.net.ssl.SSLSession;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.Buffer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.concurrent.*;

class OfbRequest{
    private String url;
    private String method;
    private String tag;
    private String body;
    private HashMap<String, String> headers;

    public OfbRequest(String url, String method, HashMap<String, String > headers,String tag){
        this.url = url;
        this.method = method;
        this.headers = headers;
        this.tag = tag;
    }

    public String getMethod() {
        return method;
    }

    public String getTag() {
        return tag;
    }

    public HashMap<String, String> getHeaders() {
        return headers;
    }

    protected void sendData(HttpURLConnection con, String data) throws IOException {
        DataOutputStream wr = null;
        try {
            wr = new DataOutputStream(con.getOutputStream());
            wr.writeBytes(data);
            wr.flush();
            wr.close();
        } catch(IOException exception) {
            throw exception;
        } finally {
            this.closeQuietly(wr);
        }
    }

    protected void closeQuietly(Closeable closeable) {
        try {
            if( closeable != null ) {
                closeable.close();
            }
        } catch(IOException ex) {

        }
    }

    public void setBody(String body){
        this.body = body;
    }

    protected HttpURLConnection getHttpURLConnection(){
        HttpURLConnection httpURLConnection = null;
        try{
            URL url = new URL(this.url);
            httpURLConnection = (HttpURLConnection)url.openConnection();
            httpURLConnection.setRequestMethod(this.getMethod().toUpperCase());
            if(this.getMethod().toUpperCase().equals("POST")){
                this.sendData(httpURLConnection,this.body);
            }
            for(String property : this.headers.keySet()) {
                httpURLConnection.setRequestProperty(property,this.headers.get(property));
            }
        }catch (Exception e){
            e.getMessage();
        }
        return httpURLConnection;
    }
}

class OfbResponse {
    public OfbResponse(InputStream content, String response){
        this.content = content;
        this.response = response;
    }
    private InputStream content;
    private String response;
    public InputStream getContent(){
        return content;
    }
    public String getResponse(){
        return response;
    }
}


public class myHttp {
    private static int numThreads = 100;
    private static ListeningExecutorService pool = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(numThreads));
    private static HashMap<String, ArrayList<ListenableFuture<OfbResponse>>> futureRegistry = new HashMap<String, ArrayList<ListenableFuture<OfbResponse>>>();


    public static void main(String[] args){
        for(int i = 1; i <=10;i++) {
            /** POST Test
            HashMap <String,String> headers = new HashMap<String, String>();
            headers.put("Content-Type","application/json");
            OfbRequest request = new OfbRequest("https://jsonplaceholder.typicode.com/posts", "POST", headers, "");
            request.setBody("{\n" +
                    "      title: 'foo',\n" +
                    "      body: 'bar',\n" +
                    "      userId: 1\n" +
                    "    }");
            ListenableFuture<OfbResponse> response = myHttp.execute(request);**/

            //Get Test -- Comment this line and uncomment the above lines to test POST
            ListenableFuture<OfbResponse> response = myHttp.execute(new OfbRequest("https://jsonplaceholder.typicode.com/todos/1", "GET", new HashMap<String, String>(), ""));
            Futures.addCallback(response, new FutureCallback<OfbResponse>() {
                public void onSuccess(@Nullable OfbResponse ofbResponse) {
                    Scanner scanner = new Scanner(ofbResponse.getContent());
                    String output="";
                    while(scanner.hasNext()){
                        output += scanner.nextLine();
                    }
                    System.out.println(output);
                }

                public void onFailure(Throwable throwable) {
                    System.out.println("Future servicing failed. Error: " + throwable.getMessage());
                    throwable.getStackTrace();
                }
            }, Executors.newSingleThreadExecutor());
           // myHttp.cancel("");
        }
    }

    static ListenableFuture<OfbResponse> execute(final OfbRequest request){
        ListenableFuture getResponseFuture = pool.submit(new Callable() {
            public Object call() throws Exception {
                HttpURLConnection connection = request.getHttpURLConnection();
                connection.connect();

                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader((InputStream)connection.getContent()));
                return  new OfbResponse((InputStream) connection.getContent(),connection.getResponseMessage());
            }
        });
        ArrayList<ListenableFuture<OfbResponse>> list = futureRegistry.get(request.getTag());
        if(list==null){
            list = new ArrayList<ListenableFuture<OfbResponse>>();
        }
        list.add(getResponseFuture);
        futureRegistry.put(request.getTag(),list);
        return getResponseFuture;
    }

    static void cancel(String tag){
        for(ListenableFuture<OfbResponse> future : futureRegistry.get(tag)){
            future.cancel(true);
        }
        futureRegistry.remove(tag);
    }
}
